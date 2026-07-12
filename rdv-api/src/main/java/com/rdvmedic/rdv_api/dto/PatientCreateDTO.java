package com.rdvmedic.rdv_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'entrée pour la création manuelle d'un patient par un administrateur
 * (POST /patient, réservé à ROLE_ADMIN).
 *
 * Remplace l'ancien @RequestBody Patient : recevoir l'entité JPA directement
 * exposait tous ses champs à l'écriture, y compris "roles" et "enabled" —
 * un client aurait pu s'auto-attribuer n'importe quel rôle (ex. ROLE_ADMIN)
 * en le glissant dans le JSON. Ce DTO ne porte que les champs métier ;
 * rôle et statut "enabled" sont toujours fixés côté serveur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreateDTO {

    @NotBlank(message = "L'identifiant utilisateur est obligatoire")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit avoir au moins 8 caractères")
    private String password;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom de famille est obligatoire")
    private String lastName;

    private String ssn;
    private String phoneNumber;
    private String address;
    private Integer age;
}
