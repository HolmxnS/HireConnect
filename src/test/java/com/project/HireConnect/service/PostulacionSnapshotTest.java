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
        application.setId("persistida");
        service.guardar(application);
        assertSame(original, application.getPrediccionPerfil());
        verify(features, times(1)).preparar(person, vacancy);
        var legacy = new Postulacion();
        legacy.setId("historica");
        legacy.setPostulante(person);
        legacy.setVacante(vacancy);
        service.guardar(legacy);
        assertNull(legacy.getPrediccionPerfil());
        verifyNoMoreInteractions(features);
    }
}
