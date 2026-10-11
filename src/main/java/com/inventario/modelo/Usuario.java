package com.inventario.modelo;

import com.inventario.util.Validaciones;

public class Usuario {

    private int id;
    private String username;
    private Rol rol;
    private boolean activo;

    public Usuario(String username, Rol rol) {
        setUsername(username);
        setRol(rol);
        this.activo = true;
    }

    public Usuario(int id, String username, Rol rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = Validaciones.usuarioValido(username);
    }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) {
        if (rol == null) {
            throw new IllegalArgumentException("Elige un rol.");
        }
        this.rol = rol;
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean esAdmin() { return rol == Rol.ADMIN; }
}