package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.PatientDTO;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<PatientDTO> newPatient(@RequestBody Patient patient) {
        Patient saved = patientService.newPatient(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientDTO.fromEntity(saved));
    }

    @DeleteMapping("/patient/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
