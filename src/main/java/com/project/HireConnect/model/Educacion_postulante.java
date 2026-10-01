package com.project.HireConnect.model;

import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.Past;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Educacion_postulante {

    private String institucion;
    private String titulo;

    @Past(message = "Debe ingresar una fecha válida en el pasado")
    @Field("fecha_inicio")
    private LocalDate fechaInicio;

    @Past(message = "Debe ingresar una fecha válida en el pasado")
    @Field("fecha_fin")
    private LocalDate fechaFin;
}