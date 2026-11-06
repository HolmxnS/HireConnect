package com.project.HireConnect.service;

import com.project.HireConnect.dto.PrediccionRequestDTO;
import com.project.HireConnect.model.Postulacion;
import com.project.HireConnect.model.Postulante;
import com.project.HireConnect.model.Vacante;
import com.project.HireConnect.repository.PostulacionRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostulacionSnapshotTest {
    @Test
    void capturaAlCrearSinReescribirElPasado() {
        var repository = mock(PostulacionRepository.class);
        var features = mock(PrediccionPerfilService.class);
        var service = new PostulacionService(repository, mock(NotificacionPostulanteService.class), features);
        var person = new Postulante();
        var vacancy = new Vacante();
        var application = new Postulacion();
        application.setPostulante(person);
        application.setVacante(vacancy);
        var original = new PrediccionRequestDTO();
        when(features.preparar(person, vacancy)).thenReturn(original);
        service.guardar(application);
        assertSame(original, application.getPrediccionPerfil());
        assertNotNull(application.getPrediccionCapturadaEn());
        assertEquals("FLUJO_APLICACION", application.getPrediccionOrigen());
        var capturedAt = application.getPrediccionCapturadaEn();
        application.setId("persistida");
        service.guardar(application);
        assertSame(original, application.getPrediccionPerfil());
        assertEquals(capturedAt, application.getPrediccionCapturadaEn());
        verify(features, times(1)).preparar(person, vacancy);
        var legacy = new Postulacion();
        legacy.setId("historica");
        legacy.setPostulante(person);
        legacy.setVacante(vacancy);
        service.guardar(legacy);
        assertNull(legacy.getPrediccionPerfil());
        assertNull(legacy.getPrediccionCapturadaEn());
        assertNull(legacy.getPrediccionOrigen());
        verifyNoMoreInteractions(features);
    }

    @Test
    void registraDecisionesNuevasSinRecrearFechasHistoricas() {
        var repository = mock(PostulacionRepository.class);
        var notification = mock(NotificacionPostulanteService.class);
        var service = new PostulacionService(repository, notification, mock(PrediccionPerfilService.class));
        var application = new Postulacion();
        application.setId("aplicacion");
        application.setEstado(com.project.HireConnect.enums.PostulacionEstado.EN_REVISION);
        when(repository.findById("aplicacion")).thenReturn(java.util.Optional.of(application));
        service.actualizarEstado("aplicacion", "ACEPTADA");
        var firstDecision = application.getResultadoActualizadoEn();
        assertNotNull(firstDecision);
        service.actualizarEstado("aplicacion", "ACEPTADA");
        assertEquals(firstDecision, application.getResultadoActualizadoEn());
        service.actualizarEstado("aplicacion", "EN_REVISION");
        assertNull(application.getResultadoActualizadoEn());
        application.setEstado(com.project.HireConnect.enums.PostulacionEstado.ACEPTADA);
        service.actualizarEstado("aplicacion", "ACEPTADA");
        assertNull(application.getResultadoActualizadoEn(), "Una repeticion no fecha retrospectivamente un resultado antiguo");
        assertNull(application.getPrediccionPerfil());
        assertNull(application.getPrediccionOrigen());
        verify(notification, times(2)).notificarCambioEstado(eq(application), any());
    }
}
