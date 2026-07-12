package com.rdvmedic.rdv_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.model.SlotStatus;
import com.rdvmedic.rdv_api.security.CustomUserDetailsService;
import com.rdvmedic.rdv_api.security.JwtTokenProvider;
import com.rdvmedic.rdv_api.security.UserPrincipal;
import com.rdvmedic.rdv_api.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SlotController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private SlotService slotService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Slot buildSlot(Long id, Long doctorId, Long patientId) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setUsername("dr.test");
        doctor.setEmail("dr@hopital.fr");
        doctor.setPassword("secret");

        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setUsername("patient.test");
        patient.setEmail("patient@mail.fr");
        patient.setPassword("secret");

        return Slot.builder()
                .id(id)
                .slotDate(LocalDate.of(2026, 4, 15))
                .slotTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .slotReason("Bilan annuel")
                .status(SlotStatus.AVAILABLE)
                .doctor(doctor)
                .patient(patient)
                .build();
    }

    // ─── GET /slots ────────────────────────────────────────────────────────────

    @Test
    void getSlots_returnsListWith200() throws Exception {
        when(slotService.getSlots()).thenReturn(List.of(buildSlot(1L, 1L, 1L), buildSlot(2L, 1L, 2L)));

        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getSlots_returnsEmptyList() throws Exception {
        when(slotService.getSlots()).thenReturn(List.of());

        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // GET /slots/{idDoctor}/{idPatient} a été supprimé (voir AUDIT-SECURITE.md, faille 1) :
    // le test de non-régression correspondant (404 attendu) est dans SecurityRegressionTest,
    // pas ici — cette classe teste le routing, pas la sécurité.

    // ─── POST /slot/{idDoctor}/{idPatient} ─────────────────────────────────────

    @Test
    void addSlot_returns201() throws Exception {
        Slot input = Slot.builder()
                .slotDate(LocalDate.of(2026, 4, 15)).slotTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30)).slotReason("Bilan annuel")
                .status(SlotStatus.AVAILABLE).build();
        Slot saved = buildSlot(10L, 1L, 2L);
        when(slotService.addSlot(eq(1L), eq(2L), any(Slot.class))).thenReturn(saved);

        // Alimente le SecurityContextHolder manuellement (même mécanisme que JwtAuthenticationFilter)
        UserPrincipal mockPatient = new UserPrincipal(2L, "patient.test", "p@mail.fr", "secret",
                true, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockPatient, null, mockPatient.getAuthorities()));

        try {
            mockMvc.perform(post("/slot/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.slotReason").value("Bilan annuel"));
        } finally {
            SecurityContextHolder.clearContext(); // Nettoie pour ne pas polluer les autres tests
        }
    }

    // ─── DELETE /slot/{id} ─────────────────────────────────────────────────────

    @Test
    void deleteSlot_returns204() throws Exception {
        // deleteSlot vérifie l'ownership : il faut un médecin authentifié dans le
        // SecurityContext (même mécanisme que JwtAuthenticationFilter en réel).
        UserPrincipal mockDoctor = new UserPrincipal(1L, "doc.test", "doc@hopital.fr", "secret",
                true, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockDoctor, null, mockDoctor.getAuthorities()));

        try {
            doNothing().when(slotService).deleteSlot(5L, 1L);

            mockMvc.perform(delete("/slot/5"))
                    .andExpect(status().isNoContent());

            verify(slotService, times(1)).deleteSlot(5L, 1L);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
