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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Enregistrement d'un nouvel utilisateur: {}", registerRequest.getUsername());

        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("L'identifiant utilisateur est déjà pris");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("L'email est déjà utilisé");
        }

        // Créer le rôle
        RoleName roleName = registerRequest.getUserType() == UserType.PATIENT ? 
                RoleName.ROLE_PATIENT : RoleName.ROLE_DOCTOR;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Le rôle n'existe pas: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user;

        if (registerRequest.getUserType() == UserType.PATIENT) {
            Patient patient = new Patient();
            patient.setUsername(registerRequest.getUsername());
            patient.setEmail(registerRequest.getEmail());
            patient.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            patient.setFirstName(registerRequest.getFirstName());
            patient.setLastName(registerRequest.getLastName());
            patient.setUserType(UserType.PATIENT);
            patient.setRoles(roles);
            patient.setEnabled(true);
            
            // Champs optionnels Patient
            if (registerRequest.getSsn() != null) patient.setSsn(registerRequest.getSsn());
            if (registerRequest.getPhoneNumber() != null) patient.setPhoneNumber(registerRequest.getPhoneNumber());
            if (registerRequest.getAddress() != null) patient.setAddress(registerRequest.getAddress());
            patient.setAge(registerRequest.getAge());

            user = patientRepository.save(patient);
        } else {
            Doctor doctor = new Doctor();
            doctor.setUsername(registerRequest.getUsername());
            doctor.setEmail(registerRequest.getEmail());
            doctor.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            doctor.setFirstName(registerRequest.getFirstName());
            doctor.setLastName(registerRequest.getLastName());
            doctor.setUserType(UserType.DOCTOR);
            doctor.setRoles(roles);
            doctor.setEnabled(true);

            // Champs optionnels Doctor
            if (registerRequest.getSpeciality() != null) doctor.setSpeciality(registerRequest.getSpeciality());
            if (registerRequest.getLicenseNumber() != null) doctor.setLicenseNumber(registerRequest.getLicenseNumber());
            if (registerRequest.getDepartment() != null) doctor.setDepartment(registerRequest.getDepartment());
            if (registerRequest.getExperienceYears() != null) doctor.setExperienceYears(registerRequest.getExperienceYears());

            user = doctorRepository.save(doctor);
        }

        String token = generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        log.info("Utilisateur enregistré avec succès: {}", user.getUsername());

        return buildAuthResponse(user, token, refreshToken);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Tentative de connexion pour l'utilisateur: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String token = generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        log.info("Utilisateur connecté avec succès: {}", user.getUsername());

        return buildAuthResponse(user, token, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token invalide ou expiré");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String newToken = generateToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        return buildAuthResponse(user, newToken, newRefreshToken);
    }

    private String generateToken(User user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        return jwtTokenProvider.generateTokenFromUsername(user.getUsername(), 
                roles.stream().collect(Collectors.toList()));
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .roles(roles)
                .build();
    }
}
