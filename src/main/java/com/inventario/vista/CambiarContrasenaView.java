package com.inventario.vista;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// Pantalla para cambiar la propia contraseña.
// Modo obligatorio: después de que un administrador la creó o la restableció (no se puede saltar).
// Modo voluntario: desde la ventana principal (se puede cancelar).
public class CambiarContrasenaView extends VBox {

    private static final double ANCHO_FORMULARIO = 420;

    private final Label titulo = new Label();
    private final Label explicacion = new Label();
    private final CampoContrasena campoActual = new CampoContrasena("Contraseña actual");
    private final CampoContrasena campoNueva = new CampoContrasena("Nueva contraseña", false);
    private final CampoContrasena campoRepetir = new CampoContrasena("Repetir nueva contraseña", false);
    private final CheckBox chkMostrar = new CheckBox("Mostrar contraseñas nuevas");
    private final Button btnCambiar = new Button("Cambiar contraseña");
    private final Button btnCancelar = new Button();

    public CambiarContrasenaView() {
        super(10);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        explicacion.setWrapText(true);
        explicacion.setMinHeight(Region.USE_PREF_SIZE);

        // Una casilla para las dos contraseñas nuevas; la actual tiene su propio botón
        campoNueva.mostrarProperty().bindBidirectional(chkMostrar.selectedProperty());
        campoRepetir.mostrarProperty().bindBidirectional(chkMostrar.selectedProperty());

        btnCambiar.setDefaultButton(true);     // Enter = Cambiar
        btnCambiar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.setMaxWidth(Double.MAX_VALUE);

        // El formulario tiene un ancho máximo y queda centrado, aunque la ventana se maximice
        VBox contenido = new VBox(10, titulo, explicacion, campoActual, campoNueva, campoRepetir,
                chkMostrar, btnCambiar, btnCancelar);
        contenido.setAlignment(Pos.CENTER);
        contenido.setMaxWidth(ANCHO_FORMULARIO);
        getChildren().add(contenido);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public void setModoObligatorio(boolean obligatorio) {
        // En el modo obligatorio, Esc no cierra la sesión: una tecla sin querer no
        // debe sacar a nadie de la app
        btnCancelar.setCancelButton(!obligatorio);
        if (obligatorio) {
            titulo.setText("Elige una contraseña nueva");
            explicacion.setText("Tu contraseña la asignó un administrador, así que también la conoce. "
                    + "Antes de continuar, escribe la que te dio como contraseña actual y elige una que solo sepas tú.");
            btnCancelar.setText("Cerrar sesión");
        } else {
            titulo.setText("Cambiar mi contraseña");
            explicacion.setText("Escribe tu contraseña actual y elige una nueva.");
            btnCancelar.setText("Cancelar");
        }
    }

    public String getActual() { return campoActual.getText(); }
    public String getNueva() { return campoNueva.getText(); }
    public String getRepeticion() { return campoRepetir.getText(); }

    public void limpiarActual() {
        campoActual.limpiar();
        campoActual.enfocar();
    }

    public void limpiarNuevas() {
        campoNueva.limpiar();
        campoRepetir.limpiar();
        campoNueva.enfocar();
    }

    public void setOnCambiar(Runnable accion) { btnCambiar.setOnAction(e -> accion.run()); }
    public void setOnCancelar(Runnable accion) { btnCancelar.setOnAction(e -> accion.run()); }
}
