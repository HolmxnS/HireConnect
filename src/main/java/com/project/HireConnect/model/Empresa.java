package com.project.HireConnect.model;

import com.project.HireConnect.dto.Paso1;
import com.project.HireConnect.dto.Paso2;
import com.project.HireConnect.enums.EmpresaEstado;
import com.project.HireConnect.enums.Rol;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.DBRef;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "empresas")
public class Empresa {

    @Id
    private String id;

    @NotBlank(message = "El nombre legal es obligatorio", groups = Paso1.class)
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres", groups = Paso1.class)
    private String nombreLegal;

    @NotBlank(message = "El RUT/NIT es obligatorio", groups = Paso1.class)
    @Pattern(regexp = "\\d{7,12}-?\\d?", message = "Debe ser un RUT/NIT válido", groups = Paso1.class)
    @Indexed(unique = true)
    private String rut;

    @NotBlank(message = "La contraseña es obligatoria", groups = Paso2.class)
    @Size(min = 6, message = "La contraseña debe tener mínimo 6 caracteres", groups = Paso2.class)
    private String password;

    @Transient
    private String confirmPassword;

    @NotBlank(message = "El email es obligatorio", groups = Paso2.class)
    @Email(message = "Debe ingresar un correo válido", groups = Paso2.class)
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio", groups = Paso2.class)
    @Pattern(regexp = "\\d{7,10}", message = "Debe ser un teléfono válido de 7 a 10 números", groups = Paso2.class)
    private String telefono;

    @URL(message = "Debe ser una URL válida")
    private String website;

    @NotBlank(message = "La ubicación es obligatoria", groups = Paso1.class)
    private String ubicacion;

    @Size(max = 300, message = "Máximo 300 caracteres")
    private String descripcion;

    @NotBlank(message = "El sector es obligatorio", groups = Paso1.class)
    private String sector;

    private String foto;
    private String portada;
    private List<String> publicacionesSociales = new ArrayList<>();
    private String threads;
    private String instagram;
    private String facebook;
    private String x;

    @Field("fechaRegistro")
    private LocalDate fechaRegistro = LocalDate.now();

    private EmpresaEstado estado = EmpresaEstado.APROBADA;

    private Rol rol;

    @DBRef(lazy = true)
    @ToString.Exclude
    private List<Vacante> vacantes = new ArrayList<>();

    private String authProvider = "LOCAL";
    private boolean perfilCompleto = false;

    public List<String> obtenerCamposFaltantes() {

        List<String> faltantes = new ArrayList<>();

        if (foto == null || foto.isBlank()) {
            faltantes.add("Sube el logo de tu empresa para generar más confianza.");
        }

        if (descripcion == null || descripcion.isBlank()) {
            faltantes.add("Agrega una descripción de tu empresa.");
        }

        if (website == null || website.isBlank()) {
            faltantes.add("Añade el sitio web oficial de tu empresa.");
        }

        if (telefono == null || telefono.isBlank()) {
            faltantes.add("Completa el número de contacto empresarial.");
        }

        if (ubicacion == null || ubicacion.isBlank()) {
            faltantes.add("Indica la ubicación principal de tu empresa.");
        }

        if (sector == null || sector.isBlank()) {
            faltantes.add("Selecciona el sector empresarial al que perteneces.");
        }

        return faltantes;
    }
}