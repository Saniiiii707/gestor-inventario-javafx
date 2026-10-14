package com.inventario.controlador;

import com.inventario.dao.CategoriaDAO;
import com.inventario.modelo.Categoria;
import com.inventario.vista.CategoriasView;
import com.inventario.modelo.Usuario;
import com.inventario.vista.Mensajes;
import javafx.scene.control.Alert;

public class CategoriasController {

    private final CategoriaDAO dao = new CategoriaDAO();
    private final CategoriasView vista;
    private final Usuario usuario;

    public CategoriasController(CategoriasView vista, Usuario usuario) {
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
            Mensajes.mostrar(Alert.AlertType.WARNING, "No tienes permiso para modificar categorías.");
            return false;
        }
        return true;
    }

    public void recargar() {
        vista.setCategorias(dao.listar());
    }

    private void agregar() {
        if (!tienePermiso()) return;
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
        if (!tienePermiso()) return;
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
        if (!tienePermiso()) return;
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
            if (Mensajes.esErrorDeRelacion(ex)) {
                Mensajes.mostrar(Alert.AlertType.WARNING,
                        "No se puede eliminar \"" + seleccionada.getNombre() + "\" porque tiene productos asignados.\n"
                        + "Cámbiales la categoría antes de eliminarla.");
            } else {
                Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
            }
        }
    }
}