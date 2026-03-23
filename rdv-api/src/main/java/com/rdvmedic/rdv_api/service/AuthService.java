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

@Service
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        String token = tokenProvider.generateToken(auth);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Set<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .id(principal.getId())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .token(token)
                .expiresIn(jwtExpirationMs)
                .roles(roles)
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Nom d'utilisateur déjà pris");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email déjà utilisé");

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        if (request.getUserType() == UserType.PATIENT) {
            Role role = roleRepository.findByName(RoleName.ROLE_PATIENT)
                    .orElseThrow(() -> new RuntimeException("Rôle PATIENT introuvable"));

            Patient patient = Patient.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .ssn(request.getSsn())
                    .phoneNumber(request.getPhoneNumber())
                    .address(request.getAddress())
                    .age(request.getAge())
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(role)))
                    .build();
            Patient saved = patientRepository.save(patient);
            return buildAuthResponse(saved, Set.of("ROLE_PATIENT"));
        }

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
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(role)))
                    .build();
            Doctor saved = doctorRepository.save(doctor);
            return buildAuthResponse(saved, Set.of("ROLE_DOCTOR"));
        }

        throw new RuntimeException("UserType non supporté : " + request.getUserType());
    }

    /** Génère un token directement depuis l'entité sauvegardée, sans re-authentification. */
    private AuthResponse buildAuthResponse(User user, Set<String> roles) {
        UserPrincipal principal = UserPrincipal.fromUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        String token = tokenProvider.generateToken(auth);

        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .expiresIn(jwtExpirationMs)
                .roles(roles)
                .build();
    }
}
