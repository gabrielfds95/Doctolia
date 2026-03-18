package com.rdvmedic.rdv_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.repository.DoctorRepository;

import lombok.Data;

@Data
@Service
public class DoctorService {

    //chaque méthode a pour unique objectif d’appeler une méthode de DoctorRepository

    @Autowired
    private DoctorRepository doctorRepository;

    public Optional<Doctor> getDoctor(final int id) {
        return doctorRepository.findById(id);
    }

    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors;
    }

    public Optional<Doctor> getDoctorById(int idDoctor) {
        Optional<Doctor> doctor = doctorRepository.findDoctorById(idDoctor);
        return doctor;
    }

    public void deleteDoctor(final int id) {
        doctorRepository.deleteById(id);
    }

    public Doctor saveDoctor(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

}
