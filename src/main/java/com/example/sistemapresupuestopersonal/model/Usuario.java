package com.example.sistemapresupuestopersonal.model;

import java.time.LocalDate;
import java.util.Objects;

public class Usuario {
    private long id;
    private String nombre;
    private String correo;
    private String contrasena;
    private boolean activo;
    private LocalDate fechaCreacion;

    public Usuario() {
        this.id = 0L;
        this.nombre = "";
        this.correo = "";
        this.contrasena = "";
        this.activo = true;
        this.fechaCreacion = LocalDate.now();
    }

    public Usuario(long id, String nombre, String correo) {
        this();
        this.id = id;
        this.nombre = nombre != null ? nombre : "";
        this.correo = correo != null ? correo : "";
    }

    public Usuario(long id, String nombre, String correo, String contrasena, boolean activo, LocalDate fechaCreacion) {
        this.id = id;
        this.nombre = nombre != null ? nombre : "";
        this.correo = correo != null ? correo : "";
        this.contrasena = contrasena != null ? contrasena : "";
        this.activo = activo;
        this.fechaCreacion = fechaCreacion != null ? fechaCreacion : LocalDate.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id == usuario.id && activo == usuario.activo && Objects.equals(nombre, usuario.nombre) &&
                Objects.equals(correo, usuario.correo) && Objects.equals(contrasena, usuario.contrasena) &&
                Objects.equals(fechaCreacion, usuario.fechaCreacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, correo, contrasena, activo, fechaCreacion);
    }
}