package com.rdvmedic.rdv_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.security.CustomUserDetailsService;
import com.rdvmedic.rdv_api.security.JwtTokenProvider;
import com.rdvmedic.rdv_api.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PatientController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PatientService patientService;
    // Nécessaires pour satisfaire les dépendances de JwtAuthenticationFilter (Filter scanné par @WebMvcTest)
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Patient buildPatient(Long id, String username) {
        Patient p = new Patient();
        p.setId(id);
        p.setUsername(username);
        p.setEmail(username + "@mail.com");
        p.setPassword("secret");
        p.setFirstName("Jean");
        p.setLastName("Dupont");
        p.setSsn("123456789");
        p.setPhoneNumber("0600000000");
        p.setAddress("12 rue de la Paix");
        p.setAge(35);
        return p;
    }

    // ─── GET /patients ─────────────────────────────────────────────────────────

    @Test
    void getPatients_returnsListWith200() throws Exception {
        when(patientService.getPatients()).thenReturn(List.of(
                buildPatient(1L, "jean.dupont"),
                buildPatient(2L, "marie.curie")));

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("jean.dupont"));
    }

    @Test
    void getPatients_returnsEmptyList() throws Exception {
        when(patientService.getPatients()).thenReturn(List.of());

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── POST /patient ─────────────────────────────────────────────────────────

    @Test
    void createPatient_returns201() throws Exception {
        Patient saved = buildPatient(1L, "jean.dupont");
        when(patientService.newPatient(any(Patient.class))).thenReturn(saved);

        mockMvc.perform(post("/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPatient(null, "jean.dupont"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("jean.dupont"));
    }

    // ─── DELETE /patient/{id} ──────────────────────────────────────────────────

    @Test
    void deletePatient_returns204() throws Exception {
        doNothing().when(patientService).deletePatient(1L);

        mockMvc.perform(delete("/patient/1"))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(1L);
    }
}
