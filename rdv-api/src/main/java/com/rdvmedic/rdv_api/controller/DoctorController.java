package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.DoctorDTO;
import com.rdvmedic.rdv_api.dto.SlotDTO;
import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import com.rdvmedic.rdv_api.service.DoctorService;
import com.rdvmedic.rdv_api.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SlotService slotService;

    @GetMapping("/doctors")
    public List<DoctorDTO> getDoctors() {
        return doctorService.getDoctors().stream()
                .map(DoctorDTO::fromEntity)
                .toList();
    }

    @GetMapping("/doctor/{idDoctor}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long idDoctor) {
        return doctorService.getDoctorById(idDoctor)
                .map(DoctorDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable : " + idDoctor));
    }

    @GetMapping("/doctors/{idDoctor}/slots")
    public List<SlotDTO> getSlotsByDoctor(@PathVariable Long idDoctor) {
        return slotService.getSlotsByDoctor(idDoctor).stream()
                .map(SlotDTO::fromEntity)
                .toList();
    }

    @DeleteMapping("/doctor/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }
}
