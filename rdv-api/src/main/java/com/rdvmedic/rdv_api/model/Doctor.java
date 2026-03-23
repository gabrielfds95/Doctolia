package com.rdvmedic.rdv_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@DiscriminatorValue("DOCTOR")
public class Doctor extends User {

    @Column(name = "speciality")
    private String speciality;

    @Column(name = "license_number", unique = true)
    private String licenseNumber;

    @Column(name = "department")
    private String department;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Slot> slots;
}
