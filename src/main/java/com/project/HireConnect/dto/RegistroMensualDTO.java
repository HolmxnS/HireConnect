package com.project.HireConnect.dto;

import java.time.LocalDate;

public class RegistroMensualDTO {

    private String nombre;
    private String tipoCuenta;
    private String correo;
    private LocalDate fechaRegistro;
    private String foto;

    public RegistroMensualDTO(String nombre, String tipoCuenta,
                              String correo, LocalDate fechaRegistro,
                              String foto) {
        this.nombre = nombre;
        this.tipoCuenta = tipoCuenta;
        this.correo = correo;
        this.fechaRegistro = fechaRegistro;
        this.foto = foto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}