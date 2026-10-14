package com.inventario.controlador;

import com.inventario.dao.UsuarioDAO;
import com.inventario.modelo.Usuario;
import com.inventario.vista.LoginView;
import com.inventario.vista.Mensajes;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.control.Alert;

public class LoginController {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final LoginView vista;
    private final Consumer<Usuario> alIniciarSesion; // qué hacer cuando alguien entra (lo decide App)

    public LoginController(LoginView vista, Consumer<Usuario> alIniciarSesion) {
        this.vista = vista;
        this.alIniciarSesion = alIniciarSesion;
        vista.setOnEntrar(this::entrar);
    }

    private void entrar() {
        String usuario = vista.getUsuarioIngresado();
        String contrasena = vista.getContrasenaIngresada();
        // Campos vacíos: avisar sin consultar la base (no revela nada de ningún usuario)
        if (usuario.isBlank() || contrasena.isEmpty()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Escribe tu usuario y tu contraseña.");
            return;
        }
        try {
            Optional<Usuario> encontrado = dao.autenticar(usuario, contrasena);
            if (encontrado.isPresent()) {
                alIniciarSesion.accept(encontrado.get());
            } else {
                // Mismo mensaje para todos los motivos: no revela si el usuario existe
                Mensajes.mostrar(Alert.AlertType.WARNING,
                        "Usuario o contraseña incorrectos.\n"
                        + "Si fallaste varias veces seguidas, espera " + UsuarioDAO.MINUTOS_BLOQUEO
                        + " minutos antes de volver a intentar.");
                vista.limpiarContrasena();
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}
