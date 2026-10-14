package com.inventario.controlador;

import com.inventario.dao.CategoriaDAO;
import com.inventario.dao.ProductoDAO;
import com.inventario.dao.ProveedorDAO;
import com.inventario.modelo.Categoria;
import com.inventario.modelo.Producto;
import com.inventario.vista.Mensajes;
import com.inventario.vista.ProductosView;
import java.util.List;
import javafx.scene.control.Alert;
import com.inventario.modelo.Usuario;

public class ProductosController {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final ProductosView vista;
    private final Usuario usuario;

    public ProductosController(ProductosView vista, Usuario usuario) {
        this.vista = vista;
        this.usuario = usuario;
        vista.setSoloLectura(!usuario.puedeEditarCatalogo());
        vista.setOnAgregar(this::agregar);
        vista.setOnActualizar(this::actualizar);
        vista.setOnEliminar(this::eliminar);
        recargar();
    }

    public void recargar() {
        vista.limpiarFormulario();
        vista.setCategorias(categoriaDAO.listar());
        vista.setProveedores(proveedorDAO.listar());
        List<Producto> lista = productoDAO.listar();
        vista.setProductos(lista);
        vista.setAlertaStockBajo(lista.stream().filter(Producto::tieneStockBajo).count());
    }

    // Aunque la vista oculte el formulario, se vuelve a comprobar aquí
    private boolean tienePermiso() {
        if (!usuario.puedeEditarCatalogo()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "No tienes permiso para modificar productos.");
            return false;
        }
        return true;
    }

    // Lee el formulario y arma un Producto; lanza excepciones si hay datos inválidos
    private Producto leerFormulario() {
        Categoria categoria = vista.getCategoriaElegida();
        if (categoria == null) {
            throw new IllegalArgumentException("Elige una categoría.");
        }
        double precio = Double.parseDouble(vista.getPrecioIngresado().trim().replace(',', '.'));
        int stockMinimo = Integer.parseInt(vista.getStockMinimoIngresado().trim());
        return new Producto(vista.getNombreIngresado(), precio, stockMinimo,
                categoria.getId(), vista.getProveedorIdElegido());
    }

    private void agregar() {
        if (!tienePermiso()) return;
        try {
            productoDAO.insertar(leerFormulario());
            vista.limpiarFormulario();
            recargar();
        } catch (NumberFormatException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "El precio y el stock mínimo deben ser números válidos.");
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void actualizar() {
        if (!tienePermiso()) return;
        Producto seleccionado = vista.getSeleccionado();
        if (seleccionado == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona un producto de la tabla.");
            return;
        }
        try {
            Producto editado = leerFormulario();
            editado.setId(seleccionado.getId());
            productoDAO.actualizar(editado);
            vista.limpiarFormulario();
            recargar();
        } catch (NumberFormatException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "El precio y el stock mínimo deben ser números válidos.");
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }

    private void eliminar() {
        if (!tienePermiso()) return;
        Producto seleccionado = vista.getSeleccionado();
        if (seleccionado == null) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "Selecciona un producto de la tabla.");
            return;
        }
        if (!Mensajes.confirmar("¿Eliminar el producto \"" + seleccionado.getNombre() + "\"?")) {
            return;
        }
        try {
            productoDAO.eliminar(seleccionado.getId());
            vista.limpiarFormulario();
            recargar();
        } catch (RuntimeException ex) {
            if (Mensajes.esErrorDeRelacion(ex)) {
                Mensajes.mostrar(Alert.AlertType.WARNING,
                        "No se puede eliminar \"" + seleccionado.getNombre() + "\" porque tiene movimientos registrados.\n"
                        + "Su historial debe conservarse.");
            } else {
                Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
            }
        }
    }
}