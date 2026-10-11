package com.inventario.vista;

import com.inventario.modelo.Proveedor;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProveedoresView extends VBox {

    private final ObservableList<Proveedor> datos = FXCollections.observableArrayList();
    private final TableView<Proveedor> tabla = new TableView<>(datos);
    private final TextField campoNombre = new TextField();
    private final TextField campoTelefono = new TextField();
    private final Button btnAgregar = new Button("Agregar");
    private final Button btnActualizar = new Button("Actualizar");
    private final Button btnEliminar = new Button("Eliminar");
    private final Button btnLimpiar = new Button("Limpiar");

    public ProveedoresView() {
        super(10);
        setPadding(new Insets(15));

        TableColumn<Proveedor, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<Proveedor, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(300);

        TableColumn<Proveedor, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colTelefono.setPrefWidth(180);

        tabla.getColumns().addAll(colId, colNombre, colTelefono);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                campoNombre.setText(nuevo.getNombre());
                campoTelefono.setText(nuevo.getTelefono() == null ? "" : nuevo.getTelefono());
            }
        });

        campoNombre.setPromptText("Nombre del proveedor");
        campoTelefono.setPromptText("Teléfono (opcional)");

        // Modo "nuevo" (sin fila elegida) o modo "edición" (con fila elegida)
        BooleanBinding sinSeleccion = tabla.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding sinCambios = Bindings.createBooleanBinding(
                () -> !hayCambios(),
                tabla.getSelectionModel().selectedItemProperty(),
                campoNombre.textProperty(), campoTelefono.textProperty());
        btnAgregar.disableProperty().bind(sinSeleccion.not());
        btnActualizar.disableProperty().bind(sinSeleccion.or(sinCambios));
        btnEliminar.disableProperty().bind(sinSeleccion);
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        HBox campos = new HBox(10, campoNombre, campoTelefono);
        HBox botones = new HBox(10, btnAgregar, btnActualizar, btnEliminar, btnLimpiar);
        getChildren().addAll(campos, botones, tabla);
    }

    // Solo tiene sentido actualizar si hay fila elegida y algo cambió
    private boolean hayCambios() {
        Proveedor seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return false;
        }
        String telefonoActual = seleccionado.getTelefono() == null ? "" : seleccionado.getTelefono();
        return !campoNombre.getText().trim().equals(seleccionado.getNombre())
                || !campoTelefono.getText().trim().equals(telefonoActual);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public void setProveedores(List<Proveedor> lista) { datos.setAll(lista); }

    public String getNombreIngresado() { return campoNombre.getText(); }
    public String getTelefonoIngresado() { return campoTelefono.getText(); }
    public Proveedor getSeleccionado() { return tabla.getSelectionModel().getSelectedItem(); }

    public void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        campoNombre.clear();
        campoTelefono.clear();
    }

    public void setOnAgregar(Runnable accion) { btnAgregar.setOnAction(e -> accion.run()); }
    public void setOnActualizar(Runnable accion) { btnActualizar.setOnAction(e -> accion.run()); }
    public void setOnEliminar(Runnable accion) { btnEliminar.setOnAction(e -> accion.run()); }
}
