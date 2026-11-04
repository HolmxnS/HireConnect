package com.project.HireConnect.controller;

import com.project.HireConnect.enums.VacanteModalidad;
import com.project.HireConnect.enums.VacanteTipocontrato;
import com.project.HireConnect.enums.VacanteExperienciaRequerida;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.service.*;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Requires the local Python API. Run with -Dml.integration=true. */
@EnabledIfSystemProperty(named = "ml.integration", matches = "true")
class PrediccionLiveIntegrationTest {
    @Test
    void perfilAutenticadoLlegaAlModeloPythonYRegresaComoJson() throws Exception {
        var people = mock(PostulanteService.class);
        var vacancies = mock(VacanteService.class);
        var person = new Postulante();
        person.setHabilidades(Set.of("Java"));
        var vacancy = new Vacante();
        vacancy.setHabilidadesRequeridas("Java, Python");
        vacancy.setModalidad(VacanteModalidad.Remoto);
        vacancy.setTipoContrato(VacanteTipocontrato.INDEFINIDO);
        vacancy.setExperienciaRequerida(VacanteExperienciaRequerida.DE_1_A_2_ANIOS);
        when(people.buscarPorEmail("integracion@example.invalid")).thenReturn(Optional.of(person));
        when(vacancies.buscarPorId("vacante-prueba")).thenReturn(Optional.of(vacancy));
        var controller = new PrediccionVacanteController(vacancies, people,
                new PredictionService("http://127.0.0.1:5050/predict"), new PrediccionPerfilService());
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/postulante/vacante/vacante-prueba/prediccion")
                        .principal(new UsernamePasswordAuthenticationToken("integracion@example.invalid", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("logistic_regression"))
                .andExpect(jsonPath("$.schemaVersion").value(2))
                .andExpect(jsonPath("$.probabilidad").isNumber())
                .andExpect(jsonPath("$.coberturaLimitada").value(true));
        mvc.perform(get("/postulante/vacante/vacante-prueba/prediccion"))
                .andExpect(status().isUnauthorized());
    }
}
