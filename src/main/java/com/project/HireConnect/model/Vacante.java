package com.project.HireConnect.model;

import com.project.HireConnect.enums.VacanteEstado;
import com.project.HireConnect.enums.VacanteExperienciaRequerida;
import com.project.HireConnect.enums.VacanteModalidad;
import com.project.HireConnect.enums.VacanteTipocontrato;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.Transient;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Document(collection = "vacantes")
public class Vacante {

    @Id
    private String id;

    @Size(max = 100, message = "El título no puede superar los 100 caracteres")
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String descripcion;

    @Min(value = 0, message = "El salario debe ser mayor o igual a 0")
    private Double salario;

    @NotNull(message = "Debe seleccionar un tipo de contrato")
    @Field("tipoContrato")
    private String tipoContrato;

    @NotNull(message = "Debe seleccionar una modalidad de trabajo")
    @Field("modalidad")
    private String modalidad;

    private VacanteEstado estado = VacanteEstado.PENDIENTE;

    @Field("experienciaRequerida")
    private String experienciaRequerida;

    public VacanteTipocontrato getTipoContrato() {
        return tipoContrato != null ? VacanteTipocontrato.fromString(tipoContrato) : null;
    }

    public VacanteModalidad getModalidad() {
        return modalidad != null ? VacanteModalidad.fromString(modalidad) : null;
    }

    public VacanteExperienciaRequerida getExperienciaRequerida() {
        return experienciaRequerida != null ? VacanteExperienciaRequerida.fromString(experienciaRequerida) : null;
    }

    // Setters que aceptan enum y convierten a String
    public void setTipoContrato(VacanteTipocontrato tipoContrato) {
        this.tipoContrato = tipoContrato != null ? tipoContrato.getDescripcion() : null;
    }

    public void setModalidad(VacanteModalidad modalidad) {
        this.modalidad = modalidad != null ? modalidad.getDescripcion() : null;
    }

    public void setExperienciaRequerida(VacanteExperienciaRequerida experienciaRequerida) {
        this.experienciaRequerida = experienciaRequerida != null ? experienciaRequerida.getDescripcion() : null;
    }

    private String habilidadesRequeridas;

    @Field("fechaPublicacion")
    private LocalDate fechaPublicacion = LocalDate.now();

    @Field("fecha_inicio")
    private LocalDate fechaInicio;

    @Field("fecha_fin")
    private LocalDate fechaFin;

    private EmpresaResumen empresa;

    public Vacante() {
        this.fechaPublicacion = LocalDate.now();
    }

    @DBRef(lazy = true)
    private List<Postulacion> postulaciones = new ArrayList<>();

    @Transient
    private long totalPostulaciones;
}