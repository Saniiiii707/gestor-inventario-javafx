package com.inventario.controlador;

import com.inventario.dao.ProveedorDAO;
import com.inventario.modelo.Proveedor;
import com.inventario.vista.Mensajes;
import com.inventario.vista.ProveedoresView;
import javafx.scene.control.Alert;

public class ProveedoresController {

    private final ProveedorDAO dao = new ProveedorDAO();
    private final ProveedoresView vista;

    public ProveedoresController(ProveedoresView vista) {
        this.vista = vista;
        vista.setOnAgregar(this::agregar);
        vista.setOnActualizar(this::actualizar);
        vista.setOnEliminar(this::eliminar);
        recargar();
    }

    public void recargar() {
        vista.setProveedores(dao.listar());
    }

    private void agregar() {
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
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}
