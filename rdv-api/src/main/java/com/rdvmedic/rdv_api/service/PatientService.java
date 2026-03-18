package com.rdvmedic.rdv_api.service;

import java.util.List;
import java.util.Optional;

import com.rdvmedic.rdv_api.model.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.repository.PatientRepository;

import lombok.Data;


@Data
@Service
public class PatientService {

    //chaque méthode a pour unique objectif d’appeler une méthode de PatientRepository
    @Autowired
    private PatientRepository patientRepository;

    public List<Patient> getPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients;
    }

    public Patient newPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public Optional<Patient> getPatient(final int id) {
        return patientRepository.findById(id);
    }

    public void deletePatient(final int id) {
        patientRepository.deleteById(id);
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }
}
