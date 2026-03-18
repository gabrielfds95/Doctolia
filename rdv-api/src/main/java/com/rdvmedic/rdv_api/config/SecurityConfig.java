package com.rdvmedic.rdv_api.config;

import com.rdvmedic.rdv_api.security.JwtAuthenticationFilter;
import com.rdvmedic.rdv_api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
                http.getSharedObject(AuthenticationManagerBuilder.class);
        
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                    // Endpoints publics
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/doctors").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/doctors/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/doctors/{id}/slots").permitAll()
                    .requestMatchers("/health").permitAll()
                    
                    // Endpoints Patient
                    .requestMatchers(HttpMethod.GET, "/api/patients/me").hasRole("PATIENT")
                    .requestMatchers(HttpMethod.PUT, "/api/patients/me").hasRole("PATIENT")
                    .requestMatchers(HttpMethod.GET, "/api/documents").hasRole("PATIENT")
                    .requestMatchers(HttpMethod.POST, "/api/documents").hasRole("PATIENT")
                    .requestMatchers(HttpMethod.DELETE, "/api/documents/{id}").hasRole("PATIENT")
                    
                    // Endpoints Doctor
                    .requestMatchers(HttpMethod.POST, "/api/doctors/{id}/slots").hasRole("DOCTOR")
                    .requestMatchers(HttpMethod.PUT, "/api/slots/{id}").hasRole("DOCTOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/slots/{id}").hasRole("DOCTOR")
                    .requestMatchers(HttpMethod.GET, "/api/doctors/me/slots").hasRole("DOCTOR")
                    
                    // Tous les autres endpoints requièrent l'authentification
                    .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
