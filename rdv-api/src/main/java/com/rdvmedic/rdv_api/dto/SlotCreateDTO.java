package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.Slot;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO d'entrée pour la création d'un créneau (réservation par un patient,
 * ou indisponibilité posée par un médecin — même forme dans les deux cas).
 *
 * Remplace l'ancien @RequestBody Slot : recevoir directement l'entité JPA
 * permettait à un client de fournir n'importe quel champ dans le JSON,
 * y compris "id". Comme Slot a un id généré en IDENTITY, un id non-null
 * fourni par le client aurait fait passer slotRepository.save(slot) en
 * UPDATE au lieu d'INSERT — un attaquant aurait pu écraser un créneau
 * existant (y compris celui d'un autre patient) rien qu'en devinant son id.
 * Ce DTO ne porte aucun champ id/status/doctor/patient : ils sont toujours
 * fixés côté serveur (voir SlotService.addSlot / createUnavailability).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotCreateDTO {

    @NotNull(message = "La date du créneau est obligatoire")
    private LocalDate slotDate;

    @NotNull(message = "L'heure de début est obligatoire")
    private LocalTime slotTime;

    @NotNull(message = "L'heure de fin est obligatoire")
    private LocalTime endTime;

    private String slotReason;

    /** Construit une entité Slot neuve (id null → garantit un INSERT, jamais un UPDATE). */
    public Slot toEntity() {
        return Slot.builder()
                .slotDate(slotDate)
                .slotTime(slotTime)
                .endTime(endTime)
                .slotReason(slotReason)
                .build();
    }
}
