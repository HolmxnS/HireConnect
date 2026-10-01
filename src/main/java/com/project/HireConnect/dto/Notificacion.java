package com.project.HireConnect.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Notificacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String mensaje;
    private String nombreRelacionado;
    private TipoNotificacion tipo;
    private LocalDateTime fecha;
    private boolean leida = false;

    public enum TipoNotificacion {
        POSTULANTE,
        EMPRESA
    }

    public Notificacion() {
        this.fecha = LocalDateTime.now();
        this.id = System.currentTimeMillis();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getNombreRelacionado() {
        return nombreRelacionado;
    }

    public void setNombreRelacionado(String nombreRelacionado) {
        this.nombreRelacionado = nombreRelacionado;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}