package com.inventario.modelo;

import com.inventario.util.Validaciones;

public class Proveedor {

    private int id;
    private String nombre;
    private String telefono;


    public Proveedor(String nombre, String telefono) {
        setNombre(nombre);
        setTelefono(telefono);
    }

    public Proveedor(int id, String nombre, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = Validaciones.nombreValido(nombre, "nombre del proveedor");
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) {
        this.telefono = Validaciones.telefonoOpcional(telefono);
    }
}
