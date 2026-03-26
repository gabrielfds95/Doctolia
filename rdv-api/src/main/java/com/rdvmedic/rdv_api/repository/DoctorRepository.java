package com.rdvmedic.rdv_api.repository;

import com.rdvmedic.rdv_api.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUsername(String username);
    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByLicenseNumber(String licenseNumber);
    java.util.List<Doctor> findByEnabledFalse();
    java.util.List<Doctor> findByEnabledTrue();
}
