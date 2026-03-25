package com.rdvmedic.rdv_api.repository;

import com.rdvmedic.rdv_api.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
}
