package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.UserProfileDTO;
import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.model.User;
import com.rdvmedic.rdv_api.repository.UserRepository;
import com.rdvmedic.rdv_api.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileDTO> getProfile() {
        UserPrincipal principal = extractCurrentUser();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userRepository.findById(principal.getId()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(toDTO(user));
    }

    @PatchMapping("/users/me")
    public ResponseEntity<UserProfileDTO> updateProfile(@RequestBody UserProfileDTO dto) {
        UserPrincipal principal = extractCurrentUser();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userRepository.findById(principal.getId()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName()  != null) user.setLastName(dto.getLastName());
        if (dto.getEmail()     != null) user.setEmail(dto.getEmail());

        if (user instanceof Patient p) {
            if (dto.getPhoneNumber() != null) p.setPhoneNumber(dto.getPhoneNumber());
            if (dto.getAddress()     != null) p.setAddress(dto.getAddress());
            if (dto.getAge()         != null) p.setAge(dto.getAge());
        }

        if (user instanceof Doctor d) {
            if (dto.getSpeciality()      != null) d.setSpeciality(dto.getSpeciality());
            if (dto.getDepartment()      != null) d.setDepartment(dto.getDepartment());
            if (dto.getExperienceYears() != null) d.setExperienceYears(dto.getExperienceYears());
        }

        return ResponseEntity.ok(toDTO(userRepository.save(user)));
    }

    private UserProfileDTO toDTO(User user) {
        UserProfileDTO.UserProfileDTOBuilder b = UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName());

        if (user instanceof Patient p) {
            b.phoneNumber(p.getPhoneNumber())
             .address(p.getAddress())
             .age(p.getAge());
        }
        if (user instanceof Doctor d) {
            b.speciality(d.getSpeciality())
             .department(d.getDepartment())
             .experienceYears(d.getExperienceYears());
        }
        return b.build();
    }

    private UserPrincipal extractCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) return null;
        return p;
    }
}
