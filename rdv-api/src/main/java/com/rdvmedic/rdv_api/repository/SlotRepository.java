package com.rdvmedic.rdv_api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.time.LocalTime;


import com.rdvmedic.rdv_api.model.Slot;


//@Repository est une annotation Spring pour indiquer que la classe est un bean,
// et que son rôle est de communiquer avec une source de données (en l'occurrence
// la base de données).
@Repository
//extends JpaRepository<Slot, Integer> : Hérite des méthodes CRUD de Spring Data JPA pour l’entité Slot.
//findByDoctorId(int doctorId) : Méthode personnalisée qui utilise le nom de la propriété doctor dans l’entité
// Slot pour générer automatiquement une requête SQL
public interface SlotRepository extends JpaRepository<Slot, Integer> {
    List<Slot> findByDoctorId(int doctorId);
    List<Slot> findByDoctorIdAndPatientId(int doctorId, int patientId);

// Cette méthode permet de vérifier si un créneau existe déjà pour un médecin à une date donnée
//Optional : conteneur qui peut contenir une valeur non nulle ou être vide
  //  Optional<Slot> findSlotByDoctorAndDateTime(int doctorId, LocalDate slotDate, LocalTime slotTime);

}

