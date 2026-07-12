package com.rdvmedic.rdv_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entrée pour POST /slots/{id}/messages. Seul le contenu vient du client :
 * slotId (URL), senderId (JWT) et sentAt (serveur) ne sont jamais fournis par lui
 * — même principe que SlotCreateDTO/PatientCreateDTO (voir AUDIT-SECURITE.md, faille 4).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateDTO {

    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(max = 2000, message = "Le message ne peut pas dépasser 2000 caractères")
    private String content;
}
