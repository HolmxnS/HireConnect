package com.project.HireConnect.service;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.model.Experiencia_postulante;
import com.project.HireConnect.enums.VacanteModalidad;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrediccionPerfilServiceTest {
    @Test
    void usaPerfilDelCandidatoSinTransmitirDatosPersonales() {
        var persona = new Postulante();
        persona.setEmail("privado@example.invalid");
        persona.setHabilidades(Set.of("Java", "SQL"));
        var experiencia = new Experiencia_postulante();
        experiencia.setEmpresa("Empresa privada");
        experiencia.setFechaInicio(LocalDate.of(2020, 1, 1));
        persona.setExperiencia(List.of(experiencia));
        var vacante = new Vacante();
        vacante.setHabilidadesRequeridas("Java, Python");
        vacante.setModalidad(VacanteModalidad.Remoto);
        var dto = new PrediccionPerfilService().preparar(persona, vacante);
        assertEquals(Set.of("Java", "SQL"), Set.copyOf(dto.getHabilidades()));
        assertEquals("Java, Python", dto.getHabilidadesRequeridas());
        assertEquals("Remoto", dto.getModalidad());
        assertEquals(java.util.Map.of("fechaInicio", "2020-01-01"), dto.getExperiencia().getFirst());
        assertEquals(2, dto.getSchemaVersion());
        assertFalse(dto.toString().contains("privado"));
        assertFalse(dto.toString().contains("Empresa privada"));
        var otro = new Postulante();
        otro.setHabilidades(Set.of("Python"));
        assertNotEquals(dto.getHabilidades(), new PrediccionPerfilService().preparar(otro, vacante).getHabilidades());
    }
}
