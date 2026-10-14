package com.inventario.vista;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

// Campo de contraseña con un botón para mostrar u ocultar lo escrito.
// Hay dos campos que comparten el mismo texto; solo cambia cuál se ve.
public class CampoContrasena extends HBox {

    private final PasswordField oculto = new PasswordField();
    private final TextField visible = new TextField();
    private final ToggleButton btnMostrar = new ToggleButton("Mostrar");
    private final boolean conBoton;

    public CampoContrasena(String textoGuia) {
        this(textoGuia, true);
    }

    public CampoContrasena(String textoGuia, boolean conBoton) {
        super(5);
        this.conBoton = conBoton;
        oculto.setPromptText(textoGuia);
        visible.setPromptText(textoGuia);
        visible.textProperty().bindBidirectional(oculto.textProperty()); // escribir en uno = escribir en el otro

        // Se ve uno solo a la vez, y el oculto no ocupa lugar
        visible.visibleProperty().bind(btnMostrar.selectedProperty());
        visible.managedProperty().bind(btnMostrar.selectedProperty());
        oculto.visibleProperty().bind(btnMostrar.selectedProperty().not());
        oculto.managedProperty().bind(btnMostrar.selectedProperty().not());

        btnMostrar.selectedProperty().addListener((obs, antes, mostrar) -> {
            btnMostrar.setText(mostrar ? "Ocultar" : "Mostrar");
            if (conBoton) {
                enfocar(); // seguir escribiendo sin tener que volver a hacer clic en el campo
            }
        });
        btnMostrar.setFocusTraversable(false); // con Tab se pasa al siguiente campo, no al botón

        oculto.setMaxWidth(Double.MAX_VALUE);
        visible.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(oculto, Priority.ALWAYS);
        HBox.setHgrow(visible, Priority.ALWAYS);
        getChildren().addAll(oculto, visible);
        if (conBoton) {
            getChildren().add(btnMostrar);
        }
    }

    // Permite que un control externo (por ejemplo, una casilla) muestre u oculte este campo
    public BooleanProperty mostrarProperty() { return btnMostrar.selectedProperty(); }

    public String getText() { return oculto.getText(); }

    // Para que la vista pueda reaccionar a lo que se escribe (por ejemplo, activar un botón)
    public StringProperty textProperty() { return oculto.textProperty(); }

    // Borra y vuelve a ocultar: tras un error, la contraseña no queda a la vista
    public void limpiar() {
        oculto.clear();
        btnMostrar.setSelected(false);
    }

    // Deja el cursor al final del campo que se está viendo
    public void enfocar() {
        TextField activo = btnMostrar.isSelected() ? visible : oculto;
        activo.requestFocus();
        activo.positionCaret(activo.getText().length());
    }
}
