package com.project.HireConnect.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Data
@Document(collection = "historial_busquedas")
public class HistorialBusqueda {

    @Id
    private String id;

    private String email;
    private String texto;
    private Date fecha = new Date();
}
