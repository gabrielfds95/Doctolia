package com.rdvmedic.rdv_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.model.SlotStatus;
import com.rdvmedic.rdv_api.repository.DoctorRepository;
import com.rdvmedic.rdv_api.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.repository.SlotRepository;

import com.rdvmedic.rdv_api.exception.ResourceNotFoundException;
import lombok.Data;

@Data
@Service
public class SlotService {

    //chaque méthode a pour unique objectif d’appeler une méthode de SlotRepository

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;


    public Optional<Slot> getSlot(final Long id) {
        return slotRepository.findById(id);
    }

    public List<Slot> getSlots() {
        return slotRepository.findAll();
    }

    public void deleteSlot(final Long id) {
        slotRepository.deleteById(id);
    }

    public Slot addSlot(Long doctorId, Long patientId, Slot slot) {
        // On récupère le médecin en base avec son id
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable : " + doctorId));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable : " + patientId));


        // Vérifie les conflits : créneau déjà réservé ou indisponibilité qui couvre cette plage
        boolean conflict = slotRepository.findByDoctorId(doctorId).stream().anyMatch(existing -> {
            if (!existing.getSlotDate().equals(slot.getSlotDate())) return false;
            if (existing.getStatus() == SlotStatus.RESERVED) {
                return existing.getSlotTime().equals(slot.getSlotTime());
            }
            if (existing.getStatus() == SlotStatus.CANCELLED && existing.getPatient() == null) {
                // Indisponibilité : vérifie si l'heure demandée est dans la plage bloquée
                return !slot.getSlotTime().isBefore(existing.getSlotTime())
                        && slot.getSlotTime().isBefore(existing.getEndTime());
            }
            return false;
        });

        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce créneau est déjà réservé ou indisponible.");
        }

        // On rattache le médecin et le patient au créneau
        slot.setDoctor(doctor);
        slot.setPatient(patient);
        slot.setStatus(SlotStatus.RESERVED); // Toute réservation passe en RESERVED

        // On enregistre le créneau en base
        return slotRepository.save(slot);
    }

    //public List<Slot> : La méthode retourne une liste de créneaux.
    //doctorId : C’est l’ID du médecin reçu depuis le controller.
    //slotRepository.findByDoctorId(doctorId) : Utilise le repository
    // pour interroger la base de données et récupérer tous les créneaux associés à ce médecin.
    public List<Slot> getSlotsByDoctor(Long idDoctor) {
        return slotRepository.findByDoctorId(idDoctor);
    }

    public List<Slot> getSlotsByDoctorIdAndPatientId(Long idDoctor, Long idPatient) {
        return slotRepository.findByDoctorIdAndPatientId(idDoctor,idPatient);
    }

    public List<Slot> getSlotsByPatient(Long patientId) {
        return slotRepository.findByPatientId(patientId);
    }

    public Slot cancelSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        slot.setStatus(SlotStatus.CANCELLED);
        return slotRepository.save(slot);
    }

    public Slot updateSlotReason(Long slotId, String newReason) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        slot.setSlotReason(newReason);
        return slotRepository.save(slot);
    }

    public Slot completeSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable : " + slotId));
        slot.setStatus(SlotStatus.COMPLETED);
        return slotRepository.save(slot);
    }

    public Slot createUnavailability(Long doctorId, Slot slot) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable : " + doctorId));
        slot.setDoctor(doctor);
        slot.setPatient(null);
        slot.setStatus(SlotStatus.CANCELLED); // Indisponibilité : bloque le créneau sans patient
        return slotRepository.save(slot);
    }

}