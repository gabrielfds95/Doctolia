package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.PublicSlotDTO;
import com.rdvmedic.rdv_api.dto.SlotCreateDTO;
import com.rdvmedic.rdv_api.dto.SlotDTO;
import com.rdvmedic.rdv_api.dto.SlotReasonUpdateDTO;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.security.UserPrincipal;
import com.rdvmedic.rdv_api.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     *
     * Retourne PublicSlotDTO, PAS SlotDTO : SlotDTO embarque un PatientDTO complet
     * (SSN, téléphone, adresse) qui serait exposé à n'importe qui, sans authentification.
     * PublicSlotDTO ne contient que ce qui est nécessaire pour afficher un calendrier
     * (créneau pris/libre) — aucune identité patient, aucun motif de consultation.
     *
     * (Faille corrigée : voir AUDIT-SECURITE.md, faille 1)
     */
    @GetMapping("/slots")
    public List<PublicSlotDTO> getSlots() {
        return slotService.getSlots().stream()
                .map(PublicSlotDTO::fromEntity)
                .toList();
    }

    // GET /slots/{idDoctor}/{idPatient} a été SUPPRIMÉ (pas juste fermé) : requête ciblée
    // sur un patient précis, sans cas d'usage anonyme légitime, redondante avec
    // /patients/me/slots et /doctors/me/slots (qui sont, eux, correctement scopés par
    // ownership via le JWT). Confirmé inutilisé par rdv_medic_front et doctolia-mobile
    // avant suppression. Voir AUDIT-SECURITE.md, faille 1 (gap résiduel → fermé par
    // réduction de la surface d'attaque plutôt que par un contrôle d'ownership de plus).

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
     *
     * Ownership : l'id du patient vient du JWT (currentUser.getId()), jamais de l'URL.
     * SlotService vérifie que ce patient est bien le propriétaire du créneau ({id})
     * et lève 403 sinon (voir AUDIT-SECURITE.md, faille 2).
     */
    @PatchMapping("/slots/{id}/cancel")
    public ResponseEntity<SlotDTO> cancelSlot(@PathVariable Long id) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot updated = slotService.cancelSlot(id, currentUser.getId());
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    /**
     * PATCH /slots/{id} → modifie le motif d'un créneau.
     * Le body JSON contient : { "slotReason": "nouveau motif" }
     * Protégé par : SecurityConfig → hasRole("PATIENT")
     *
     * Ownership : même principe que cancelSlot, id patient extrait du JWT.
     */
    @PatchMapping("/slots/{id}")
    public ResponseEntity<SlotDTO> updateSlotReason(
            @PathVariable Long id,
            @Valid @RequestBody SlotReasonUpdateDTO body) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot updated = slotService.updateSlotReason(id, body.getSlotReason(), currentUser.getId());
        return ResponseEntity.ok(SlotDTO.fromEntity(updated));
    }

    /**
     * PUT /slots/{id}/complete → marque un RDV comme terminé.
     * Protégé par : SecurityConfig → hasRole("DOCTOR")
     *
     * Ownership : l'id du médecin vient du JWT — un médecin ne peut clôturer
     * que les créneaux de SON planning.
     */
    @PutMapping("/slots/{id}/complete")
    public ResponseEntity<SlotDTO> completeSlot(@PathVariable Long id) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot updated = slotService.completeSlot(id, currentUser.getId());
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
     *
     * Le body est un SlotCreateDTO (pas l'entité Slot) : le client ne peut fournir
     * que date/heure/motif, jamais id/status/doctor/patient (voir SlotCreateDTO).
     */
    @PostMapping("/slot/{idDoctor}")
    public ResponseEntity<SlotDTO> addSlot(
            @PathVariable Long idDoctor,
            @Valid @RequestBody SlotCreateDTO dto) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // currentUser.getId() = id du patient extrait du JWT (non falsifiable)
        Slot created = slotService.addSlot(idDoctor, currentUser.getId(), dto.toEntity());
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
     *
     * Même DTO d'entrée que addSlot (SlotCreateDTO) : même forme (date/heure/motif),
     * même raison de sécurité (pas d'id/status/doctor/patient fournis par le client).
     */
    @PostMapping("/doctors/me/slots")
    public ResponseEntity<SlotDTO> createUnavailability(@Valid @RequestBody SlotCreateDTO dto) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Slot created = slotService.createUnavailability(currentUser.getId(), dto.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(SlotDTO.fromEntity(created));
    }

    // ── Suppression ──────────────────────────────────────────────────────────────

    /**
     * DELETE /slot/{id} → supprime définitivement un créneau (indisponibilité).
     * Protégé par : SecurityConfig → hasRole("DOCTOR")
     * Ownership : id médecin extrait du JWT, un médecin ne peut supprimer
     * qu'une indisponibilité de SON planning.
     * Retourne 204 No Content si la suppression a réussi.
     */
    @DeleteMapping("/slot/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        slotService.deleteSlot(id, currentUser.getId());
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
