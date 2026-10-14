package com.inventario.controlador;

import com.inventario.dao.ProveedorDAO;
import com.inventario.modelo.Proveedor;
import com.inventario.modelo.Usuario;
import com.inventario.vista.Mensajes;
import com.inventario.vista.ProveedoresView;
import javafx.scene.control.Alert;

public class ProveedoresController {

    private final ProveedorDAO dao = new ProveedorDAO();
    private final ProveedoresView vista;
    private final Usuario usuario;

    public ProveedoresController(ProveedoresView vista, Usuario usuario) {
        this.vista = vista;
        this.usuario = usuario;
        vista.setOnAgregar(this::agregar);
        vista.setOnActualizar(this::actualizar);
        vista.setOnEliminar(this::eliminar);
        recargar();
    }

    // Aunque la pestaña solo exista para quien puede, se vuelve a comprobar aquí
    private boolean tienePermiso() {
        if (!usuario.puedeEditarCatalogo()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "No tienes permiso para modificar proveedores.");
            return false;
        }
        return true;
    }

    public void recargar() {
        vista.setProveedores(dao.listar());
    }

    private void agregar() {
        if (!tienePermiso()) return;
        try {
            dao.insertar(new Proveedor(vista.getNombreIngresado(), vista.getTelefonoIngresado()));
            vista.limpiarFormulario();
            recargar();
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void actualizar() {
        if (!tienePermiso()) return;
        Proveedor seleccionado = vista.getSeleccionado();
        if (seleccionado == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona un proveedor de la tabla.");
            return;
        }
        try {
            Proveedor editado = new Proveedor(vista.getNombreIngresado(), vista.getTelefonoIngresado());
            editado.setId(seleccionado.getId());
            dao.actualizar(editado);
            vista.limpiarFormulario();
            recargar();
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void eliminar() {
        if (!tienePermiso()) return;
        Proveedor seleccionado = vista.getSeleccionado();
        if (seleccionado == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona un proveedor de la tabla.");
            return;
        }
        if (!Mensajes.confirmar("¿Eliminar al proveedor \"" + seleccionado.getNombre() + "\"?")) {
            return;
        }
        try {
            dao.eliminar(seleccionado.getId());
            vista.limpiarFormulario();
            recargar();
        } catch (RuntimeException ex) {
            if (Mensajes.esErrorDeRelacion(ex)) {
                Mensajes.mostrar(Alert.AlertType.WARNING,
                        "No se puede eliminar \"" + seleccionado.getNombre() + "\" porque tiene productos asignados.\n"
                        + "Asígnales otro proveedor o \"Sin proveedor\" antes de eliminarlo.");
            } else {
                Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
            }
        }
    }
}
