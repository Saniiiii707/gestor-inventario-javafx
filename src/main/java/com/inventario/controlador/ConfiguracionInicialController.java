package com.inventario.controlador;

import com.inventario.dao.UsuarioDAO;
import com.inventario.modelo.Rol;
import com.inventario.modelo.Usuario;
import com.inventario.util.Validaciones;
import com.inventario.vista.ConfiguracionInicialView;
import com.inventario.vista.Mensajes;
import java.util.function.Consumer;
import javafx.scene.control.Alert;

public class ConfiguracionInicialController {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final ConfiguracionInicialView vista;
    private final Consumer<Usuario> alCrear; // qué hacer cuando el administrador queda creado

    public ConfiguracionInicialController(ConfiguracionInicialView vista, Consumer<Usuario> alCrear) {
        this.vista = vista;
        this.alCrear = alCrear;
        vista.setOnCrear(this::crear);
    }

    private void crear() {
        // a) Validar el usuario: si está mal, se borra solo ese campo
        Usuario admin;
        try {
            admin = new Usuario(vista.getUsuarioIngresado(), Rol.ADMIN);
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
            vista.limpiarUsuario();
            return;
        }

        // b) Las dos contraseñas deben coincidir: si no, se borran solo ellas
        String contrasena = vista.getContrasenaIngresada();
        if (!contrasena.equals(vista.getRepeticionIngresada())) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Las contraseñas no coinciden.");
            vista.limpiarContrasenas();
            return;
        }

        // c) Validar la contraseña antes de ir al DAO (que la vuelve a validar)
        try {
            Validaciones.contrasenaValida(contrasena);
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
            vista.limpiarContrasenas();
            return;
        }

        // d) Guardar: ante un error no se limpia ningún campo
        try {
            dao.crearAdministradorInicial(admin, contrasena);
            Mensajes.mostrar(Alert.AlertType.INFORMATION,
                    "Administrador \"" + admin.getUsername() + "\" creado. Usa ese nombre para iniciar sesión.");
            alCrear.accept(admin);
        } catch (IllegalStateException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage()); // ya estaba creado
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}
