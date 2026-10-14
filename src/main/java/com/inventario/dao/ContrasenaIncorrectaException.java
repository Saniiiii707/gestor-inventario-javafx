package com.inventario.dao;

// La contraseña actual escrita no coincide con la guardada.
// Excepción propia para no confundirla con IllegalArgumentException (reglas de la nueva).
public class ContrasenaIncorrectaException extends RuntimeException {
    public ContrasenaIncorrectaException() {
        super("La contraseña actual no es correcta.");
    }
}
