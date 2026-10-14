package com.inventario.controlador;

import com.inventario.dao.ContrasenaIncorrectaException;
import com.inventario.dao.UsuarioDAO;
import com.inventario.modelo.Usuario;
import com.inventario.vista.CambiarContrasenaView;
import com.inventario.vista.Mensajes;
import javafx.scene.control.Alert;

public class CambiarContrasenaController {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final CambiarContrasenaView vista;
    private final Usuario sesion;
    private final Runnable alTerminar;   // qué hacer después de cambiarla (lo decide App)

    public CambiarContrasenaController(CambiarContrasenaView vista, Usuario sesion, boolean obligatorio,
                                       Runnable alTerminar, Runnable alCancelar) {
        this.vista = vista;
        this.sesion = sesion;
        this.alTerminar = alTerminar;
        vista.setModoObligatorio(obligatorio);
        vista.setOnCambiar(this::cambiar);
        vista.setOnCancelar(alCancelar);
    }

    private void cambiar() {
        // a) La actual no puede estar vacía (no hace falta consultar la base para eso)
        if (vista.getActual().isEmpty()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Escribe tu contraseña actual.");
            return;
        }
        // b) Las nuevas: iguales entre sí y válidas. Si fallan, se limpian solo ellas.
        String nueva = vista.getNueva();
        if (!ValidacionContrasena.contrasenaAceptada(nueva, vista.getRepeticion(), vista::limpiarNuevas)) {
            return;
        }
        // c) Guardar: el DAO comprueba la actual y que la nueva sea distinta
        try {
            dao.cambiarContrasena(sesion.getId(), vista.getActual(), nueva);
            Mensajes.mostrar(Alert.AlertType.INFORMATION, "Tu contraseña se cambió correctamente.");
            alTerminar.run();
        } catch (ContrasenaIncorrectaException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
            vista.limpiarActual();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());   // problemas de la contraseña nueva
            vista.limpiarNuevas();
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}
