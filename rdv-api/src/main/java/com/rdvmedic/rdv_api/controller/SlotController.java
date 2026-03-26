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

/**
 * Controller REST pour les créneaux (Slots).
 *
 * Responsabilités :
 *  - Recevoir les requêtes HTTP et extraire les paramètres
 *  - Identifier l'utilisateur connecté depuis le SecurityContext (JWT déjà validé)
 *  - Déléguer la logique métier au SlotService
 *  - Convertir les entités Slot en SlotDTO avant de répondre (jamais d'entité brute)
 *
 * L'annotation @RestController = @Controller + @ResponseBody :
 * toutes les méthodes retournent automatiquement du JSON.
 *
 * Remarque sécurité : les règles d'autorisation (hasRole) sont définies dans
 * SecurityConfig. Ce controller se concentre uniquement sur le routage.
 */
@RestController
public class SlotController {

    @Autowired
    private SlotService slotService;

    // ── Lecture publique ────────────────────────────────────────────────────────

    /**
     * GET /slots → tous les créneaux (public).
     * Retourne des SlotDTO : jamais les entités JPA directement
     * → pas de risque d'exposer des données sensibles ou de boucles infinies JPA.
     */
    @GetMapping("/slots")
    public List<SlotDTO> getSlots() {
        return slotService.getSlots().stream()
                .map(SlotDTO::fromEntity) // conversion entité → DTO
                .toList();
    }

    /**
     * GET /slots/{idDoctor}/{idPatient} → créneaux partagés entre un médecin et un patient.
     * Retourne 404 si aucun créneau trouvé pour cette combinaison.
     */
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

    // ── Patient : ses propres rendez-vous ───────────────────────────────────────

    /**
     * GET /patients/me/slots → rendez-vous du patient connecté.
     *
     * "me" signifie que l'on identifie le patient depuis le JWT, pas depuis l'URL.
     * Avantage sécurité : un patient ne peut PAS voir les RDV d'un autre patient
     * en changeant un id dans l'URL — il ne peut voir que les SIENS.
     *
     * Protégé par : SecurityConfig → hasRole("PATIENT")
     */
    @GetMapping("/patients/me/slots")
    public ResponseEntity<List<SlotDTO>> getMySlots() {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // L'id du patient vient du JWT, jamais de l'URL → pas d'IDOR possible
        return ResponseEntity.ok(slotService.getSlotsByPatient(currentUser.getId())
                .stream().map(SlotDTO::fromEntity).toList());
    }

    /**
     * PATCH /slots/{id}/cancel → annule un créneau.
     * Protégé par : SecurityConfig → hasRole("PATIENT")
     * TODO : vérifier que le slot appartient bien au patient connecté
     */
    @PatchMapping("/slots/{id}/cancel")
    public ResponseEntity<SlotDTO> cancelSlot(@PathVariable Long id) {
        Slot updated = slotService.cancelSlot(id);
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    /**
     * PATCH /slots/{id} → modifie le motif d'un créneau.
     * Le body JSON contient : { "slotReason": "nouveau motif" }
     * Protégé par : SecurityConfig → hasRole("PATIENT")
     */
    @PatchMapping("/slots/{id}")
    public ResponseEntity<SlotDTO> updateSlotReason(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Slot updated = slotService.updateSlotReason(id, body.get("slotReason"));
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    /**
     * PUT /slots/{id}/complete → marque un RDV comme terminé.
     * Protégé par : SecurityConfig → hasRole("DOCTOR")
     */
    @PutMapping("/slots/{id}/complete")
    public ResponseEntity<SlotDTO> completeSlot(@PathVariable Long id) {
        Slot updated = slotService.completeSlot(id);
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    // ── Patient : réserver un créneau ───────────────────────────────────────────

    /**
     * POST /slot/{idDoctor} → réserve un créneau chez un médecin.
     *
     * L'id du patient n'est PAS dans l'URL — il est extrait du JWT.
     * Cela empêche un patient de réserver au nom d'un autre utilisateur.
     *
     * Le body JSON contient les données du Slot (date, heure, motif).
     * Retourne 201 Created avec le SlotDTO du créneau créé.
     * Retourne 409 Conflict si le créneau est déjà pris (géré par SlotService).
     *
     * Protégé par : SecurityConfig → hasRole("PATIENT")
     */
    @PostMapping("/slot/{idDoctor}")
    public ResponseEntity<SlotDTO> addSlot(
            @PathVariable Long idDoctor,
            @RequestBody Slot slot) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // currentUser.getId() = id du patient extrait du JWT (non falsifiable)
        Slot created = slotService.addSlot(idDoctor, currentUser.getId(), slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(SlotDTO.fromEntity(created));
    }

    // ── Médecin : son planning ───────────────────────────────────────────────────

    /**
     * GET /doctors/me/slots → tous les créneaux du médecin connecté.
     * "me" = id extrait du JWT, même principe que /patients/me/slots.
     * Retourne TOUS les statuts (RESERVED, CANCELLED, COMPLETED) pour afficher le planning complet.
     *
     * Protégé par : SecurityConfig → hasRole("DOCTOR")
     */
    @GetMapping("/doctors/me/slots")
    public ResponseEntity<List<SlotDTO>> getDoctorMySlots() {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(slotService.getSlotsByDoctor(currentUser.getId())
                .stream().map(SlotDTO::fromEntity).toList());
    }

    /**
     * POST /doctors/me/slots → crée une indisponibilité dans le planning du médecin.
     * Une indisponibilité = Slot avec status CANCELLED et patient null.
     * Elle bloque les réservations sur cette plage horaire.
     *
     * Protégé par : SecurityConfig → hasRole("DOCTOR")
     */
    @PostMapping("/doctors/me/slots")
    public ResponseEntity<SlotDTO> createUnavailability(@RequestBody Slot slot) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot created = slotService.createUnavailability(currentUser.getId(), slot);
        return ResponseEntity.status(HttpStatus.CREATED).body(SlotDTO.fromEntity(created));
    }

    // ── Suppression ──────────────────────────────────────────────────────────────

    /**
     * DELETE /slot/{id} → supprime définitivement un créneau (indisponibilité).
     * Retourne 204 No Content si la suppression a réussi.
     */
    @DeleteMapping("/slot/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build(); // 204 : succès sans corps de réponse
    }

    // ── Méthode utilitaire ───────────────────────────────────────────────────────

    /**
     * Extrait l'utilisateur connecté depuis le SecurityContext.
     *
     * Le SecurityContext est alimenté par JwtAuthenticationFilter à chaque requête.
     * On cast le principal en UserPrincipal (notre wrapper) pour accéder à l'id BDD.
     *
     * @return UserPrincipal ou null si non authentifié (ne devrait pas arriver
     *         si SecurityConfig est bien configuré)
     */
    private UserPrincipal extractCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return null;
        return principal;
    }
}
