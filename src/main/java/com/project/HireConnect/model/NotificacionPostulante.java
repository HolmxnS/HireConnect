package com.project.HireConnect.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Aviso para el postulante, por ejemplo cuando cambia el estado de una
 * postulacion.
 */
@Data
@Document(collection = "notificaciones_postulante")
public class NotificacionPostulante {

    @Id
    private String id;

    @Indexed
    private String postulanteId;

    private String postulacionId;

    private String vacanteTitulo;

    // Nombre del estado nuevo (EN_REVISION, ACEPTADA...)
    private String estado;

    private String mensaje;

    private LocalDateTime fecha = LocalDateTime.now();

    private boolean leida = false;
}