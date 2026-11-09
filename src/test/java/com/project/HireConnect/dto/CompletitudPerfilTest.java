package com.project.HireConnect.dto;

import com.project.HireConnect.model.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompletitudPerfilTest {
    @Test
    void perfilVacioNoGanaPuntosPorColeccionesInicializadasNiEspacios() {
        var p = new Postulante();
        p.setNombre("  ");
        p.setFoto("\t");
        p.setHabilidades(new HashSet<>(Arrays.asList(null, " ")));
        p.setEducacion(Arrays.asList(null, new Educacion_postulante()));
        p.setExperiencia(Arrays.asList(null, new Experiencia_postulante()));
        assertEquals(0, CompletitudPerfil.calcular(p).getPorcentaje());
        p.setEducacion(null);
        p.setExperiencia(null);
        p.setHabilidades(null);
        assertEquals(0, CompletitudPerfil.calcular(p).getPorcentaje());
    }

    @Test
    void datosGuardadosSumanDiezPuntosPorApartadoSinUsarLaBanderaDelRegistro() {
        var p = new Postulante();
        p.setPerfilCompleto(true);
        assertEquals(0, CompletitudPerfil.calcular(p).getPorcentaje());
        p.setNombre("Ana");
        assertEquals(0, CompletitudPerfil.calcular(p).getPorcentaje());
        p.setApellido("Pérez");
        p.setEmail("ana@example.invalid");
        p.setTelefono("3001234567");
        assertEquals(30, CompletitudPerfil.calcular(p).getPorcentaje());
        p.setTitulacion("Desarrolladora");
        p.setNacionalidad("Colombiana");
        p.setFoto("/foto.jpg");
        p.setCv("uploads/cv/ana.pdf");
        p.setHabilidades(Set.of("Java", "SQL"));
        p.setEducacion(List.of(Educacion_postulante.builder().institucion("Universidad")
            .titulo("Ingeniería").fechaInicio(LocalDate.of(2020, 1, 1)).build()));
        p.setExperiencia(List.of(Experiencia_postulante.builder().empresa("Empresa")
            .cargo("Desarrolladora").fechaInicio(LocalDate.of(2024, 1, 1)).build()));
        var resultado = CompletitudPerfil.calcular(p);
        assertEquals(100, resultado.getPorcentaje());
        assertEquals(10, resultado.getCompletados());
        p.setFoto(null);
        assertEquals(90, CompletitudPerfil.calcular(p).getPorcentaje());
    }
}
