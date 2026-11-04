package com.project.HireConnect.model;

import com.project.HireConnect.enums.PostulacionEstado;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Data
@Document(collection = "postulaciones")
public class Postulacion {

    @Id
    private String id;

    @DBRef
    @Field("postulanteId")
    private Postulante postulante;

    @DBRef
    @Field("vacanteId")
    private Vacante vacante;

    @Field("fechaPostulacion")
    private LocalDate fechaPostulacion = LocalDate.now();

    private PostulacionEstado estado = PostulacionEstado.PENDIENTE;

    private String mensaje;

    // Datos disponibles antes de conocer el resultado, sin identificadores personales.
    private com.project.HireConnect.dto.PrediccionRequestDTO prediccionPerfil;
}