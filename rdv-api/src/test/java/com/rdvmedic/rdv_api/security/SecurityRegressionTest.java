package com.rdvmedic.rdv_api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdvmedic.rdv_api.model.Doctor;
import com.rdvmedic.rdv_api.model.Patient;
import com.rdvmedic.rdv_api.model.Slot;
import com.rdvmedic.rdv_api.repository.DoctorRepository;
import com.rdvmedic.rdv_api.repository.PatientRepository;
import com.rdvmedic.rdv_api.repository.SlotRepository;
import com.rdvmedic.rdv_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de NON-RÉGRESSION SÉCURITÉ — contrairement aux *ControllerTest
 * (@WebMvcTest avec excludeAutoConfiguration = SecurityAutoConfiguration et
 * services mockés), cette suite charge le contexte Spring COMPLET :
 *  - la vraie chaîne de filtres Spring Security (JwtAuthenticationFilter inclus)
 *  - les vrais services, vrais repositories, vraie base H2 (seedée par DataInitializer)
 *  - de vrais JWT obtenus via POST /login (pas de UserPrincipal fabriqué à la main)
 *
 * Objectif : verrouiller les 4 failles corrigées en Phase 1 (voir AUDIT-SECURITE.md)
 * pour qu'un futur changement qui les réintroduirait fasse échouer le build, pas
 * seulement une relecture manuelle.
 *
 * @Transactional : chaque test s'exécute dans sa propre transaction, annulée à la
 * fin (rollback automatique du framework de test Spring) — les créneaux/patients
 * créés ou modifiés par un test n'impactent jamais les tests suivants, sans avoir
 * à relancer tout le contexte Spring (coûteux) entre chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityRegressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Faille 1 — Fuite PII sur /slots (public)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getSlots_publicEndpoint_neverExposesPatientData() throws Exception {
        String body = mockMvc.perform(get("/slots"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assertions explicites sur l'ABSENCE des champs PII : si SlotDTO (avec
        // patient imbriqué) est réintroduit sur cet endpoint public, ces clés
        // réapparaissent dans le JSON et le test échoue.
        assertThat(body).doesNotContain("\"patient\"");
        assertThat(body).doesNotContain("\"ssn\"");
        assertThat(body).doesNotContain("\"phoneNumber\"");
        assertThat(body).doesNotContain("\"address\"");
        assertThat(body).doesNotContain("\"slotReason\"");

        // Garde-fou : vérifie qu'on a bien testé un cas non trivial (au moins un
        // créneau RESERVED dans les données de seed), pas un test qui passe "par
        // hasard" parce que la liste renvoyée serait vide.
        assertThat(body).contains("\"RESERVED\"");
    }

    @Test
    void getDoctorSlots_publicEndpoint_neverExposesPatientData() throws Exception {
        Doctor doctorWithSlots = doctorRepository.findAll().stream()
                .filter(d -> !slotRepository.findByDoctorId(d.getId()).isEmpty())
                .findFirst().orElseThrow();

        String body = mockMvc.perform(get("/doctors/" + doctorWithSlots.getId() + "/slots"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"patient\"");
        assertThat(body).doesNotContain("\"ssn\"");
    }

    /**
     * GET /slots/{idDoctor}/{idPatient} a été SUPPRIMÉ (pas juste fermé) : gap résiduel
     * d'ownership documenté dans AUDIT-SECURITE.md, jamais utilisé par le front
     * (rdv_medic_front / doctolia-mobile), redondant avec /patients/me/slots et
     * /doctors/me/slots (correctement scopés par ownership). Réduction de la surface
     * d'attaque plutôt qu'un contrôle d'ownership de plus à maintenir.
     *
     * Test AVEC un token valide (pas sans) : on veut isoler "la route n'existe plus"
     * (404, ce que ce test vérifie) de "bloqué par la sécurité" (403, ce que
     * anyRequest().authenticated() ferait de toute façon même si la route existait
     * encore — sans token, cette requête reste bloquée avant même d'atteindre le
     * dispatcher, donc un 404 sans token ne prouverait rien de spécifique à CE test).
     */
    @Test
    void getSlotsByDoctorAndPatient_routeNoLongerExists() throws Exception {
        Slot reserved = slotRepository.findAll().stream()
                .filter(s -> s.getPatient() != null)
                .findFirst().orElseThrow();
        String token = login(reserved.getPatient().getUsername(), "password");

        mockMvc.perform(get("/slots/" + reserved.getDoctor().getId() + "/" + reserved.getPatient().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Faille 2 — Absence d'ownership sur les slots
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void patientCancelsAnotherPatientsSlot_isForbidden() throws Exception {
        Slot slot = slotRepository.findAll().stream()
                .filter(s -> s.getPatient() != null)
                .findFirst().orElseThrow();
        Patient owner = slot.getPatient();
        Patient attacker = patientRepository.findAll().stream()
                .filter(p -> !p.getId().equals(owner.getId()))
                .findFirst().orElseThrow();

        String attackerToken = login(attacker.getUsername(), "password");

        mockMvc.perform(patch("/slots/" + slot.getId() + "/cancel")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());

        // Preuve que l'attaque n'a produit aucun effet de bord : le statut n'a pas bougé.
        Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(slot.getStatus());
    }

    @Test
    void patientCancelsOwnSlot_succeeds() throws Exception {
        Slot slot = slotRepository.findAll().stream()
                .filter(s -> s.getPatient() != null)
                .findFirst().orElseThrow();
        String ownerToken = login(slot.getPatient().getUsername(), "password");

        mockMvc.perform(patch("/slots/" + slot.getId() + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void doctorCompletesAnotherDoctorsSlot_isForbidden() throws Exception {
        Slot slot = slotRepository.findAll().stream().findFirst().orElseThrow();
        Doctor owner = slot.getDoctor();
        Doctor attacker = doctorRepository.findAll().stream()
                .filter(d -> !d.getId().equals(owner.getId()))
                .findFirst().orElseThrow();

        String attackerToken = login(attacker.getUsername(), "password");

        mockMvc.perform(put("/slots/" + slot.getId() + "/complete")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Faille 3 — Endpoints sans règle déclarée (deny-by-default)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void patientDeletingDoctor_isForbidden() throws Exception {
        Patient patient = patientRepository.findAll().stream().findFirst().orElseThrow();
        Doctor targetDoctor = doctorRepository.findAll().stream().findFirst().orElseThrow();
        String token = login(patient.getUsername(), "password");

        mockMvc.perform(delete("/doctor/" + targetDoctor.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(doctorRepository.existsById(targetDoctor.getId())).isTrue();
    }

    @Test
    void getPatients_withoutAdminRole_isForbidden() throws Exception {
        Patient patient = patientRepository.findAll().stream().findFirst().orElseThrow();
        String token = login(patient.getUsername(), "password");

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPatients_withAdminRole_isAllowed() throws Exception {
        String adminToken = login("admin", "password");

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Faille 4 — Mass assignment (Patient / Slot)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void createPatient_passwordIsHashed_andRoleIsAssigned() throws Exception {
        String adminToken = login("admin", "password");

        String body = "{\"username\":\"pat.regression\",\"email\":\"pat.regression@mail.com\"," +
                "\"password\":\"motdepasseclair\",\"firstName\":\"Reg\",\"lastName\":\"Test\"}";

        mockMvc.perform(post("/patient")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        var saved = userRepository.findByUsername("pat.regression").orElseThrow();

        // Le mot de passe stocké n'est jamais la valeur en clair envoyée...
        assertThat(saved.getPassword()).isNotEqualTo("motdepasseclair");
        // ...mais un hash BCrypt qui la vérifie correctement.
        assertThat(passwordEncoder.matches("motdepasseclair", saved.getPassword())).isTrue();
        // ROLE_PATIENT assigné automatiquement (jamais fourni par le client).
        assertThat(saved.getRoles()).anyMatch(r -> r.getName().name().equals("ROLE_PATIENT"));
    }

    @Test
    void createSlot_withForgedId_neverOverwritesExistingRow() throws Exception {
        Slot existing = slotRepository.findAll().stream()
                .filter(s -> s.getPatient() != null)
                .findFirst().orElseThrow();
        Patient attacker = patientRepository.findAll().stream()
                .filter(p -> !p.getId().equals(existing.getPatient().getId()))
                .findFirst().orElseThrow();
        String attackerToken = login(attacker.getUsername(), "password");

        long countBefore = slotRepository.count();

        // Le body tente de fournir l'id d'un créneau existant appartenant à quelqu'un d'autre,
        // avec une date différente et un statut arbitraire.
        String forgedBody = String.format(
                "{\"id\":%d,\"slotDate\":\"2026-06-01\",\"slotTime\":\"09:00:00\"," +
                        "\"endTime\":\"09:30:00\",\"slotReason\":\"attaque\",\"status\":\"COMPLETED\"}",
                existing.getId());

        MvcResult result = mockMvc.perform(post("/slot/" + existing.getDoctor().getId())
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgedBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(created.get("id").asLong()).isNotEqualTo(existing.getId().longValue());
        assertThat(created.get("status").asText()).isEqualTo("RESERVED"); // jamais COMPLETED : fixé serveur

        // Le créneau existant n'a pas été modifié (pas d'UPDATE déguisé en INSERT).
        Slot reloaded = slotRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getSlotDate()).isEqualTo(existing.getSlotDate());
        assertThat(reloaded.getStatus()).isEqualTo(existing.getStatus());
        assertThat(reloaded.getPatient().getId()).isEqualTo(existing.getPatient().getId());

        // Un nouveau créneau a bien été inséré (pas de merge sur l'id fourni par le client).
        assertThat(slotRepository.count()).isEqualTo(countBefore + 1);
    }
}
