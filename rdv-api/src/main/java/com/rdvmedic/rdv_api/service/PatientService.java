package com.rdvmedic.rdv_api.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.rdvmedic.rdv_api.dto.PatientCreateDTO;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.model.Role;
import com.rdvmedic.rdv_api.model.RoleName;
import com.rdvmedic.rdv_api.repository.PatientRepository;
import com.rdvmedic.rdv_api.repository.RoleRepository;
import com.rdvmedic.rdv_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.Data;


@Data
@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    // Nécessaires pour la création manuelle d'un patient (newPatient) :
    // uniqueness username/email, hachage du mot de passe, attribution du rôle.
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Patient> getPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients;
    }

    /**
     * Création manuelle d'un compte patient par un administrateur (ex. patient
     * inscrit au guichet, sans accès à l'app). Même traitement que l'inscription
     * publique (AuthService.register) : mot de passe hashé en BCrypt, ROLE_PATIENT
     * assigné, compte actif immédiatement.
     *
     * Avant ce correctif, cette méthode faisait un save() direct de l'entité reçue
     * du client : mot de passe stocké EN CLAIR, aucun rôle assigné → compte créé
     * mais inutilisable et non sécurisé. Voir CARNET-JUSTIFICATIONS.md.
     */
    public Patient newPatient(PatientCreateDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nom d'utilisateur déjà pris");
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");

        Role role = roleRepository.findByName(RoleName.ROLE_PATIENT)
                .orElseThrow(() -> new IllegalStateException("Rôle PATIENT introuvable"));

        Patient patient = Patient.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword())) // jamais en clair
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .ssn(dto.getSsn())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .age(dto.getAge() != null ? dto.getAge() : 0)
                .enabled(true)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        return patientRepository.save(patient);
    }

    public Optional<Patient> getPatient(final Long id) {
        return patientRepository.findById(id);
    }

    public void deletePatient(final Long id) {
        patientRepository.deleteById(id);
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }
}
