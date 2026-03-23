package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.model.SlotStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class SlotDTO {

    private Long id;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private LocalTime endTime;
    private String slotReason;
    private SlotStatus status;
    private DoctorDTO doctor;
    private PatientDTO patient;

    public static SlotDTO fromEntity(Slot slot) {
        if (slot == null) return null;
        return SlotDTO.builder()
                .id(slot.getId())
                .slotDate(slot.getSlotDate())
                .slotTime(slot.getSlotTime())
                .endTime(slot.getEndTime())
                .slotReason(slot.getSlotReason())
                .status(slot.getStatus())
                .doctor(DoctorDTO.fromEntity(slot.getDoctor()))
                .patient(PatientDTO.fromEntity(slot.getPatient()))
                .build();
    }
}
