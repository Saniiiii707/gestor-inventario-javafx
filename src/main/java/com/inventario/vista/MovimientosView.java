package com.inventario.vista;

import com.inventario.modelo.Movimiento;
import com.inventario.modelo.Producto;
import com.inventario.modelo.TipoMovimiento;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class MovimientosView extends VBox {

    private final ObservableList<Movimiento> movimientos = FXCollections.observableArrayList();
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();

    private final TableView<Movimiento> tabla = new TableView<>(movimientos);
    private final ComboBox<Producto> comboProducto = new ComboBox<>(productos);
    private final ComboBox<TipoMovimiento> comboTipo =
            new ComboBox<>(FXCollections.observableArrayList(TipoMovimiento.values()));
    private final TextField campoCantidad = new TextField();
    private final TextField campoDescripcion = new TextField();
    private final Button btnRegistrar = new Button("Registrar");
    private final Label lblResumen = new Label();

    public MovimientosView() {
        super(10);
        setPadding(new Insets(15));

        TableColumn<Movimiento, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(150);

        TableColumn<Movimiento, String> colProducto = new TableColumn<>("Producto");
        colProducto.setCellValueFactory(new PropertyValueFactory<>("productoNombre"));
        colProducto.setPrefWidth(200);

        TableColumn<Movimiento, TipoMovimiento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(90);

        TableColumn<Movimiento, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCantidad.setPrefWidth(80);

        TableColumn<Movimiento, Integer> colStockAnterior = new TableColumn<>("Stock anterior");
        colStockAnterior.setCellValueFactory(new PropertyValueFactory<>("stockAnterior"));
        colStockAnterior.setPrefWidth(120);

        TableColumn<Movimiento, Integer> colStockResultante = new TableColumn<>("Stock resultante");
        colStockResultante.setCellValueFactory(new PropertyValueFactory<>("stockResultante"));
        colStockResultante.setPrefWidth(135);

        TableColumn<Movimiento, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(250);

        tabla.getColumns().addAll(colFecha, colProducto, colTipo, colCantidad,
                colStockAnterior, colStockResultante, colDescripcion);

        comboProducto.setPromptText("Producto");
        comboProducto.setPrefWidth(280);
        comboProducto.setConverter(new StringConverter<>() {
            @Override
            public String toString(Producto p) {
                return p == null ? "" : p.getNombre() + " (stock: " + p.getStockActual() + ")";
            }

            @Override
            public Producto fromString(String texto) {
                return null;
            }
        });

        comboTipo.setPromptText("Tipo");
        campoCantidad.setPromptText("Cantidad");
        campoDescripcion.setPromptText("Descripción");
        HBox.setHgrow(campoDescripcion, Priority.ALWAYS); // ocupa todo el ancho libre

        comboProducto.valueProperty().addListener((obs, anterior, nuevo) -> actualizarResumen());
        comboTipo.valueProperty().addListener((obs, anterior, nuevo) -> actualizarResumen());
        campoCantidad.textProperty().addListener((obs, anterior, nuevo) -> actualizarResumen());

        HBox filaDatos = new HBox(10, comboProducto, comboTipo, campoCantidad);
        HBox filaDescripcion = new HBox(10, campoDescripcion, btnRegistrar);
        getChildren().addAll(filaDatos, lblResumen, filaDescripcion, tabla);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    // Tipos que se ofrecen en el desplegable (según los permisos de quien inició sesión)
    public void setTiposPermitidos(List<TipoMovimiento> tipos) {
        comboTipo.getItems().setAll(tipos);
    }

    public void setProductos(List<Producto> lista) { productos.setAll(lista); }
    public void setMovimientos(List<Movimiento> lista) { movimientos.setAll(lista); }

    public Producto getProductoElegido() { return comboProducto.getValue(); }
    public TipoMovimiento getTipoElegido() { return comboTipo.getValue(); }
    public String getCantidadIngresada() { return campoCantidad.getText(); }
    public String getDescripcionIngresada() { return campoDescripcion.getText(); }

    public void limpiarFormulario() {
        comboProducto.setValue(null);
        comboTipo.setValue(null);
        campoCantidad.clear();
        campoDescripcion.clear();
        actualizarResumen(); // asegura que el aviso no quede con datos viejos
    }

    // Vuelve a elegir un producto por id. Tras recargar, la lista tiene objetos nuevos,
    // así que hay que buscarlo por id y no por referencia.
    public void seleccionarProducto(int productoId) {
        for (Producto p : productos) {
            if (p.getId() == productoId) {
                comboProducto.setValue(p);
                return;
            }
        }
        comboProducto.setValue(null); // ya no existe (por ejemplo, se eliminó)
    }

    public void limpiarCantidad() {
        campoCantidad.clear();
    }

    public void setOnRegistrar(Runnable accion) {
        btnRegistrar.setOnAction(e -> accion.run());
    }

    private void actualizarResumen() {
        Producto p = comboProducto.getValue();
        TipoMovimiento tipo = comboTipo.getValue();
        String texto = campoCantidad.getText() == null ? "" : campoCantidad.getText().trim();
        lblResumen.setStyle("");

        // En un ajuste el campo guarda el stock contado, no una cantidad a sumar/restar
        campoCantidad.setPromptText(tipo == TipoMovimiento.AJUSTE ? "Stock contado" : "Cantidad");

        if (p == null) {
            lblResumen.setText("");
            return;
        }
        if (tipo == null || texto.isEmpty()) {
            lblResumen.setText("Stock actual: " + p.getStockActual());
            return;
        }
        try {
            int cantidad = Integer.parseInt(texto);
            if (tipo == TipoMovimiento.AJUSTE) {
                if (cantidad < 0) {
                    lblResumen.setText("El stock contado no puede ser negativo.");
                    return;
                }
                if (cantidad > Producto.MAX_STOCK) {
                    lblResumen.setText("El stock contado no puede superar " + Producto.MAX_STOCK + ".");
                    return;
                }
                int anterior = p.getStockActual();
                int diferencia = cantidad - anterior;
                String texto2 = "Sistema: " + anterior + " | Contado: " + cantidad
                        + " | Diferencia: " + (diferencia >= 0 ? "+" : "") + diferencia;
                if (p.esStockBajo(cantidad)) {
                    texto2 += "  (quedará en stock bajo; mínimo: " + p.getStockMinimo() + ")";
                    lblResumen.setStyle("-fx-text-fill: darkorange;");
                } else {
                    lblResumen.setStyle("");
                }
                lblResumen.setText(texto2);
            } else {
                if (cantidad <= 0) {
                    lblResumen.setText("La cantidad debe ser mayor que cero.");
                    return;
                }
                if (cantidad > Movimiento.MAX_CANTIDAD) {
                    lblResumen.setText("La cantidad no puede superar " + Movimiento.MAX_CANTIDAD + ".");
                    return;
                }
                boolean esEntrada = tipo == TipoMovimiento.ENTRADA;
                int resultante = esEntrada ? p.getStockActual() + cantidad : p.getStockActual() - cantidad;
                String texto2 = (esEntrada ? "SUMA " : "RESTA ") + cantidad
                        + ": el stock pasará de " + p.getStockActual() + " a " + resultante;
                if (resultante < 0) {
                    texto2 += "  (stock insuficiente)";
                    lblResumen.setStyle("-fx-text-fill: red;");
                } else if (p.esStockBajo(resultante)) {
                    texto2 += "  (quedará en stock bajo; mínimo: " + p.getStockMinimo() + ")";
                    lblResumen.setStyle("-fx-text-fill: darkorange;");
                }
                lblResumen.setText(texto2);
            }
        } catch (NumberFormatException e) {
            lblResumen.setText("Stock actual: " + p.getStockActual() + "   |   (escribe un número entero)");
        }
    }
}
