package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.DoctorDTO;
import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private DoctorRepository doctorRepository;

    /** Liste des médecins en attente de validation */
    @GetMapping("/doctors/pending")
    public List<DoctorDTO> getPendingDoctors() {
        return doctorRepository.findByEnabledFalse()
                .stream().map(DoctorDTO::fromEntity).toList();
    }

    /** Approuver un médecin */
    @PutMapping("/doctors/{id}/approve")
    public ResponseEntity<DoctorDTO> approveDoctor(@PathVariable Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable : " + id));
        doctor.setEnabled(true);
        return ResponseEntity.ok(DoctorDTO.fromEntity(doctorRepository.save(doctor)));
    }

    /** Rejeter (supprimer) un médecin */
    @DeleteMapping("/doctors/{id}/reject")
    public ResponseEntity<Void> rejectDoctor(@PathVariable Long id) {
        if (!doctorRepository.existsById(id))
            throw new ResourceNotFoundException("Médecin introuvable : " + id);
        doctorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
