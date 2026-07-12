package com.rdvmedic.rdv_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.model.SlotStatus;
import com.rdvmedic.rdv_api.repository.DoctorRepository;
import com.rdvmedic.rdv_api.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.repository.SlotRepository;

import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import lombok.Data;

/**
 * Service gérant toute la logique métier autour des créneaux (Slot).
 *
 * Les statuts possibles d'un Slot :
 *  - AVAILABLE  : créneau libre, pas encore réservé
 *  - RESERVED   : réservé par un patient
 *  - CANCELLED  : annulé par le patient OU indisponibilité posée par le médecin
 *                 (distinction : si patient == null → c'est une indisponibilité médecin)
 *  - COMPLETED  : consultation terminée (marquée par le médecin)
 *
 * Le service est l'unique endroit où ces règles métier sont appliquées.
 * Les controllers ne font que déléguer ici.
 */
@Data
@Service
public class SlotService {

    @Autowired
    private SlotRepository slotRepository;   // accès table slots

    @Autowired
    private DoctorRepository doctorRepository; // accès table doctors

    @Autowired
    private PatientRepository patientRepository; // accès table patients

    /** Récupère un créneau par son id (Optional : peut ne pas exister). */
    public Optional<Slot> getSlot(final Long id) {
        return slotRepository.findById(id);
    }

    /** Récupère tous les créneaux de la BDD (endpoint public /slots). */
    public List<Slot> getSlots() {
        return slotRepository.findAll();
    }

