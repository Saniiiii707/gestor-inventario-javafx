package com.inventario.controlador;

import com.inventario.util.Validaciones;
import com.inventario.vista.Mensajes;
import javafx.scene.control.Alert;

final class ValidacionContrasena {

    private ValidacionContrasena() { }

    // Comprueba que la repetición coincida y que la contraseña cumpla las reglas.
    // Si falla, avisa y ejecuta 'limpiar'. Devuelve true si se puede seguir.
    static boolean contrasenaAceptada(String contrasena, String repeticion, Runnable limpiar) {
        if (!contrasena.equals(repeticion)) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Las contraseñas no coinciden.");
            limpiar.run();
            return false;
        }
        try {
            Validaciones.contrasenaValida(contrasena);
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
            limpiar.run();
            return false;
        }
        return true;
    }
}
