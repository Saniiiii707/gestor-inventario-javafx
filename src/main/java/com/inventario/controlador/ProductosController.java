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

public class ProductosController {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final ProductosView vista;

    public ProductosController(ProductosView vista) {
        this.vista = vista;
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
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}