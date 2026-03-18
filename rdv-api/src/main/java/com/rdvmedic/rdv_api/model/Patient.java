package com.rdvmedic.rdv_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@Table(name = "patients")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DiscriminatorValue("PATIENT")
public class Patient extends User {

    @Column(name = "ssn")
    private String ssn;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    private int age;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Slot> slots;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Document> documents;
}