    /**
     * Supprime un créneau (utilisé pour les indisponibilités médecin).
     * Ownership : seul le médecin propriétaire du créneau peut le supprimer.
     */
    public void deleteSlot(final Long id, final Long currentDoctorId) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + id));
        requireOwnerDoctor(slot, currentDoctorId);
        slotRepository.deleteById(id);
    }

    /**
     * Réserve un créneau pour un patient chez un médecin donné.
     *
     * Étapes :
     *  1. Vérifier que le médecin et le patient existent
     *  2. Vérifier l'absence de conflits sur cette plage horaire
     *  3. Rattacher le médecin et le patient au Slot
     *  4. Passer le statut à RESERVED et sauvegarder
     *
     * Conflits détectés :
     *  - Un autre RESERVED sur la même date/heure → double réservation impossible
     *  - Une indisponibilité (CANCELLED sans patient) qui couvre l'heure demandée
     *
     * @param doctorId  id du médecin choisi
     * @param patientId id du patient connecté (extrait du JWT par le controller)
     * @param slot      données du créneau envoyées par le frontend (date, heure, motif)
     */
    public Slot addSlot(Long doctorId, Long patientId, Slot slot) {
        // Vérifie que le médecin existe et est actif (enabled), sinon lève une 404/403
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable : " + doctorId));
        if (!Boolean.TRUE.equals(doctor.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce médecin n'est pas encore disponible.");
        }

        // Vérifie que le patient existe en BDD, sinon lève une 404
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable : " + patientId));

        // Récupère tous les créneaux du médecin et cherche un conflit
        boolean conflict = slotRepository.findByDoctorId(doctorId).stream().anyMatch(existing -> {
            // Pas de conflit si ce n'est pas le même jour
            if (!existing.getSlotDate().equals(slot.getSlotDate())) return false;

            // Conflit si un créneau RESERVED existe déjà à cette heure exacte
            if (existing.getStatus() == SlotStatus.RESERVED) {
                return existing.getSlotTime().equals(slot.getSlotTime());
            }

            // Conflit si une indisponibilité (CANCELLED sans patient) couvre l'heure
            // Exemple : médecin bloque 12h00 → 13h00, patient demande 12h30 → conflit
            if (existing.getStatus() == SlotStatus.CANCELLED && existing.getPatient() == null) {
                return !slot.getSlotTime().isBefore(existing.getSlotTime())     // heure >= début bloc
                        && slot.getSlotTime().isBefore(existing.getEndTime());  // heure < fin bloc
            }
            return false;
        });

        if (conflict) {
            // Renvoie un 409 Conflict au client
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce créneau est déjà réservé ou indisponible.");
        }

        // Attache les entités et définit le statut
        slot.setDoctor(doctor);
        slot.setPatient(patient);
        slot.setStatus(SlotStatus.RESERVED); // Toute réservation est RESERVED dès la création

        return slotRepository.save(slot);
    }

    /**
     * Récupère tous les créneaux d'un médecin donné.
     * Utilisé par le calendrier hebdomadaire (SlotListComponent Angular)
     * pour afficher les créneaux pris et les indisponibilités.
     */
    public List<Slot> getSlotsByDoctor(Long idDoctor) {
        return slotRepository.findByDoctorId(idDoctor);
    }

    /**
     * Récupère tous les créneaux d'un patient.
     * Utilisé par "Mes rendez-vous" (patient connecté → id extrait du JWT).
     */
    public List<Slot> getSlotsByPatient(Long patientId) {
        return slotRepository.findByPatientId(patientId);
    }

    /**
     * Annule un créneau réservé par un patient.
     * Le slot passe en CANCELLED mais reste en BDD (historique).
     * Note : le patient est conservé sur le slot — c'est le patient null qui
     * distingue une indisponibilité médecin d'un RDV annulé par le patient.
     *
     * @param slotId          id du créneau à annuler (vient de l'URL)
     * @param currentPatientId id du patient connecté, extrait du JWT par le controller
     *                         (jamais fourni par le client) → vérifie l'ownership
     */
    public Slot cancelSlot(Long slotId, Long currentPatientId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        requireOwnerPatient(slot, currentPatientId);
        slot.setStatus(SlotStatus.CANCELLED);
        return slotRepository.save(slot);
    }

    /**
     * Met à jour le motif d'un créneau réservé.
     * Permet au patient de modifier la raison de sa consultation avant le RDV.
     *
     * @param currentPatientId id du patient connecté, extrait du JWT → vérifie l'ownership
     */
    public Slot updateSlotReason(Long slotId, String newReason, Long currentPatientId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        requireOwnerPatient(slot, currentPatientId);
        slot.setSlotReason(newReason);
        return slotRepository.save(slot);
    }

    /**
     * Marque un RDV comme terminé (action du médecin après la consultation).
     * Statut COMPLETED → apparaîtra dans les "RDV passés" côté patient.
     *
     * @param currentDoctorId id du médecin connecté, extrait du JWT → vérifie l'ownership
     */
    public Slot completeSlot(Long slotId, Long currentDoctorId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        requireOwnerDoctor(slot, currentDoctorId);
        slot.setStatus(SlotStatus.COMPLETED);
        return slotRepository.save(slot);
    }

    /**
     * Vérifie que le créneau appartient bien au patient connecté.
     * Lève 403 (pas 404) : le créneau existe, l'appelant n'a juste pas le droit d'agir dessus.
     * C'est le cœur du correctif de la faille 2 (voir AUDIT-SECURITE.md) : avant, n'importe
     * quel patient authentifié pouvait annuler/modifier le RDV d'un autre patient.
     */
    private void requireOwnerPatient(Slot slot, Long currentPatientId) {
        if (slot.getPatient() == null || !slot.getPatient().getId().equals(currentPatientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce créneau ne vous appartient pas.");
        }
    }

    /** Même principe que requireOwnerPatient, côté médecin (son propre planning uniquement). */
    private void requireOwnerDoctor(Slot slot, Long currentDoctorId) {
        if (slot.getDoctor() == null || !slot.getDoctor().getId().equals(currentDoctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce créneau n'appartient pas à votre planning.");
        }
    }

    /**
     * Crée une indisponibilité dans le planning d'un médecin.
     *
     * Une indisponibilité est un Slot avec :
     *  - status  = CANCELLED
     *  - patient = null  (différenciateur clé avec un RDV annulé)
     *
     * Côté calendrier Angular, ces plages apparaissent grisées et bloquent
     * les nouvelles réservations via la vérification de conflit dans addSlot().
     */
    public Slot createUnavailability(Long doctorId, Slot slot) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable : " + doctorId));
        slot.setDoctor(doctor);
        slot.setPatient(null);                    // null = indisponibilité (pas un RDV)
        slot.setStatus(SlotStatus.CANCELLED);     // même statut qu'annulé, patient null = marqueur
        return slotRepository.save(slot);
    }
}
