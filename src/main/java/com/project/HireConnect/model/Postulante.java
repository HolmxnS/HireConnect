package com.project.HireConnect.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.project.HireConnect.dto.MayorDeEdad;
import com.project.HireConnect.dto.Paso1;
import com.project.HireConnect.dto.Paso2;
import com.project.HireConnect.enums.PostulanteGenero;
import com.project.HireConnect.enums.PostulanteTipoDocumento;
import com.project.HireConnect.enums.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "postulantes")
@ToString(exclude = { "habilidades", "educacion", "experiencia", "postulaciones" })
@EqualsAndHashCode(exclude = { "habilidades", "educacion", "experiencia", "postulaciones" })
public class Postulante {

    @Id
    private String id;

    private boolean ocultarInvitacionPerfil;

    @Indexed(unique = true)
    @NotBlank(message = "El correo es obligatorio", groups = Paso2.class)
    @Email(message = "Debe ingresar un correo válido", groups = Paso2.class)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria", groups = Paso2.class)
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres", groups = Paso2.class)
    private String password;

    @Transient
    private String confirmPassword;

    @NotBlank(message = "El teléfono es obligatorio", groups = Paso2.class)
    @Pattern(regexp = "\\d{7,10}", message = "Debe ser un teléfono válido de 7 a 10 números", groups = Paso2.class)
    private String telefono;

    @NotBlank(message = "El nombre es obligatorio", groups = Paso1.class)
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres", groups = Paso1.class)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio", groups = Paso1.class)
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres", groups = Paso1.class)
    private String apellido;

    @NotBlank(message = "La identificación es obligatoria", groups = Paso1.class)
    @Pattern(regexp = "\\d{5,15}", message = "La identificación debe tener entre 5 y 15 números", groups = Paso1.class)
    private String identificacion;

    @NotNull(message = "Debe seleccionar un tipo de documento", groups = Paso1.class)
    @Field("tipoDocumento")
    private PostulanteTipoDocumento tipoDocumento;

    @NotNull(message = "La fecha de nacimiento es obligatoria", groups = Paso1.class)
    @Past(message = "Debe ingresar una fecha válida en el pasado", groups = Paso1.class)
    @MayorDeEdad(message = "Debe ser mayor o igual a 18 años", groups = Paso1.class)
    @Field("fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @NotNull(message = "Debe seleccionar un género", groups = Paso1.class)
    private PostulanteGenero genero;

    @Size(max = 100, message = "La titulación no puede superar 100 caracteres")
    private String titulacion;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    @Size(max = 50, message = "La nacionalidad no puede superar 50 caracteres")
    private String nacionalidad;

    private Rol rol;

    @Transient
    private Integer edad;

    private String foto;
    private String cv;

    @Field("fechaRegistro")
    private LocalDate fechaRegistro = LocalDate.now();

    public Integer getEdad() {
        if (this.fechaNacimiento != null) {
            return java.time.Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
        }
        return null;
    }

    private Set<String> habilidades = new HashSet<>();

    private List<Educacion_postulante> educacion = new ArrayList<>();

    private List<Experiencia_postulante> experiencia = new ArrayList<>();

    @DBRef(lazy = true)
    private Set<Postulacion> postulaciones = new HashSet<>();

    private String authProvider = "LOCAL";
    private boolean perfilCompleto = false;

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isPerfilCompleto() {
        return perfilCompleto;
    }

    public void setPerfilCompleto(boolean perfilCompleto) {
        this.perfilCompleto = perfilCompleto;
    }
}
