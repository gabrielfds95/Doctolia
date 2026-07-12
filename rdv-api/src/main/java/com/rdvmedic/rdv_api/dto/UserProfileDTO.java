package com.rdvmedic.rdv_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Utilisé en sortie (GET /users/me, tous les champs) ET en entrée (PATCH /users/me,
 * mise à jour partielle — voir UserController : un champ null signifie "ne pas modifier").
 * Les contraintes ci-dessous ne s'appliquent donc qu'aux champs FOURNIS (Bean Validation
 * ignore un champ null pour @Email/@Size/@Min/@Max, sauf usage de @NotNull) : elles
 * empêchent une valeur mal formée ou absurde, sans rendre les champs obligatoires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;

    @Email(message = "L'email doit être valide")
    private String email;

    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String firstName;

    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String lastName;

    // Champs patient
    @Size(max = 30, message = "Le numéro de téléphone ne peut pas dépasser 30 caractères")
    private String phoneNumber;

    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    @Min(value = 0, message = "L'âge ne peut pas être négatif")
    @Max(value = 150, message = "L'âge fourni n'est pas plausible")
    private Integer age;

    // Champs médecin
    @Size(max = 100, message = "La spécialité ne peut pas dépasser 100 caractères")
    private String speciality;

    @Size(max = 100, message = "Le département ne peut pas dépasser 100 caractères")
    private String department;

    @Min(value = 0, message = "Le nombre d'années d'expérience ne peut pas être négatif")
    @Max(value = 80, message = "Le nombre d'années d'expérience fourni n'est pas plausible")
    private Integer experienceYears;
}
