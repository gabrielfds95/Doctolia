package com.rdvmedic.rdv_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    
    @NotBlank(message = "L'identifiant utilisateur ou email est obligatoire")
    private String username;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
