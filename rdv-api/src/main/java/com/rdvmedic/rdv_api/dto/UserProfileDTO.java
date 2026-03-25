package com.rdvmedic.rdv_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    // Champs patient
    private String phoneNumber;
    private String address;
    private Integer age;
    // Champs médecin
    private String speciality;
    private String department;
    private Integer experienceYears;
}
