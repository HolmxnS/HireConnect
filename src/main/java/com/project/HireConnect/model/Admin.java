package com.project.HireConnect.model;

import com.project.HireConnect.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admins")
public class Admin {

    @Id
    private String id;

    private String nombre;
    private String apellido;
    private String foto;
    private String about;

    @Indexed(unique = true) 
    private String email;

    private String password;

    
    private Rol rol;
}
