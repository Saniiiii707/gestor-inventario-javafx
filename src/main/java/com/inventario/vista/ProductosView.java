package com.inventario.vista;

import com.inventario.modelo.Categoria;
import com.inventario.modelo.Producto;
import com.inventario.modelo.Proveedor;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class ProductosView extends VBox {

    // Opción "vacía" del desplegable: un producto puede no tener proveedor.
    // Existe solo en la vista; hacia afuera se traduce a null.
    private static final Proveedor SIN_PROVEEDOR = new Proveedor(0, "Sin proveedor", null);

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final ObservableList<Categoria> categorias = FXCollections.observableArrayList();
    private final ObservableList<Proveedor> proveedores = FXCollections.observableArrayList();

    private final TableView<Producto> tabla = new TableView<>(productos);
    private final TextField campoNombre = new TextField();
    private final TextField campoPrecio = new TextField();
    private final TextField campoStockMinimo = new TextField();
    private final ComboBox<Categoria> comboCategoria = new ComboBox<>(categorias);
    private final ComboBox<Proveedor> comboProveedor = new ComboBox<>(proveedores);
    private final Label lblAlerta = new Label();
    private final Button btnAgregar = new Button("Agregar");
    private final Button btnActualizar = new Button("Actualizar");
    private final Button btnEliminar = new Button("Eliminar");
    private final Button btnLimpiar = new Button("Limpiar");

    public ProductosView() {
        super(10);
        setPadding(new Insets(15));

        // --- Tabla ---
        TableColumn<Producto, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Producto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(180);

        TableColumn<Producto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoriaNombre"));
        colCategoria.setPrefWidth(120);

        TableColumn<Producto, String> colProveedor = new TableColumn<>("Proveedor");
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedorNombre"));
        colProveedor.setPrefWidth(140);

        TableColumn<Producto, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colPrecio.setPrefWidth(80);

        TableColumn<Producto, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colStock.setPrefWidth(60);

        TableColumn<Producto, Integer> colMinimo = new TableColumn<>("Mínimo");
        colMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colMinimo.setPrefWidth(70);

        tabla.getColumns().addAll(colId, colNombre, colCategoria, colProveedor,
                colPrecio, colStock, colMinimo);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                campoNombre.setText(nuevo.getNombre());
                campoPrecio.setText(String.valueOf(nuevo.getPrecio()));
                campoStockMinimo.setText(String.valueOf(nuevo.getStockMinimo()));
                seleccionarCategoria(nuevo.getCategoriaId());
                seleccionarProveedor(nuevo.getProveedorId());
            }
        });

        // Resalta en rojo las filas con stock bajo (sin pisar el color de la fila seleccionada)
        tabla.setRowFactory(tv -> {
            TableRow<Producto> fila = new TableRow<>();
            fila.itemProperty().addListener((obs, anterior, producto) -> pintar(fila));
            fila.selectedProperty().addListener((obs, antes, ahora) -> pintar(fila));
            return fila;
        });

        // --- Formulario ---
        campoNombre.setPromptText("Nombre");
        campoPrecio.setPromptText("Precio");
        campoStockMinimo.setPromptText("Stock mínimo");
        comboCategoria.setPromptText("Categoría");
        comboProveedor.setValue(SIN_PROVEEDOR);

        comboCategoria.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categoria c) {
                return c == null ? "" : c.getNombre();
            }

            @Override
            public Categoria fromString(String texto) {
                return null;
            }
        });

        comboProveedor.setConverter(new StringConverter<>() {
            @Override
            public String toString(Proveedor p) {
                return p == null ? "" : p.getNombre();
            }

            @Override
            public Proveedor fromString(String texto) {
                return null;
            }
        });

        // Modo "nuevo" (sin fila elegida) o "edición" (con fila elegida y cambios)
        BooleanBinding sinSeleccion = tabla.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding sinCambios = Bindings.createBooleanBinding(
                () -> !hayCambios(),
                tabla.getSelectionModel().selectedItemProperty(),
                campoNombre.textProperty(), campoPrecio.textProperty(),
                campoStockMinimo.textProperty(), comboCategoria.valueProperty(),
                comboProveedor.valueProperty());
        btnAgregar.disableProperty().bind(sinSeleccion.not());
        btnActualizar.disableProperty().bind(sinSeleccion.or(sinCambios));
        btnEliminar.disableProperty().bind(sinSeleccion);
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        lblAlerta.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        lblAlerta.setVisible(false);
        lblAlerta.setManaged(false); // oculto y sin ocupar espacio mientras no haya alertas

        HBox fila1 = new HBox(10, campoNombre, campoPrecio, campoStockMinimo);
        HBox fila2 = new HBox(10, comboCategoria, comboProveedor);
        HBox botones = new HBox(10, btnAgregar, btnActualizar, btnEliminar, btnLimpiar);

        getChildren().addAll(lblAlerta, fila1, fila2, botones, tabla);
    }

    private void seleccionarCategoria(int categoriaId) {
        for (Categoria c : categorias) {
            if (c.getId() == categoriaId) {
                comboCategoria.setValue(c);
                return;
            }
        }
        comboCategoria.setValue(null);
    }

    private void seleccionarProveedor(Integer proveedorId) {
        if (proveedorId != null) {
            for (Proveedor p : proveedores) {
                if (p.getId() == proveedorId) {
                    comboProveedor.setValue(p);
                    return;
                }
            }
        }
        comboProveedor.setValue(SIN_PROVEEDOR);
    }

    private boolean hayCambios() {
        Producto seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return false;
        }
        Categoria categoria = comboCategoria.getValue();
        if (categoria == null || categoria.getId() != seleccionado.getCategoriaId()) {
            return true;
        }
        int proveedorElegido = comboProveedor.getValue() == null ? 0 : comboProveedor.getValue().getId();
        int proveedorActual = seleccionado.getProveedorId() == null ? 0 : seleccionado.getProveedorId();
        if (proveedorElegido != proveedorActual) {
            return true;
        }
        if (!campoNombre.getText().trim().equals(seleccionado.getNombre())) {
            return true;
        }
        try {
            double precio = Double.parseDouble(campoPrecio.getText().trim().replace(',', '.'));
            int stockMinimo = Integer.parseInt(campoStockMinimo.getText().trim());
            return precio != seleccionado.getPrecio() || stockMinimo != seleccionado.getStockMinimo();
        } catch (NumberFormatException e) {
            return true; // si lo escrito no es un número válido, cuenta como cambio
        }
    }

    private void pintar(TableRow<Producto> fila) {
        Producto p = fila.getItem();
        boolean bajo = p != null && p.tieneStockBajo();
        fila.setStyle(bajo && !fila.isSelected() ? "-fx-background-color: #ffe0e0;" : "");
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public void setProductos(List<Producto> lista) { productos.setAll(lista); }
    public void setCategorias(List<Categoria> lista) { categorias.setAll(lista); }

    public void setProveedores(List<Proveedor> lista) {
        List<Proveedor> opciones = new ArrayList<>();
        opciones.add(SIN_PROVEEDOR);
        opciones.addAll(lista);
        proveedores.setAll(opciones);
        if (comboProveedor.getValue() == null) {
            comboProveedor.setValue(SIN_PROVEEDOR);
        }
    }

    public void setAlertaStockBajo(long cantidad) {
        boolean hayAlerta = cantidad > 0;
        lblAlerta.setText(hayAlerta
                ? "Atención: " + cantidad + " producto(s) con stock bajo (resaltados en rojo)."
                : "");
        lblAlerta.setVisible(hayAlerta);
        lblAlerta.setManaged(hayAlerta);
    }

    public String getNombreIngresado() { return campoNombre.getText(); }
    public String getPrecioIngresado() { return campoPrecio.getText(); }
    public String getStockMinimoIngresado() { return campoStockMinimo.getText(); }
    public Categoria getCategoriaElegida() { return comboCategoria.getValue(); }
    public Producto getSeleccionado() { return tabla.getSelectionModel().getSelectedItem(); }

    // El controlador solo ve un id o null; no sabe nada de la opción "Sin proveedor"
    public Integer getProveedorIdElegido() {
        Proveedor elegido = comboProveedor.getValue();
        return (elegido == null || elegido == SIN_PROVEEDOR) ? null : elegido.getId();
    }

    public void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        campoNombre.clear();
        campoPrecio.clear();
        campoStockMinimo.clear();
        comboCategoria.setValue(null);
        comboProveedor.setValue(SIN_PROVEEDOR);
    }

    public void setOnAgregar(Runnable accion) { btnAgregar.setOnAction(e -> accion.run()); }
    public void setOnActualizar(Runnable accion) { btnActualizar.setOnAction(e -> accion.run()); }
    public void setOnEliminar(Runnable accion) { btnEliminar.setOnAction(e -> accion.run()); }
}
