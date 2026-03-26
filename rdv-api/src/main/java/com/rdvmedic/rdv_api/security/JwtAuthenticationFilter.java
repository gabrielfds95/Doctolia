package com.rdvmedic.rdv_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre HTTP exécuté UNE SEULE FOIS par requête (OncePerRequestFilter).
 *
 * Rôle : intercepter chaque requête entrante, extraire le JWT du header
 * Authorization, le valider, puis charger l'utilisateur correspondant dans
 * le SecurityContext de Spring Security.
 *
 * Sans ce filtre, Spring Security ne saurait pas "qui" fait la requête.
 * Une fois le SecurityContext alimenté, les annotations @PreAuthorize et
 * les règles de SecurityConfig.java prennent le relais.
 *
 * Flux :
 *   Requête HTTP
 *     → extractToken()  → "Bearer eyJ..." → "eyJ..."
 *     → tokenProvider.validateToken()  → true/false
 *     → userDetailsService.loadUserByUsername()  → UserDetails
 *     → SecurityContextHolder.setAuthentication()
 *     → chain.doFilter() → le reste de la chaîne Spring (controllers...)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Vérifie et décode les tokens JWT
    @Autowired
    private JwtTokenProvider tokenProvider;

    // Charge l'utilisateur depuis la base via son username
    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Méthode principale du filtre, appelée automatiquement sur chaque requête HTTP.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 1. Extraire le token du header "Authorization: Bearer <token>"
        String token = extractToken(request);

        // 2. Si un token est présent ET valide (signature + expiration OK)
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {

            // 3. Récupérer le username depuis le payload du token
            String username = tokenProvider.getUsernameFromToken(token);

            // 4. Charger l'utilisateur depuis la BDD pour obtenir ses rôles à jour
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Créer un objet Authentication avec les droits (rôles) de l'utilisateur
            //    Le mot de passe est null car on ne refait pas de vérification ici
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 6. Ajouter des métadonnées (adresse IP, session...) à l'authentification
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 7. Injecter l'authentification dans le SecurityContext
            //    → Spring Security sait maintenant QUI fait la requête
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 8. Passer la requête au filtre/controller suivant dans la chaîne,
        //    qu'on soit authentifié ou non (c'est SecurityConfig qui bloquera si besoin)
        chain.doFilter(request, response);
    }

    /**
     * Extrait le token brut depuis le header Authorization.
     *
     * Format attendu : "Bearer eyJhbGciOiJIUzI1NiJ9..."
     * On retire les 7 premiers caractères ("Bearer ") pour obtenir le token seul.
     *
     * @return le token JWT ou null si absent/mal formé
     */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        // Vérifie que le header existe ET commence bien par "Bearer "
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7); // "Bearer " = 7 caractères
        }
        return null;
    }
}
