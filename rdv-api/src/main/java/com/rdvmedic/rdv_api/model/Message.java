package com.rdvmedic.rdv_api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Message de chat rattaché à un RDV (Slot). Stocké en MongoDB, pas en SQL :
 * voir CARNET-JUSTIFICATIONS.md pour la justification du choix NoSQL ici.
 *
 * slotId/senderId sont volontairement de simples Long (pas de relation JPA) :
 * MongoDB ne connaît pas les entités SQL, la cohérence référentielle
 * (le slot et l'expéditeur existent bien) est vérifiée côté service,
 * dans SQL, avant toute lecture/écriture d'un message.
 */
@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    private Long slotId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;
}
