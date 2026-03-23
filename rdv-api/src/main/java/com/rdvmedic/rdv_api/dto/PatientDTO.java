package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.Patient;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String ssn;
    private String phoneNumber;
    private String address;
    private int age;

    public static PatientDTO fromEntity(Patient patient) {
        if (patient == null) return null;
        return PatientDTO.builder()
                .id(patient.getId())
                .username(patient.getUsername())
                .email(patient.getEmail())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .ssn(patient.getSsn())
                .phoneNumber(patient.getPhoneNumber())
                .address(patient.getAddress())
                .age(patient.getAge())
                .build();
    }
}
