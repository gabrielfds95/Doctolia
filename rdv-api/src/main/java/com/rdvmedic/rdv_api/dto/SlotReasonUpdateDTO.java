package com.rdvmedic.rdv_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entrée pour PATCH /slots/{id} (modification du motif de consultation).
 * Remplace un Map<String,String> non validé : bornait ici la longueur du motif,
 * un champ texte libre saisi par le patient (donc à surveiller côté validation/XSS).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotReasonUpdateDTO {

    @NotBlank(message = "Le motif ne peut pas être vide")
    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String slotReason;
}
