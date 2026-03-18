package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.model.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.service.DoctorService;
import com.rdvmedic.rdv_api.service.SlotService;

import java.util.List;
import java.util.Optional;


//permet d’indiquer à Spring que cette classe est un bean
//et indique à Spring d’insérer le retour de la méthode
// au format JSON dans le corps de la réponse HTTP
@CrossOrigin(origins = "http://localhost:4200")
@RestController

public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private SlotService slotService;

    /**
     * Read - Get all employees
     * @return - An Iterable object of Employee full filled
     */
    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/doctors")
    //les requêtes HTTP de type GET à l’URL /doctors exécuteront le code de cette
    // méthode. Et ce code est tout simple : il s’agit d’appeler la méthode getDoctors()
    // du service, ce dernier appellera la méthode findAll() du repository, et nous obtiendrons
    // ainsi tous les médecins enregistrés en base de données
    public List<Doctor> getDoctors() {
        return doctorService.getDoctors();
    }


    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/doctors/{idDoctor}/slots")
    //List<Slot> : La méthode retourne une liste de créneaux (Slot) au format JSON
    // PathVariable int id Récupère la valeur de {id} dans l’URL.
    // slotService.getSlotsByDoctor(idDoctor) : Appelle la méthode du service
    // qui va chercher les créneaux liés à ce médecin.
    public List<Slot> getSlotsByDoctor(@PathVariable int idDoctor) {
        return slotService.getSlotsByDoctor(idDoctor);
    }


    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/doctor/{idDoctor}")
    public Optional<Doctor> getDoctorById(@PathVariable int idDoctor) {
        return doctorService.getDoctorById(idDoctor);
    }


}