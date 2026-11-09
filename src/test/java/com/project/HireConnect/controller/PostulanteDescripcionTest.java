package com.project.HireConnect.controller;

import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.service.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostulanteDescripcionTest {
    @Test
    void guardaLaDescripcionYPermiteDejarlaVaciaPeroRechazaMasDe500Caracteres() throws Exception {
        var people = mock(PostulanteService.class);
        var person = new Postulante(); person.setId("candidato"); person.setEmail("prueba@example.invalid");
        when(people.buscarPorEmail(person.getEmail())).thenReturn(Optional.of(person));
        var mvc = MockMvcBuilders.standaloneSetup(new PostulanteController(people,
            mock(PostulacionService.class), mock(VacanteService.class), mock(HistorialService.class), mock(EmpresaService.class))).build();
        var principal = new UsernamePasswordAuthenticationToken(person.getEmail(), null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PERSONA")));
        mvc.perform(post("/postulante/actualizar").principal(principal).param("descripcion", "  Desarrolladora Java.\nBusco oportunidades remotas.  "))
            .andExpect(redirectedUrl("/postulante/perfil"));
        verify(people).guardar(person);
        assertEquals("Desarrolladora Java.\nBusco oportunidades remotas.", person.getDescripcion());
        clearInvocations(people);
        mvc.perform(post("/postulante/actualizar").principal(principal).param("descripcion", "x".repeat(501)))
            .andExpect(redirectedUrl("/postulante/perfil"))
            .andExpect(flash().attribute("error", "La descripción no puede superar 500 caracteres."));
        verify(people, never()).guardar(any());
        mvc.perform(post("/postulante/actualizar").principal(principal).param("descripcion", "  "))
            .andExpect(redirectedUrl("/postulante/perfil"));
        assertNull(person.getDescripcion());
    }
}
