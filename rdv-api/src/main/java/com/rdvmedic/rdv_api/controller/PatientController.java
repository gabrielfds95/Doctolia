package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.service.PatientService;

import java.util.List;

//permet d’indiquer à Spring que cette classe est un bean
//et indique à Spring d’insérer le retour de la méthode
// au format JSON dans le corps de la réponse HTTP
@RestController

public class PatientController {

    @Autowired
    private PatientService patientService;

    /**
     * Read - Get all employees
     * @return - An Iterable object of Employee full filled
     */
    @GetMapping("/patients")
    //les requêtes HTTP de type GET à l’URL /patients exécuteront le code de cette
    // méthode. Et ce code est tout simple : il s’agit d’appeler la méthode getPatients()
    // du service, ce dernier appellera la méthode findAll() du repository, et nous obtiendrons
    // ainsi tous les patients enregistrés en base de données
    public List<Patient> getPatients() {
        return patientService.getPatients();
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/patient")
    public ResponseEntity<Patient> newPatient(@RequestBody Patient patient) {
        Patient addPatient = patientService.newPatient(patient);
        return  ResponseEntity.status(HttpStatus.CREATED).body(addPatient);
    }
}