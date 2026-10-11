package com.inventario.controlador;

import com.inventario.dao.CategoriaDAO;
import com.inventario.modelo.Categoria;
import com.inventario.vista.CategoriasView;
import com.inventario.vista.Mensajes;
import javafx.scene.control.Alert;

public class CategoriasController {

    private final CategoriaDAO dao = new CategoriaDAO();
    private final CategoriasView vista;

    public CategoriasController(CategoriasView vista) {
        this.vista = vista;
        vista.setOnAgregar(this::agregar);
        vista.setOnActualizar(this::actualizar);
        vista.setOnEliminar(this::eliminar);
        recargar();
    }

    public void recargar() {
        vista.setCategorias(dao.listar());
    }

    private void agregar() {
        try {
            dao.insertar(new Categoria(vista.getNombreIngresado()));
            vista.limpiarFormulario();
            recargar();
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void actualizar() {
        Categoria seleccionada = vista.getSeleccionada();
        if (seleccionada == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona una categoría de la tabla.");
            return;
        }
        try {
            Categoria editada = new Categoria(vista.getNombreIngresado());
            editada.setId(seleccionada.getId());
            dao.actualizar(editada);
            vista.limpiarFormulario();
            recargar();
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void eliminar() {
        Categoria seleccionada = vista.getSeleccionada();
        if (seleccionada == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona una categoría de la tabla.");
            return;
        }
        if (!Mensajes.confirmar("¿Eliminar la categoría \"" + seleccionada.getNombre() + "\"?")) {
            return;
        }
        try {
            dao.eliminar(seleccionada.getId());
            vista.limpiarFormulario();
            recargar();
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}