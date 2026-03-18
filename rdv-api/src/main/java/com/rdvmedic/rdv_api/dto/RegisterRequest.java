package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    
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
    
    @NotBlank(message = "Le type d'utilisateur est obligatoire")
    private UserType userType;
    
    // Champs optionnels pour Patient
    private String ssn;
    private String phoneNumber;
    private String address;
    private int age;
    
    // Champs optionnels pour Doctor
    private String speciality;
    private String licenseNumber;
    private String department;
    private Integer experienceYears;
}
