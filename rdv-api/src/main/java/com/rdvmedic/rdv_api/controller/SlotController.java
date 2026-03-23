package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.SlotDTO;
import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SlotController {

    @Autowired
    private SlotService slotService;

    @GetMapping("/slots")
    public List<SlotDTO> getSlots() {
        return slotService.getSlots().stream()
                .map(SlotDTO::fromEntity)
                .toList();
    }

    @GetMapping("/slots/{idDoctor}/{idPatient}")
    public List<SlotDTO> getSlotsByDoctorIdAndPatientId(
            @PathVariable Long idDoctor,
            @PathVariable Long idPatient) {
        List<Slot> slots = slotService.getSlotsByDoctorIdAndPatientId(idDoctor, idPatient);
        if (slots.isEmpty()) {
            throw new ResourceNotFoundException("Aucun créneau trouvé pour ce médecin et ce patient.");
        }
        return slots.stream().map(SlotDTO::fromEntity).toList();
    }

    @PostMapping("/slot/{idDoctor}/{idPatient}")
    public ResponseEntity<SlotDTO> addSlot(
            @PathVariable Long idDoctor,
            @PathVariable Long idPatient,
            @RequestBody Slot slot) {
        Slot created = slotService.addSlot(idDoctor, idPatient, slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(SlotDTO.fromEntity(created));
    }

    @DeleteMapping("/slot/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
