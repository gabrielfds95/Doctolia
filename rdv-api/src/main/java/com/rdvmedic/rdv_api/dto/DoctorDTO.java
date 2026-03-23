package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.Doctor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoctorDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String speciality;
    private String licenseNumber;
    private String department;
    private Integer experienceYears;

    public static DoctorDTO fromEntity(Doctor doctor) {
        if (doctor == null) return null;
        return DoctorDTO.builder()
                .id(doctor.getId())
                .username(doctor.getUsername())
                .email(doctor.getEmail())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .speciality(doctor.getSpeciality())
                .licenseNumber(doctor.getLicenseNumber())
                .department(doctor.getDepartment())
                .experienceYears(doctor.getExperienceYears())
                .build();
    }
}
