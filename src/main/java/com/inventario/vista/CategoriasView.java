package com.inventario.vista;

import com.inventario.modelo.Categoria;
import java.util.List;
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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;

public class CategoriasView extends VBox {

    private final ObservableList<Categoria> datos = FXCollections.observableArrayList();
    private final TableView<Categoria> tabla = new TableView<>(datos);
    private final TextField campoNombre = new TextField();
    private final Button btnAgregar = new Button("Agregar");
    private final Button btnActualizar = new Button("Actualizar");
    private final Button btnEliminar = new Button("Eliminar");
    private final Button btnLimpiar = new Button("Limpiar");

    public CategoriasView() {
        super(10);
        setPadding(new Insets(15));

        TableColumn<Categoria, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<Categoria, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(300);

        tabla.getColumns().addAll(colId, colNombre);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                campoNombre.setText(nueva.getNombre());
            }
        });

        campoNombre.setPromptText("Nombre de la categoría");
        BooleanBinding sinSeleccion = tabla.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding sinCambios = Bindings.createBooleanBinding(
                () -> !hayCambios(),
                tabla.getSelectionModel().selectedItemProperty(), campoNombre.textProperty());
        btnAgregar.disableProperty().bind(sinSeleccion.not());
        btnActualizar.disableProperty().bind(sinSeleccion.or(sinCambios));
        btnEliminar.disableProperty().bind(sinSeleccion);
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        HBox formulario = new HBox(10, campoNombre, btnAgregar, btnActualizar, btnEliminar, btnLimpiar);
        getChildren().addAll(formulario, tabla);
    }

    // Solo tiene sentido actualizar si hay fila elegida y el nombre cambió
    private boolean hayCambios() {
        Categoria seleccionada = tabla.getSelectionModel().getSelectedItem();
        return seleccionada != null
                && !campoNombre.getText().trim().equals(seleccionada.getNombre());
    }

    public void setCategorias(List<Categoria> categorias) {
        datos.setAll(categorias);
    }

    public String getNombreIngresado() {
        return campoNombre.getText();
    }

    public Categoria getSeleccionada() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    public void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        campoNombre.clear();
    }

    public void setOnAgregar(Runnable accion) {
        btnAgregar.setOnAction(e -> accion.run());
    }

    public void setOnActualizar(Runnable accion) {
        btnActualizar.setOnAction(e -> accion.run());
    }

    public void setOnEliminar(Runnable accion) {
        btnEliminar.setOnAction(e -> accion.run());
    }
}