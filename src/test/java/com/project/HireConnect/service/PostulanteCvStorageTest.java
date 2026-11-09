package com.project.HireConnect.service;

import com.project.HireConnect.repository.PostulanteRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostulanteCvStorageTest {
    @Test
    void resuelveElArchivoPersistenteSinAceptarReferenciasInvalidas() {
        var storage = mock(GridFsTemplate.class);
        var service = new PostulanteService(mock(PostulanteRepository.class), mock(PasswordEncoder.class), storage);
        assertNull(service.buscarCvAlmacenado("uploads/cv/existente.pdf"));
        assertNull(service.buscarCvAlmacenado("gridfs:no-valido"));
        verifyNoInteractions(storage);
        var file = new GridFSFile(new org.bson.BsonObjectId(new org.bson.types.ObjectId()), "cv.pdf", 100L, 261120, new java.util.Date(), new org.bson.Document());
        var resource = mock(GridFsResource.class);
        when(storage.findOne(any())).thenReturn(file);
        when(storage.getResource(file)).thenReturn(resource);
        assertSame(resource, service.buscarCvAlmacenado("gridfs:000000000000000000000001"));
        when(storage.findOne(any())).thenReturn(null);
        assertNull(service.buscarCvAlmacenado("gridfs:000000000000000000000002"));
    }
}
