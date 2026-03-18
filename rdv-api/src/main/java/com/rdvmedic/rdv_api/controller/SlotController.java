package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.service.SlotService;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

//permet d’indiquer à Spring que cette classe est un bean
//et indique à Spring d’insérer le retour de la méthode
// au format JSON dans le corps de la réponse HTTP
@RestController

public class SlotController {


    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private SlotService slotService;

    /**
     * Read - Get all employees
     * @return - An Iterable object of Employee full filled
     */
    @GetMapping("/slots")
    //les requêtes HTTP de type GET à l’URL /slots exécuteront le code de cette
    // méthode. Et ce code est tout simple : il s’agit d’appeler la méthode getSlots()
    // du service, ce dernier appellera la méthode findAll() du repository, et nous obtiendrons
    // ainsi tous les crénaux enregistrés en base de données
    public Iterable<Slot> getSlots() {
        return slotService.getSlots();
    }


    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/slots/{idDoctor}/{idPatient}")
    //List<Slot> : La méthode retourne une liste de créneaux (Slot) au format JSON
    // PathVariable int id Récupère la valeur de {id} dans l’URL.
    // slotService.getSlotsByDoctor(idDoctor) : Appelle la méthode du service
    // qui va chercher les créneaux liés à ce médecin.
    public List<Slot> getSlotsByDoctorIdAndPatientId(@PathVariable int idDoctor, @PathVariable int idPatient) {
        List<Slot> slots = slotService.getSlotsByDoctorIdAndPatientId(idDoctor, idPatient);

        //msg d'erreur si un patient n'est pas attribué au créneau
        if (slots.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Aucun créneau trouvé pour ce médecin et ce patient."
            );
        }

        return slots;
    }


    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/slot/{idDoctor}/{idPatient}")
    public ResponseEntity<Slot> addSlot(@PathVariable int idDoctor,@PathVariable int idPatient, @RequestBody Slot slot) {
        Slot createdSlot = slotService.addSlot(idDoctor,idPatient, slot);
        return  ResponseEntity.status(HttpStatus.CREATED).body(createdSlot);
    }
}