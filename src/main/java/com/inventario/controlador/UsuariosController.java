package com.inventario.controlador;

import com.inventario.dao.UsuarioDAO;
import com.inventario.modelo.Rol;
import com.inventario.modelo.Usuario;
import com.inventario.vista.Mensajes;
import com.inventario.vista.UsuariosView;
import javafx.scene.control.Alert;

public class UsuariosController {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final UsuariosView vista;
    private final Usuario sesion; // quien inició sesión (no confundir con el usuario que se crea)

    public UsuariosController(UsuariosView vista, Usuario sesion) {
        this.vista = vista;
        this.sesion = sesion;
        vista.setIdSesion(sesion.getId());
        vista.setOnCrear(this::crear);
        vista.setOnCambiarRol(this::cambiarRol);
        vista.setOnRestablecer(this::restablecerContrasena);
        vista.setOnDesactivar(this::desactivar);
        vista.setOnReactivar(this::reactivar);
        recargar();
    }

    public void recargar() {
        vista.setUsuarios(dao.listar());
    }

    private void crear() {
        // Defensa en profundidad: la pestaña solo existe para quien puede, pero se vuelve a comprobar
        if (!sesion.puedeGestionarUsuarios()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "No tienes permiso para gestionar usuarios.");
            return;
        }

        // a) Rol (se revisa aparte para no borrar el usuario si lo único que falta es el rol)
        Rol rol = vista.getRolElegido();
        if (rol == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Elige un rol.");
            return;
        }

        // b) Nombre de usuario: si es inválido, se borra solo ese campo
        Usuario nuevo;
        try {
            nuevo = new Usuario(vista.getUsuarioIngresado(), rol);
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
            vista.limpiarUsuario();
            return;
        }

        // c) Contraseñas: si fallan, se borran solo ellas
        String contrasena = vista.getContrasenaIngresada();
        if (!ValidacionContrasena.contrasenaAceptada(contrasena, vista.getRepeticionIngresada(),
                vista::limpiarContrasenas)) {
            return;
        }

        // d) Guardar
        try {
            dao.crear(nuevo, contrasena);
            Mensajes.mostrar(Alert.AlertType.INFORMATION,
                    "Usuario \"" + nuevo.getUsername() + "\" creado como " + nuevo.getRol() + ".\n"
                    + "Deberá cambiar la contraseña la primera vez que inicie sesión.");
            vista.limpiarFormulario();
            recargar();
        } catch (RuntimeException ex) {
            if (Mensajes.esErrorDeDuplicado(ex)) {
                Mensajes.mostrar(Alert.AlertType.WARNING,
                        "Ya existe un usuario llamado \"" + nuevo.getUsername() + "\". Elige otro nombre.");
                vista.limpiarUsuario();
            } else {
                Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
            }
        }
    }

    // Devuelve el usuario elegido si se puede modificar; si no, avisa y devuelve null
    private Usuario elegidoParaModificar() {
        if (!sesion.puedeGestionarUsuarios()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "No tienes permiso para gestionar usuarios.");
            return null;
        }
        Usuario elegido = vista.getSeleccionado();
        if (elegido == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona un usuario de la tabla.");
            return null;
        }
        // Protección: nadie se modifica a sí mismo desde aquí (ni rol, ni estado, ni contraseña)
        if (elegido.getId() == sesion.getId()) {
            Mensajes.mostrar(Alert.AlertType.WARNING,
                    "No puedes modificar tu propia cuenta desde aquí. Pídeselo a otro administrador.");
            return null;
        }
        return elegido;
    }

    // Ejecuta una acción sobre un usuario y maneja el resultado igual para todas
    private void ejecutar(Runnable accion, String mensajeExito) {
        ejecutar(accion, mensajeExito, () -> { });
    }

    private void ejecutar(Runnable accion, String mensajeExito, Runnable siHayAviso) {
        try {
            accion.run();
            Mensajes.mostrar(Alert.AlertType.INFORMATION, mensajeExito);
            vista.limpiarFormulario();
            recargar();
        } catch (IllegalStateException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());   // por ejemplo, el último ADMIN
            siHayAviso.run();
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void cambiarRol() {
        Usuario elegido = elegidoParaModificar();
        Rol nuevoRol = vista.getRolElegido();
        if (elegido == null || nuevoRol == null) {
            return;
        }
        if (!Mensajes.confirmar("¿Cambiar el rol de \"" + elegido.getUsername() + "\" a " + nuevoRol + "?")) {
            return;
        }
        ejecutar(() -> dao.cambiarRol(elegido.getId(), nuevoRol),
                "\"" + elegido.getUsername() + "\" ahora es " + nuevoRol + ".");
    }

    private void desactivar() {
        Usuario elegido = elegidoParaModificar();
        if (elegido == null) {
            return;
        }
        if (!Mensajes.confirmar("¿Desactivar a \"" + elegido.getUsername() + "\"?\n"
                + "No podrá iniciar sesión, pero su historial de movimientos se conserva.")) {
            return;
        }
        ejecutar(() -> dao.desactivar(elegido.getId()),
                "\"" + elegido.getUsername() + "\" fue desactivado.");
    }

    private void reactivar() {
        Usuario elegido = elegidoParaModificar();
        if (elegido == null) {
            return;
        }
        ejecutar(() -> dao.reactivar(elegido.getId()),
                "\"" + elegido.getUsername() + "\" fue reactivado.");
    }

    private void restablecerContrasena() {
        Usuario elegido = elegidoParaModificar();
        if (elegido == null) {
            return;
        }
        String contrasena = vista.getContrasenaIngresada();
        if (!ValidacionContrasena.contrasenaAceptada(contrasena, vista.getRepeticionIngresada(),
                vista::limpiarContrasenas)) {
            return;
        }
        if (!Mensajes.confirmar("¿Restablecer la contraseña de \"" + elegido.getUsername() + "\"?")) {
            return;
        }
        ejecutar(() -> dao.restablecerContrasena(elegido.getId(), contrasena),
                "Contraseña de \"" + elegido.getUsername() + "\" restablecida.\n"
                + "Comunícasela en persona. Deberá cambiarla al iniciar sesión.",
                vista::limpiarContrasenas);
    }
}
