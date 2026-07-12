package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.model.SlotStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Version publique d'un créneau, utilisée sur les endpoints accessibles
 * sans authentification (parcours du calendrier avant connexion).
 *
 * Contrairement à SlotDTO, ne contient NI le patient (identité, SSN,
 * téléphone, adresse), NI le motif de consultation (donnée médicale).
 * Seules les informations nécessaires pour afficher un calendrier
 * (créneau pris ou libre) sont exposées.
 */
@Data
@Builder
public class PublicSlotDTO {

    private Long id;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private LocalTime endTime;
    private SlotStatus status;
    private DoctorDTO doctor;

    public static PublicSlotDTO fromEntity(Slot slot) {
        if (slot == null) return null;
        return PublicSlotDTO.builder()
                .id(slot.getId())
                .slotDate(slot.getSlotDate())
                .slotTime(slot.getSlotTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .doctor(DoctorDTO.fromEntity(slot.getDoctor()))
                .build();
    }
}
