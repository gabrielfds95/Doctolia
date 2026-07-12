package com.rdvmedic.rdv_api.service;

import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import com.rdvmedic.rdv_api.model.Message;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.repository.MessageRepository;
import com.rdvmedic.rdv_api.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service du chat. Le RDV (Slot) vit en SQL, les messages en MongoDB : ce service
 * est le seul endroit qui parle aux deux — il vérifie l'ownership via SlotRepository
 * (SQL) avant de lire/écrire dans MessageRepository (NoSQL).
 */
@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SlotRepository slotRepository;

    /** Récupère les messages d'un RDV, dans l'ordre chronologique. Ownership vérifié. */
    public List<Message> getMessages(Long slotId, Long currentUserId) {
        Slot slot = loadRealAppointment(slotId);
        requireParticipant(slot, currentUserId);
        return messageRepository.findBySlotIdOrderBySentAtAsc(slotId);
    }

    /** Envoie un message sur un RDV. Ownership vérifié, sentAt fixé côté serveur. */
    public Message sendMessage(Long slotId, Long currentUserId, String content) {
        Slot slot = loadRealAppointment(slotId);
        requireParticipant(slot, currentUserId);

        Message message = Message.builder()
                .slotId(slotId)
                .senderId(currentUserId)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    /**
     * Charge le slot et vérifie que c'est un VRAI rendez-vous (patient non-null) —
     * une indisponibilité médecin (patient = null) n'a pas de "2 participants",
     * donc pas de chat possible dessus.
     */
    private Slot loadRealAppointment(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        if (slot.getPatient() == null) {
            throw new ResourceNotFoundException("Aucun rendez-vous trouvé pour ce créneau : " + slotId);
        }
        return slot;
    }

    /**
     * Seuls les 2 participants du RDV (le patient concerné et le médecin concerné)
     * peuvent lire ou écrire dans ce chat — même principe que requireOwnerPatient/
     * requireOwnerDoctor dans SlotService (voir AUDIT-SECURITE.md, faille 2).
     */
    private void requireParticipant(Slot slot, Long currentUserId) {
        boolean isPatient = slot.getPatient().getId().equals(currentUserId);
        boolean isDoctor = slot.getDoctor().getId().equals(currentUserId);
        if (!isPatient && !isDoctor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas participant à ce rendez-vous.");
        }
    }
}
