package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.SlotDTO;
import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.security.UserPrincipal;
import com.rdvmedic.rdv_api.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // ── Patient : ses propres RDV ─────────────────────────────────────────────

    @GetMapping("/patients/me/slots")
    public ResponseEntity<List<SlotDTO>> getMySlots() {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(slotService.getSlotsByPatient(currentUser.getId())
                .stream().map(SlotDTO::fromEntity).toList());
    }

    @PatchMapping("/slots/{id}/cancel")
    public ResponseEntity<SlotDTO> cancelSlot(@PathVariable Long id) {
        Slot updated = slotService.cancelSlot(id);
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    @PatchMapping("/slots/{id}")
    public ResponseEntity<SlotDTO> updateSlotReason(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Slot updated = slotService.updateSlotReason(id, body.get("slotReason"));
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    @PutMapping("/slots/{id}/complete")
    public ResponseEntity<SlotDTO> completeSlot(@PathVariable Long id) {
        Slot updated = slotService.completeSlot(id);
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    // ── Patient : réserver un créneau ─────────────────────────────────────────

    @PostMapping("/slot/{idDoctor}")
    public ResponseEntity<SlotDTO> addSlot(
            @PathVariable Long idDoctor,
            @RequestBody Slot slot) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot created = slotService.addSlot(idDoctor, currentUser.getId(), slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(SlotDTO.fromEntity(created));
    }

    // ── Doctor : son planning ─────────────────────────────────────────────────

    @GetMapping("/doctors/me/slots")
    public ResponseEntity<List<SlotDTO>> getDoctorMySlots() {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(slotService.getSlotsByDoctor(currentUser.getId())
                .stream().map(SlotDTO::fromEntity).toList());
    }

    @PostMapping("/doctors/me/slots")
    public ResponseEntity<SlotDTO> createUnavailability(@RequestBody Slot slot) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot created = slotService.createUnavailability(currentUser.getId(), slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(SlotDTO.fromEntity(created));
    }

    // ── Admin / général ───────────────────────────────────────────────────────

    @DeleteMapping("/slot/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserPrincipal extractCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return null;
        return principal;
    }
}
