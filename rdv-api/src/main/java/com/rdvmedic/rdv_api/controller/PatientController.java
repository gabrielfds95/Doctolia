package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.PatientCreateDTO;
import com.rdvmedic.rdv_api.dto.PatientDTO;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// GET/POST/DELETE ici sont réservés à ROLE_ADMIN (voir SecurityConfig) : gestion
// administrative des comptes patients, pas un endpoint public d'inscription
// (l'inscription publique passe par POST /register → AuthController).
@RestController
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping("/patients")
    public List<PatientDTO> getPatients() {
        return patientService.getPatients().stream()
                .map(PatientDTO::fromEntity)
                .toList();
    }

    @PostMapping("/patient")
    public ResponseEntity<PatientDTO> newPatient(@Valid @RequestBody PatientCreateDTO dto) {
        Patient saved = patientService.newPatient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientDTO.fromEntity(saved));
    }

    @DeleteMapping("/patient/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
