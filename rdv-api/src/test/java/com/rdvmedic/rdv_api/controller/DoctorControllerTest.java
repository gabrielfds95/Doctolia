package com.rdvmedic.rdv_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.model.SlotStatus;
import com.rdvmedic.rdv_api.security.CustomUserDetailsService;
import com.rdvmedic.rdv_api.security.JwtTokenProvider;
import com.rdvmedic.rdv_api.service.DoctorService;
import com.rdvmedic.rdv_api.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DoctorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private DoctorService doctorService;
    @MockBean private SlotService slotService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Doctor buildDoctor(Long id, String username, String speciality) {
        Doctor d = new Doctor();
        d.setId(id);
        d.setUsername(username);
        d.setEmail(username + "@hopital.fr");
        d.setPassword("secret");
        d.setFirstName("Pierre");
        d.setLastName("Martin");
        d.setSpeciality(speciality);
        d.setLicenseNumber("LIC-" + id);
        d.setDepartment("Cardiologie");
        d.setExperienceYears(10);
        return d;
    }

    private Slot buildSlot(Long id, Doctor doctor) {
        return Slot.builder()
                .id(id)
                .slotDate(LocalDate.of(2026, 4, 1))
                .slotTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .slotReason("Consultation")
                .status(SlotStatus.AVAILABLE)
                .doctor(doctor)
                .build();
    }

    // ─── GET /doctors ──────────────────────────────────────────────────────────

    @Test
    void getDoctors_returnsListWith200() throws Exception {
        when(doctorService.getDoctors()).thenReturn(List.of(
                buildDoctor(1L, "pierre.martin", "Cardiologie"),
                buildDoctor(2L, "sophie.leroy", "Dermatologie")));

        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].speciality").value("Cardiologie"));
    }

    @Test
    void getDoctors_returnsEmptyList() throws Exception {
        when(doctorService.getDoctors()).thenReturn(List.of());

        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── GET /doctor/{id} ──────────────────────────────────────────────────────

    @Test
    void getDoctorById_returnsDoctor() throws Exception {
        Doctor d = buildDoctor(1L, "pierre.martin", "Cardiologie");
        when(doctorService.getDoctorById(1L)).thenReturn(Optional.of(d));

        mockMvc.perform(get("/doctor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("pierre.martin"));
    }

    @Test
    void getDoctorById_returns404WhenNotFound() throws Exception {
        when(doctorService.getDoctorById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/doctor/99"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /doctors/{id}/slots ───────────────────────────────────────────────

    @Test
    void getSlotsByDoctor_returnsSlotList() throws Exception {
        Doctor d = buildDoctor(1L, "pierre.martin", "Cardiologie");
        when(slotService.getSlotsByDoctor(1L)).thenReturn(List.of(buildSlot(10L, d), buildSlot(11L, d)));

        mockMvc.perform(get("/doctors/1/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getSlotsByDoctor_returnsEmptyList() throws Exception {
        when(slotService.getSlotsByDoctor(1L)).thenReturn(List.of());

        mockMvc.perform(get("/doctors/1/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── DELETE /doctor/{id} ───────────────────────────────────────────────────

    @Test
    void deleteDoctor_returns204() throws Exception {
        doNothing().when(doctorService).deleteDoctor(1L);

        mockMvc.perform(delete("/doctor/1"))
                .andExpect(status().isNoContent());

        verify(doctorService, times(1)).deleteDoctor(1L);
    }
}
