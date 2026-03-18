package com.rdvmedic.rdv_api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;
    
    @Column(name = "slot_time", nullable = false)
    private LocalTime slotTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "slot_reason")
    private String slotReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @JsonIgnoreProperties({"slots", "roles"})
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    @JsonIgnoreProperties({"slots", "documents", "roles"})
    private Patient patient;
}
