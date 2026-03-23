package com.rdvmedic.rdv_api.config;

import com.rdvmedic.rdv_api.model.*;
import com.rdvmedic.rdv_api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        initRoles();
        if (userRepository.count() == 0) {
            initSeedData();
        }
    }

    private void initRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }
    }

    private void initSeedData() {
        String password = passwordEncoder.encode("password");

        Role doctorRole  = roleRepository.findByName(RoleName.ROLE_DOCTOR).orElseThrow();
        Role patientRole = roleRepository.findByName(RoleName.ROLE_PATIENT).orElseThrow();

        // ─── Médecins ────────────────────────────────────────────────────────
        Doctor doc1 = Doctor.builder()
                .username("doc.john").email("john.dupont@rdvmedic.local").password(password)
                .firstName("John").lastName("Dupont")
                .speciality("Ostéopathie").licenseNumber("LIC-OST-001")
                .department("Général").experienceYears(8)
                .enabled(true).roles(new HashSet<>(Set.of(doctorRole))).build();

        Doctor doc2 = Doctor.builder()
                .username("doc.paul").email("paul.dupard@rdvmedic.local").password(password)
                .firstName("Paul").lastName("Dupard")
                .speciality("Dentiste").licenseNumber("LIC-DEN-002")
                .department("Dentaire").experienceYears(5)
                .enabled(true).roles(new HashSet<>(Set.of(doctorRole))).build();

        Doctor savedDoc1 = doctorRepository.save(doc1);
        Doctor savedDoc2 = doctorRepository.save(doc2);

        // ─── Patients ────────────────────────────────────────────────────────
        Patient pat1 = Patient.builder()
                .username("pat.marc").email("marc.galar@rdvmedic.local").password(password)
                .firstName("Marc").lastName("Galar")
                .ssn("SSN-001").phoneNumber("0600000001").address("1 rue de Paris").age(34)
                .enabled(true).roles(new HashSet<>(Set.of(patientRole))).build();

        Patient pat2 = Patient.builder()
                .username("pat.jean").email("jean.dude@rdvmedic.local").password(password)
                .firstName("Jean").lastName("Dude")
                .ssn("SSN-002").phoneNumber("0600000002").address("2 rue de Lyon").age(21)
                .enabled(true).roles(new HashSet<>(Set.of(patientRole))).build();

        Patient savedPat1 = patientRepository.save(pat1);
        Patient savedPat2 = patientRepository.save(pat2);

        // ─── Créneaux de démo ────────────────────────────────────────────────
        slotRepository.saveAll(List.of(
                Slot.builder()
                        .slotDate(LocalDate.of(2026, 4, 1)).slotTime(LocalTime.of(9, 30))
                        .endTime(LocalTime.of(10, 0)).slotReason("Caries")
                        .status(SlotStatus.RESERVED).doctor(savedDoc2).patient(savedPat1).build(),
                Slot.builder()
                        .slotDate(LocalDate.of(2026, 4, 3)).slotTime(LocalTime.of(14, 30))
                        .endTime(LocalTime.of(15, 0)).slotReason("Rhume")
                        .status(SlotStatus.RESERVED).doctor(savedDoc1).patient(savedPat2).build(),
                Slot.builder()
                        .slotDate(LocalDate.of(2026, 4, 5)).slotTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(10, 30)).slotReason("Consultation")
                        .status(SlotStatus.AVAILABLE).doctor(savedDoc1).patient(null).build()
        ));

        log.info("✅ Seed data initialisé — password: 'password' pour tous les comptes.");
    }
}
