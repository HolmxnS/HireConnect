package com.project.HireConnect.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.model.*;
import com.project.HireConnect.service.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class PostulanteCvAccessTest {
    private static final String DESTINO = "000000000000000000000001";
    private PostulanteController controller;
    private PostulanteService personas;
    private EmpresaService empresas;
    private VacanteService vacantes;
    private PostulacionService candidaturas;

    @BeforeEach
    void preparar() {
        personas = mock(PostulanteService.class); empresas = mock(EmpresaService.class);
        vacantes = mock(VacanteService.class); candidaturas = mock(PostulacionService.class);
        controller = new PostulanteController(personas, candidaturas, vacantes, mock(HistorialService.class), empresas);
        var candidato = new Postulante(); candidato.setId(DESTINO);
        when(personas.buscarPorId(DESTINO)).thenReturn(Optional.of(candidato));
    }

    private UsernamePasswordAuthenticationToken auth(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority(rol)));
    }

    @Test
    void cuentaAjenaYAnonimoNoAcceden() {
        assertEquals(401, controller.descargarCV(DESTINO, null).getStatusCode().value());
        assertEquals(403, controller.descargarCV(DESTINO, auth("ajeno@example.invalid", "ROLE_PERSONA")).getStatusCode().value());
        verify(personas, never()).buscarPorId(DESTINO);
    }

    @Test
    void empresaSinCandidaturaNoPuedeSondearElCv() {
        var empresa = new Empresa(); empresa.setId("000000000000000000000002"); empresa.setEstado(EmpresaEstado.APROBADA);
        when(empresas.buscarPorEmailEmpresa("empresa@example.invalid")).thenReturn(Optional.of(empresa));
        when(vacantes.buscarPorEmpresa(empresa)).thenReturn(List.of());
        assertEquals(403, controller.descargarCV(DESTINO, auth("empresa@example.invalid", "ROLE_EMPRESA")).getStatusCode().value());
        verify(personas, never()).buscarPorId(DESTINO);
    }

    @Test
    void candidaturaPropiaHabilitaConsultaYEmpresaSuspendidaNo() {
        var empresa = new Empresa(); empresa.setId("000000000000000000000002"); empresa.setEstado(EmpresaEstado.APROBADA);
        var oferta = new Vacante(); oferta.setId("000000000000000000000003");
        when(empresas.buscarPorEmailEmpresa("empresa@example.invalid")).thenReturn(Optional.of(empresa));
        when(vacantes.buscarPorEmpresa(empresa)).thenReturn(List.of(oferta));
        when(candidaturas.puedeConsultarCv(DESTINO, List.of(oferta))).thenReturn(true);
        assertEquals(404, controller.descargarCV(DESTINO, auth("empresa@example.invalid", "ROLE_EMPRESA")).getStatusCode().value());
        verify(personas).buscarPorId(DESTINO);
        empresa.setEstado(EmpresaEstado.SUSPENDIDA);
        assertEquals(403, controller.descargarCV(DESTINO, auth("empresa@example.invalid", "ROLE_EMPRESA")).getStatusCode().value());
    }

    @Test
    void propietarioYAdminMantienenAcceso() {
        var propietario = new Postulante(); propietario.setId(DESTINO);
        when(personas.buscarPorEmail("propietario@example.invalid")).thenReturn(Optional.of(propietario));
        assertEquals(404, controller.descargarCV(DESTINO, auth("propietario@example.invalid", "ROLE_PERSONA")).getStatusCode().value());
        assertEquals(404, controller.descargarCV(DESTINO, auth("admin@example.invalid", "ROLE_ADMIN_PLATAFORMA")).getStatusCode().value());
        verifyNoInteractions(candidaturas);
    }
}
