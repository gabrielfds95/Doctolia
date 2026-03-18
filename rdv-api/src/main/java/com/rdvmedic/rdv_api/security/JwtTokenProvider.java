package com.rdvmedic.rdv_api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwtSecret:MyVerySecureSecretKeyThatMustBeAtLeast32CharactersLongForHS512Algorithm}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}") // 24 heures en ms
    private int jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs:604800000}") // 7 jours en ms
    private int jwtRefreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un JWT token à partir de l'authentification
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Génère un JWT token à partir d'un username
     */
    public String generateTokenFromUsername(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Génère un refresh token
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extrait le username du JWT token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Extrait les roles du JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return (List<String>) claims.get("roles");
    }

    /**
     * Valide un JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformé: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expiré: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT non supporté: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("String JWT vide: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.error("Erreur de signature JWT: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Récupère la durée d'expiration du token
     */
    public long getExpirationTime() {
        return jwtExpirationMs / 1000; // Retourne en secondes
    }
}
