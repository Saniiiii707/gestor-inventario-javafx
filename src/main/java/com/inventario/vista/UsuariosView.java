package com.inventario.vista;

import com.inventario.modelo.Rol;
import com.inventario.modelo.Usuario;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Modo "nuevo" (sin fila elegida): crear un usuario.
// Modo "edición" (con fila elegida): cambiar rol, restablecer contraseña, desactivar o reactivar.
public class UsuariosView extends VBox {

    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final TableView<Usuario> tabla = new TableView<>(usuarios);

    private final TextField campoUsuario = new TextField();
    private final ComboBox<Rol> comboRol = new ComboBox<>(FXCollections.observableArrayList(Rol.values()));
    private final CampoContrasena campoContrasena = new CampoContrasena("Contraseña", false);
    private final CampoContrasena campoRepetir = new CampoContrasena("Repetir contraseña", false);
    private final CheckBox chkMostrar = new CheckBox("Mostrar contraseñas");

    private final Button btnCrear = new Button("Crear usuario");
    private final Button btnCambiarRol = new Button("Cambiar rol");
    private final Button btnRestablecer = new Button("Restablecer contraseña");
    private final Button btnDesactivar = new Button("Desactivar");
    private final Button btnReactivar = new Button("Reactivar");
    private final Button btnLimpiar = new Button("Limpiar");

    private int idSesion;   // quien inició sesión: no puede modificarse a sí mismo

    public UsuariosView() {
        super(10);
        setPadding(new Insets(15));

        // --- Tabla ---
        TableColumn<Usuario, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Usuario, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsuario.setPrefWidth(200);

        TableColumn<Usuario, Rol> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colRol.setPrefWidth(110);

        // Texto legible en lugar de true/false
        TableColumn<Usuario, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setPrefWidth(100);

        // De a una (con add y no addAll): así no aparece el aviso "unchecked"
        tabla.getColumns().add(colId);
        tabla.getColumns().add(colUsuario);
        tabla.getColumns().add(colRol);
        tabla.getColumns().add(colEstado);

        // Al elegir una fila, el formulario muestra a ese usuario
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, elegido) -> {
            if (elegido != null) {
                campoUsuario.setText(elegido.getUsername());
                comboRol.setValue(elegido.getRol());
                campoContrasena.limpiar();
                campoRepetir.limpiar();
            }
        });

        // --- Formulario ---
        campoUsuario.setPromptText("Usuario");
        comboRol.setPromptText("Rol");

        // Una sola casilla muestra u oculta las dos contraseñas.
        // bindBidirectional (y no bind) porque limpiar() también cambia ese valor.
        campoContrasena.mostrarProperty().bindBidirectional(chkMostrar.selectedProperty());
        campoRepetir.mostrarProperty().bindBidirectional(chkMostrar.selectedProperty());

        // --- Qué se puede usar en cada modo ---
        BooleanBinding sinSeleccion = tabla.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding elegidoActivo = Bindings.createBooleanBinding(
                () -> getSeleccionado() != null && getSeleccionado().isActivo(),
                tabla.getSelectionModel().selectedItemProperty());
        BooleanBinding elegidoInactivo = Bindings.createBooleanBinding(
                () -> getSeleccionado() != null && !getSeleccionado().isActivo(),
                tabla.getSelectionModel().selectedItemProperty());
        BooleanBinding rolSinCambios = Bindings.createBooleanBinding(
                () -> getSeleccionado() == null || comboRol.getValue() == getSeleccionado().getRol(),
                tabla.getSelectionModel().selectedItemProperty(), comboRol.valueProperty());
        // idSesion se fija una vez, antes de que haya selección: basta con observar la selección
        BooleanBinding esPropio = Bindings.createBooleanBinding(
                () -> getSeleccionado() != null && getSeleccionado().getId() == idSesion,
                tabla.getSelectionModel().selectedItemProperty());

        campoUsuario.disableProperty().bind(sinSeleccion.not());   // el nombre de usuario no se cambia
        btnCrear.disableProperty().bind(sinSeleccion.not());
        btnCambiarRol.disableProperty().bind(rolSinCambios.or(esPropio));
        btnRestablecer.disableProperty().bind(sinSeleccion.or(esPropio)
                .or(campoContrasena.textProperty().isEmpty()));
        btnDesactivar.disableProperty().bind(elegidoActivo.not().or(esPropio));
        btnReactivar.disableProperty().bind(elegidoInactivo.not().or(esPropio));
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        HBox fila1 = new HBox(10, campoUsuario, comboRol);
        HBox fila2 = new HBox(10, campoContrasena, campoRepetir, chkMostrar);
        fila2.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campoContrasena, Priority.ALWAYS);
        HBox.setHgrow(campoRepetir, Priority.ALWAYS);
        HBox botones = new HBox(10, btnCrear, btnCambiarRol, btnRestablecer,
                btnDesactivar, btnReactivar, btnLimpiar);

        getChildren().addAll(
                fila1,
                fila2,
                botones,
                tabla);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public void setIdSesion(int id) { idSesion = id; }
    public void setUsuarios(List<Usuario> lista) { usuarios.setAll(lista); }

    public Usuario getSeleccionado() { return tabla.getSelectionModel().getSelectedItem(); }
    public String getUsuarioIngresado() { return campoUsuario.getText(); }
    public Rol getRolElegido() { return comboRol.getValue(); }
    public String getContrasenaIngresada() { return campoContrasena.getText(); }
    public String getRepeticionIngresada() { return campoRepetir.getText(); }

    public void limpiarUsuario() {
        campoUsuario.clear();
        campoUsuario.requestFocus();
    }

    public void limpiarContrasenas() {
        campoContrasena.limpiar();
        campoRepetir.limpiar();
        campoContrasena.enfocar();
    }

    // Vuelve al modo "nuevo"
    public void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();   // primero, para que el campo usuario se reactive
        campoUsuario.clear();
        comboRol.setValue(null);
        campoContrasena.limpiar();
        campoRepetir.limpiar();
    }

    public void setOnCrear(Runnable accion) { btnCrear.setOnAction(e -> accion.run()); }
    public void setOnCambiarRol(Runnable accion) { btnCambiarRol.setOnAction(e -> accion.run()); }
    public void setOnRestablecer(Runnable accion) { btnRestablecer.setOnAction(e -> accion.run()); }
    public void setOnDesactivar(Runnable accion) { btnDesactivar.setOnAction(e -> accion.run()); }
    public void setOnReactivar(Runnable accion) { btnReactivar.setOnAction(e -> accion.run()); }
}
