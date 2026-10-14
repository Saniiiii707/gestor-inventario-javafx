package com.inventario.controlador;

import com.inventario.dao.MovimientoDAO;
import com.inventario.dao.ProductoDAO;
import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.modelo.TipoMovimiento;
import com.inventario.modelo.Usuario;

import com.inventario.vista.Mensajes;
import com.inventario.vista.MovimientosView;
import javafx.scene.control.Alert;
import java.util.ArrayList;
import java.util.List;

public class MovimientosController {

    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientosView vista;
    private final Usuario usuario; // quien inició sesión; queda registrado en cada movimiento

    public MovimientosController(MovimientosView vista, Usuario usuario) {
        this.vista = vista;
        this.usuario = usuario;
        vista.setOnRegistrar(this::registrar);

        // Solo se ofrecen los tipos que este usuario puede registrar (la regla vive en Usuario)
        List<TipoMovimiento> permitidos = new ArrayList<>();
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            if (usuario.puedeRegistrar(tipo)) {
                permitidos.add(tipo);
            }
        }
        vista.setTiposPermitidos(permitidos);

        recargar();
    }

    public void recargar() {
        vista.setProductos(productoDAO.listar());
        vista.setMovimientos(movimientoDAO.listar());
    }

    // Al entrar a la pestaña: formulario limpio y datos frescos (pueden haber cambiado en otra)
    public void alEntrar() {
        vista.limpiarFormulario();
        recargar();
    }

    private void registrar() {
        try {
            TipoMovimiento tipo = vista.getTipoElegido();
            // Defensa en profundidad: aunque el desplegable no lo ofrezca, se vuelve a comprobar
            if (tipo != null && !usuario.puedeRegistrar(tipo)) {
                throw new IllegalArgumentException("No tienes permiso para registrar movimientos de tipo " + tipo + ".");
            }
            Producto producto = vista.getProductoElegido();
            if (producto == null) {
                throw new IllegalArgumentException("Elige un producto.");
            }
            int cantidad = Integer.parseInt(vista.getCantidadIngresada().trim());
            String descripcion = vista.getDescripcionIngresada();
            if (tipo == TipoMovimiento.AJUSTE) {
                // El usuario informa el stock contado, nunca la diferencia
                if (cantidad < 0) {
                    throw new IllegalArgumentException("El stock contado no puede ser negativo.");
                }
                if (cantidad > Producto.MAX_STOCK) {
                    throw new IllegalArgumentException("El stock contado no puede superar " + Producto.MAX_STOCK + ".");
                }
                if (descripcion == null || descripcion.trim().isEmpty()) {
                    throw new IllegalArgumentException("La descripción es obligatoria para un ajuste.");
                }
                movimientoDAO.registrarAjuste(producto.getId(), usuario.getId(), cantidad,
                        producto.getStockActual(), descripcion);
            } else {
                // El modelo valida el tipo y la cantidad al crearse
                Movimiento movimiento = new Movimiento(
                        producto.getId(), usuario.getId(), tipo, cantidad, descripcion);
                movimientoDAO.registrar(movimiento);
            }
            vista.limpiarFormulario();
            recargar();
        } catch (NumberFormatException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "La cantidad debe ser un número entero.");
        } catch (IllegalStateException ex) {
            // El stock cambió o no alcanza: avisar, refrescar y conservar lo que se pueda
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
            Producto elegido = vista.getProductoElegido();   // guardarlo antes de recargar
            TipoMovimiento tipo = vista.getTipoElegido();
            recargar();
            if (elegido != null) {
                vista.seleccionarProducto(elegido.getId());  // mismo producto, stock actualizado
            }
            if (tipo == TipoMovimiento.AJUSTE) {
                vista.limpiarCantidad(); // el conteo anterior ya no sirve: hay que volver a contar
            }
        } catch (IllegalArgumentException ex) {
            Mensajes.mostrar(Alert.AlertType.WARNING, ex.getMessage());
        } catch (RuntimeException ex) {
            Mensajes.mostrar(Alert.AlertType.ERROR, Mensajes.traducirError(ex));
        }
    }
}
