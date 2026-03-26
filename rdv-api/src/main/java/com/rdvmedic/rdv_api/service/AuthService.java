package com.rdvmedic.rdv_api.service;

import com.rdvmedic.rdv_api.dto.AuthResponse;
import com.rdvmedic.rdv_api.dto.LoginRequest;
import com.rdvmedic.rdv_api.dto.RegisterRequest;
import com.rdvmedic.rdv_api.model.*;
import com.rdvmedic.rdv_api.repository.DoctorRepository;
import com.rdvmedic.rdv_api.repository.PatientRepository;
import com.rdvmedic.rdv_api.repository.RoleRepository;
import com.rdvmedic.rdv_api.repository.UserRepository;
import com.rdvmedic.rdv_api.security.JwtTokenProvider;
import com.rdvmedic.rdv_api.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsable de l'authentification et de l'inscription.
 *
 * Il fait le lien entre :
 *  - le controller (AuthController) qui reçoit les requêtes HTTP
 *  - les repositories (accès BDD)
 *  - le JwtTokenProvider (génération du token)
 *  - Spring Security (vérification des credentials)
 */
@Service
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder; // BCrypt
    @Autowired private JwtTokenProvider tokenProvider;

    // Durée d'expiration du token (en ms), lue depuis application.properties
    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Connexion d'un utilisateur existant.
     *
     * Flux :
     *  1. authenticationManager.authenticate() → Spring Security compare le mot de passe
     *     fourni (en clair) avec le hash BCrypt en BDD via CustomUserDetailsService
     *  2. Si OK : génère un JWT et retourne un AuthResponse avec toutes les infos du user
     *  3. Si KO : lève une AuthenticationException (gérée par GlobalExceptionHandler → 401)
     *
     * @param request contient username + password en clair
     * @return AuthResponse : token JWT + infos utilisateur (id, username, rôles...)
     */
    public AuthResponse login(LoginRequest request) {
        // Délègue la vérification à Spring Security (CustomUserDetailsService + BCrypt)
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // Stocke l'authentification dans le contexte pour cette requête
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Génère le JWT signé
        String token = tokenProvider.generateToken(auth);

        // Extrait les infos depuis le principal (notre UserPrincipal wrappé)
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Set<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        // Construit la réponse envoyée au client (Angular/Mobile)
        return AuthResponse.builder()
                .id(principal.getId())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .token(token)
                .expiresIn(jwtExpirationMs)
                .roles(roles)
                .build();
    }

    /**
     * Inscription d'un nouvel utilisateur (patient ou médecin).
     *
     * @Transactional garantit que si la sauvegarde échoue à mi-chemin,
     * toute la transaction est annulée (pas de demi-création en BDD).
     *
     * Différence patient/médecin :
     *  - Patient  : enabled = true → peut se connecter immédiatement
     *  - Médecin  : enabled = false → doit être approuvé par un admin d'abord
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Unicité du username et de l'email vérifiée avant tout
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Nom d'utilisateur déjà pris");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email déjà utilisé");

        // Hachage du mot de passe en clair → hash BCrypt stocké en BDD
        // Le mot de passe en clair n'est JAMAIS persisté
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // ── Inscription patient ───────────────────────────────────────────────
        if (request.getUserType() == UserType.PATIENT) {
            // Récupère le rôle ROLE_PATIENT depuis la table des rôles
            Role role = roleRepository.findByName(RoleName.ROLE_PATIENT)
                    .orElseThrow(() -> new RuntimeException("Rôle PATIENT introuvable"));

            // Construit l'entité Patient via le builder Lombok (@Builder hérité de User)
            Patient patient = Patient.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(encodedPassword)   // jamais le mot de passe en clair
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .ssn(request.getSsn())
                    .phoneNumber(request.getPhoneNumber())
                    .address(request.getAddress())
                    .age(request.getAge())
                    .enabled(true)               // patient actif immédiatement
                    .roles(new HashSet<>(Set.of(role)))
                    .build();

            Patient saved = patientRepository.save(patient);
            // Génère un token directement → connexion automatique après inscription
            return buildAuthResponse(saved, Set.of("ROLE_PATIENT"));
        }

        // ── Inscription médecin ───────────────────────────────────────────────
        if (request.getUserType() == UserType.DOCTOR) {
            Role role = roleRepository.findByName(RoleName.ROLE_DOCTOR)
                    .orElseThrow(() -> new RuntimeException("Rôle DOCTOR introuvable"));

            Doctor doctor = Doctor.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .speciality(request.getSpeciality())
                    .licenseNumber(request.getLicenseNumber())
                    .department(request.getDepartment())
                    .experienceYears(request.getExperienceYears())
                    .enabled(false)              // ← BLOQUÉ en attente de validation admin
                    .roles(new HashSet<>(Set.of(role)))
                    .build();

            Doctor saved = doctorRepository.save(doctor);

            // Pas de token JWT : le médecin ne peut PAS se connecter tant que enabled = false
            // Spring Security refusera la connexion via CustomUserDetailsService.isEnabled()
            return AuthResponse.builder()
                    .id(saved.getId())
                    .username(saved.getUsername())
                    .email(saved.getEmail())
                    .firstName(saved.getFirstName())
                    .lastName(saved.getLastName())
                    .enabled(false)  // signal au frontend pour afficher un message d'attente
                    .roles(Set.of("ROLE_DOCTOR"))
                    .build();
        }

        throw new RuntimeException("UserType non supporté : " + request.getUserType());
    }

    /**
     * Génère un AuthResponse complet (avec token JWT) depuis une entité User sauvegardée.
     *
     * Utilisé après inscription patient pour connecter directement l'utilisateur
     * sans refaire une passe d'authentification (le mot de passe est déjà hashé).
     *
     * On construit manuellement un Authentication Spring Security à partir de l'entité
     * sauvegardée, puis on génère le token normalement.
     */
    private AuthResponse buildAuthResponse(User user, Set<String> roles) {
        // Wrap l'entité User dans notre UserPrincipal (adapté à Spring Security)
        UserPrincipal principal = UserPrincipal.fromUser(user);

        // Crée un Authentication sans mot de passe (credentials = null car déjà validé)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        String token = tokenProvider.generateToken(auth);

        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .token(token)
                .expiresIn(jwtExpirationMs)
                .roles(roles)
                .enabled(user.getEnabled())
                .build();
    }
}
