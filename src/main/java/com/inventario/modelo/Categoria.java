package com.inventario.modelo;
import com.inventario.util.Validaciones;

public class Categoria {

    private int id;
    private String nombre;


    public Categoria(String nombre) {
        setNombre(nombre);
    }


    public Categoria(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = Validaciones.nombreValido(nombre, "nombre de la categoría");
    }

    @Override
    public String toString() {
        return id + " - " + nombre;
    }
}