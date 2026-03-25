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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // H2 console et gestion d'erreurs
                .requestMatchers("/h2-console/**", "/error").permitAll()
                // Authentification
                .requestMatchers(HttpMethod.POST, "/login", "/register").permitAll()
                // Patient : ses propres RDV et annulation (avant la règle publique /doctors/**)
                .requestMatchers(HttpMethod.GET, "/patients/me/slots").hasRole("PATIENT")
                .requestMatchers(HttpMethod.PATCH, "/slots/*/cancel").hasRole("PATIENT")
                // Doctor : son planning personnel (avant la règle publique /doctors/**)
                .requestMatchers(HttpMethod.GET, "/doctors/me/slots").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.POST, "/doctors/me/slots").hasRole("DOCTOR")
                // Modification du motif et complétion de RDV
                .requestMatchers(HttpMethod.PATCH, "/slots/*").hasRole("PATIENT")
                .requestMatchers(HttpMethod.PUT, "/slots/*/complete").hasRole("DOCTOR")
                // Profil utilisateur
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/users/me").authenticated()
                // Admin
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Réservation d'un créneau : PATIENT uniquement
                .requestMatchers(HttpMethod.POST, "/slot/**").hasRole("PATIENT")
                // Lecture publique des médecins et créneaux
                .requestMatchers(HttpMethod.GET,
                        "/doctors", "/doctors/**", "/doctor/**",
                        "/slots", "/slots/**").permitAll()
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:4200",
            "http://localhost:8081"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
