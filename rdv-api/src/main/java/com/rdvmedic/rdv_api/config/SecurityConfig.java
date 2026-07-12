package com.rdvmedic.rdv_api.config;

import com.rdvmedic.rdv_api.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration centrale de Spring Security.
 *
 * C'est ici que l'on définit :
 *  1. L'algorithme de hachage des mots de passe (BCrypt)
 *  2. Le gestionnaire d'authentification (AuthenticationManager)
 *  3. La chaîne de filtres de sécurité (SecurityFilterChain) :
 *     - CORS (quels clients peuvent appeler l'API)
 *     - CSRF (désactivé car on utilise JWT stateless)
 *     - Session (STATELESS : pas de session côté serveur)
 *     - Règles d'autorisation par URL et méthode HTTP
 *     - Ajout du filtre JWT dans la chaîne
 *  4. La configuration CORS
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean BCrypt : encode les mots de passe avec un salt aléatoire et 10 rounds de hachage.
     * Un même mot de passe donne un hash différent à chaque fois → protection contre
     * les attaques par table arc-en-ciel (rainbow table).
     * Utilisé par AuthService pour encoder à l'inscription et comparer à la connexion.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager : composant Spring qui coordonne la vérification des credentials.
     * Utilisé dans AuthService.login() pour déclencher l'authentification username/password.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Chaîne de filtres de sécurité : le cœur de la configuration.
     * Chaque requête HTTP passe par cette chaîne dans l'ordre défini.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            // CORS : autorise Angular (4200) et l'app mobile (8081) à appeler l'API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF désactivé : inutile avec JWT stateless (pas de cookie de session à protéger)
            .csrf(csrf -> csrf.disable())

            // STATELESS : Spring Security ne crée PAS de session HTTP côté serveur
            // Chaque requête doit s'authentifier via son propre token JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Autorise les iframes same-origin pour la console H2 (base en mémoire pour les tests)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

            // ── Règles d'autorisation par URL ───────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Console H2 et gestion d'erreurs : accessibles sans token
                .requestMatchers("/h2-console/**", "/error").permitAll()

                // Connexion et inscription : accessibles à tous (pas encore de token)
                .requestMatchers(HttpMethod.POST, "/login", "/register").permitAll()

                // Patient : consultation et annulation de SES propres RDV
                // (l'id du patient est extrait du JWT côté controller)
                .requestMatchers(HttpMethod.GET, "/patients/me/slots").hasRole("PATIENT")
                .requestMatchers(HttpMethod.PATCH, "/slots/*/cancel").hasRole("PATIENT")

                // Médecin : consultation et création dans SON planning
                // Ces règles sont déclarées AVANT la règle publique /doctors/**
                .requestMatchers(HttpMethod.GET, "/doctors/me/slots").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.POST, "/doctors/me/slots").hasRole("DOCTOR")

                // Modification du motif : réservé au patient
                .requestMatchers(HttpMethod.PATCH, "/slots/*").hasRole("PATIENT")

                // Marquer un RDV comme terminé : réservé au médecin
                .requestMatchers(HttpMethod.PUT, "/slots/*/complete").hasRole("DOCTOR")

                // Profil : tout utilisateur connecté peut lire/modifier son profil
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/users/me").authenticated()

                // Administration : uniquement ROLE_ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Réservation d'un créneau : PATIENT uniquement
                // (l'id patient vient du JWT, pas de l'URL → sécurisé)
                .requestMatchers(HttpMethod.POST, "/slot/**").hasRole("PATIENT")

                // Suppression d'une indisponibilité : réservée au médecin, et uniquement
                // la sienne (ownership vérifié dans SlotService.deleteSlot).
                .requestMatchers(HttpMethod.DELETE, "/slot/*").hasRole("DOCTOR")

                // Gestion administrative des comptes patient/médecin : jamais accessible
                // à un simple utilisateur connecté. Avant ce correctif, ces 4 routes
                // n'avaient AUCUNE règle déclarée et retombaient sur anyRequest().authenticated()
                // → n'importe quel utilisateur authentifié (patient inclus) pouvait lister
                // tous les patients (avec leur NIR), en créer, ou supprimer un patient/médecin.
                .requestMatchers(HttpMethod.GET, "/patients").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/patient").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/patient/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/doctor/*").hasRole("ADMIN")

                // Lecture publique : tout le monde peut voir les médecins et leurs créneaux
                // (nécessaire pour afficher le calendrier avant connexion).
                // Ces deux endpoints renvoient PublicSlotDTO (pas SlotDTO) : aucune donnée
                // patient n'est exposée. Voir AUDIT-SECURITE.md, faille 1.
                //
                // "/slots/**" a été RETIRÉ de cette liste : il couvrait GET /slots/{idDoctor}/{idPatient},
                // une requête ciblée sur un patient précis, sans cas d'usage anonyme légitime
                // (contrairement au calendrier d'un médecin). Elle retombe donc sur
                // anyRequest().authenticated() plus bas → 401 sans token.
                .requestMatchers(HttpMethod.GET,
                        "/doctors", "/doctors/**", "/doctor/**",
                        "/slots").permitAll()

                // Deny-by-default : toute route non listée explicitement ci-dessus exige
                // au minimum d'être connecté (jamais permitAll par défaut). Un nouvel
                // endpoint ajouté sans règle dédiée est donc automatiquement fermé aux
                // anonymes, plutôt que silencieusement ouvert à tous.
                .anyRequest().authenticated()
            )

            // Ajoute le filtre JWT AVANT le filtre d'auth username/password de Spring
            // → à chaque requête, le JWT est lu et validé en premier
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS (Cross-Origin Resource Sharing).
     *
     * Sans CORS, le navigateur bloquerait les requêtes depuis Angular (port 4200)
     * vers l'API (port 9000) car les origines sont différentes.
     *
     * On autorise explicitement :
     *  - les deux origines connues (Angular dev + Expo web)
     *  - toutes les méthodes HTTP utiles
     *  - tous les headers (dont Authorization pour le JWT)
     *  - les credentials (cookies si besoin)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:4200",  // Angular dev server
            "http://localhost:8081"   // Expo web / React Native
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*")); // Autorise tous les headers, dont Authorization
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Applique à toutes les URLs
        return source;
    }
}
