package com.rdvmedic.rdv_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Composant responsable de TOUTE la logique JWT :
 *  - Générer un token après connexion réussie
 *  - Valider un token reçu dans une requête
 *  - Extraire l'identifiant (username) d'un token
 *
 * Un JWT est composé de 3 parties séparées par des points :
 *   HEADER.PAYLOAD.SIGNATURE
 *   - Header  : algorithme utilisé (HS256)
 *   - Payload : les données publiques (userId, roles, expiration)
 *   - Signature : hash HMAC-SHA256 du header + payload avec la clé secrète
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    // Clé secrète lue depuis application.properties (app.jwt.secret)
    // Elle sert à signer et à vérifier les tokens — ne doit JAMAIS être exposée
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Durée de validité en millisecondes, lue depuis application.properties
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Convertit la clé secrète (String) en objet cryptographique SecretKey.
     * HMAC-SHA utilise une clé symétrique : la même clé signe ET vérifie.
     */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un token JWT après une authentification réussie.
     *
     * Le token embarque dans son payload :
     *  - subject    : le username (identifiant unique de l'utilisateur)
     *  - userId     : l'id en base (utile pour les requêtes "me")
     *  - roles      : les rôles séparés par virgule (ex: "ROLE_PATIENT")
     *  - issuedAt   : date/heure de création
     *  - expiration : date/heure d'expiration (issuedAt + jwtExpiration)
     *
     * @param authentication objet Spring Security contenant l'utilisateur connecté
     * @return le token JWT signé sous forme de String compact
     */
    public String generateToken(Authentication authentication) {
        // Récupère notre objet UserPrincipal depuis le contexte d'authentification
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Concatène tous les rôles en une seule chaîne : "ROLE_PATIENT,ROLE_USER"
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(principal.getUsername())          // sub = username
                .claim("userId", principal.getId())        // claim personnalisé : id BDD
                .claim("roles", roles)                     // claim personnalisé : rôles
                .issuedAt(new Date())                      // iat = maintenant
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // exp
                .signWith(signingKey())                    // signe avec HMAC-SHA256
                .compact();                                // sérialise en String
    }

    /**
     * Extrait le username depuis le payload d'un token.
     * Appelé par JwtAuthenticationFilter pour identifier l'utilisateur.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Vérifie qu'un token est valide :
     *  - signature correcte (non falsifié)
     *  - non expiré
     *
     * @return true si valide, false sinon (log d'avertissement en cas d'échec)
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // Lève une exception si invalide ou expiré
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse et vérifie la signature du token, retourne le payload (Claims).
     * C'est la méthode centrale : toute manipulation de token passe par ici.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey()) // vérifie la signature HMAC
                .build()
                .parseSignedClaims(token) // parse + valide expiration
                .getPayload();            // retourne le payload décodé
    }
}
