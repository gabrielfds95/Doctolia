package com.rdvmedic.rdv_api.repository;

import com.rdvmedic.rdv_api.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Composant d'accès aux données NoSQL (MongoDB) — équivalent, pour les messages,
 * de ce que SlotRepository/UserRepository sont pour le SQL (JpaRepository).
 * Même principe Spring Data : méthode dérivée du nom, requête générée automatiquement,
 * pas de concaténation ni de requête manuelle.
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findBySlotIdOrderBySentAtAsc(Long slotId);
}
