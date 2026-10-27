package com.gym;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Recorre el flujo principal contra un PostgreSQL real (Testcontainers) con todas las migraciones aplicadas.
 * Necesita Docker; si no está corriendo, la prueba se omite en lugar de fallar.
 */
@SpringBootTest(properties = {
        "app.mail.enabled=false",
        "app.receipts.auto-email=false",
        "app.reminders.enabled=false",
        "app.storage.path=${java.io.tmpdir}/gym-test-uploads",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class GymFlowTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    String admin;

    @BeforeEach
    void loginAdmin() throws Exception {
        admin = login("admin@gym.local", "admin123");
    }

    @Test
    void socioSinPagoNoEntraHastaQuePaga() throws Exception {
        JsonNode socio = createMember();
        long id = socio.get("id").asLong();

        // Sin pagos: la entrada se rechaza y el error dice de quién es.
        call(post("/api/attendance/check-in"), admin, Map.of("userId", id))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("MEMBRESIA_VENCIDA"))
                .andExpect(jsonPath("$.fields.userId").value(String.valueOf(id)));

        // Se cobra un mes: el periodo empieza en la fecha de inscripción.
        JsonNode pago = body(call(post("/api/payments"), admin, Map.of(
                "userId", id, "planId", 3, "quantity", 1, "amount", 450, "method", "EFECTIVO"))
                .andExpect(status().isCreated()));
        assertThat(pago.get("coversFrom").asText()).isEqualTo(LocalDate.now().toString());

        // Con la membresía vigente, el QR registra la entrada; un segundo escaneo el mismo día no la duplica.
        String code = body(call(get("/api/users/" + id + "/qr"), admin, null)).get("code").asText();
        call(post("/api/attendance/scan"), admin, Map.of("code", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.action").value("ENTRADA"))
                .andExpect(jsonPath("$.membership.state").value("VIGENTE"));
        call(post("/api/attendance/scan"), admin, Map.of("code", code))
                .andExpect(jsonPath("$.action").value("YA_REGISTRADA"));

        // Recibo en PDF: lo baja quien cobra y también el socio.
        String socioToken = login(socio.get("email").asText(), "secreto1");
        for (String token : new String[]{admin, socioToken}) {
            byte[] pdf = call(get("/api/payments/" + pago.get("id").asLong() + "/receipt"), token, null)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        }
    }

    @Test
    void elSocioSoloVeYTocaLoSuyo() throws Exception {
        JsonNode socio = createMember();
        JsonNode otro = createMember();
        String token = login(socio.get("email").asText(), "secreto1");

        call(post("/api/payments"), token, Map.of("userId", socio.get("id").asLong(), "planId", 3, "quantity", 1, "method", "EFECTIVO"))
                .andExpect(status().isForbidden());
        call(get("/api/users"), token, null).andExpect(status().isForbidden());
        call(get("/api/profiles/" + otro.get("id").asLong() + "/measurements"), token, null).andExpect(status().isForbidden());
        call(put("/api/profiles/" + otro.get("id").asLong()), token, Map.of("phone", "1")).andExpect(status().isForbidden());

        // Su propio perfil sí lo edita, y un socio no puede forzar su propia entrada.
        call(put("/api/profiles/" + socio.get("id").asLong()), token, Map.of("emergencyName", "Mamá", "emergencyPhone", "555"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emergencyName").value("Mamá"));
        call(post("/api/attendance/check-in"), token, Map.of("force", true))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void lasMedidasCalculanElImcConLaUltimaEstatura() throws Exception {
        long id = createMember().get("id").asLong();
        call(post("/api/profiles/" + id + "/measurements"), admin, Map.of("measuredOn", "2026-01-10", "weightKg", 80, "heightCm", 175))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bmi").value(26.1));
        call(post("/api/profiles/" + id + "/measurements"), admin, Map.of("measuredOn", "2026-02-10", "weightKg", 77))
                .andExpect(jsonPath("$.bmi").value(25.1));
        call(post("/api/profiles/" + id + "/measurements"), admin, Map.of("measuredOn", "2026-02-11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laRutinaSeArmaPorDia() throws Exception {
        long socio = createMember().get("id").asLong();
        // Lunes: press militar y sentadilla con descripción; miércoles: solo press inclinado.
        JsonNode rutina = body(call(post("/api/routines"), admin, Map.of(
                "name", "Genérica para nuevos", "discipline", "GYM", "memberIds", java.util.List.of(socio),
                "days", java.util.List.of(
                        Map.of("dayOfWeek", 3, "exercises", java.util.List.of(Map.of("exerciseId", 2, "sets", 4, "reps", "10"))),
                        Map.of("dayOfWeek", 1, "description", "Calentar 10 min", "exercises", java.util.List.of(
                                Map.of("exerciseId", 18, "sets", 3), Map.of("exerciseId", 34, "reps", "12")))))));
        assertThat(rutina.get("description").isNull()).isTrue();
        JsonNode days = rutina.get("days");
        assertThat(days).hasSize(2);
        assertThat(days.get(0).get("dayOfWeek").asInt()).isEqualTo(1);
        assertThat(days.get(0).get("description").asText()).isEqualTo("Calentar 10 min");
        assertThat(days.get(0).get("exercises").get(1).get("name").asText()).isEqualTo("Sentadilla");
        assertThat(days.get(1).get("exercises").get(0).get("name").asText()).isEqualTo("Press de banca inclinado");

        // Un día repetido se rechaza.
        call(post("/api/routines"), admin, Map.of("name", "Doble", "discipline", "GYM",
                "days", java.util.List.of(Map.of("dayOfWeek", 2), Map.of("dayOfWeek", 2))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recuperarContrasenaNoRevelaSiElCorreoExiste() throws Exception {
        call(post("/api/auth/forgot-password"), null, Map.of("email", "nadie@ejemplo.com")).andExpect(status().isNoContent());
        call(post("/api/auth/reset-password"), null, Map.of("token", "inventado", "password", "nueva123"))
                .andExpect(status().is4xxClientError());
    }

    private JsonNode createMember() throws Exception {
        String email = "socio-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.local";
        return body(call(post("/api/users"), admin, Map.of(
                "email", email, "password", "secreto1", "fullName", "Socio Prueba",
                "roles", new String[]{"SOCIO"}, "disciplines", new String[]{"GYM"}))
                .andExpect(status().isCreated()));
    }

    private String login(String email, String password) throws Exception {
        return body(call(post("/api/auth/login"), null, Map.of("email", email, "password", password))
                .andExpect(status().isOk())).get("token").asText();
    }

    private ResultActions call(MockHttpServletRequestBuilder req, String token, Object payload) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (payload != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(payload));
        }
        return mvc.perform(req);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }
}
