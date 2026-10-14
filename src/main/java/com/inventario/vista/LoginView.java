package com.inventario.vista;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {

    private static final double ANCHO_FORMULARIO = 360;

    private final TextField campoUsuario = new TextField();
    private final CampoContrasena campoContrasena = new CampoContrasena("Contraseña");
    private final Button btnEntrar = new Button("Entrar");

    public LoginView() {
        super(10);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        Label titulo = new Label("Gestor de inventario");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitulo = new Label("Inicia sesión para continuar");

        campoUsuario.setPromptText("Usuario");
        btnEntrar.setDefaultButton(true);           // Enter equivale a hacer clic en Entrar
        btnEntrar.setMaxWidth(Double.MAX_VALUE);    // botón del ancho del formulario

        // El formulario tiene un ancho máximo y queda centrado, aunque la ventana se maximice
        VBox contenido = new VBox(10);
        contenido.setAlignment(Pos.CENTER);
        contenido.setMaxWidth(ANCHO_FORMULARIO);
        contenido.getChildren().addAll(titulo, subtitulo, campoUsuario, campoContrasena, btnEntrar);
        getChildren().add(contenido);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public String getUsuarioIngresado() { return campoUsuario.getText(); }
    public String getContrasenaIngresada() { return campoContrasena.getText(); }

    // Tras un intento fallido: borra solo la contraseña y deja el cursor ahí
    public void limpiarContrasena() {
        campoContrasena.limpiar();
        campoContrasena.enfocar();
    }

    public void setOnEntrar(Runnable accion) {
        btnEntrar.setOnAction(e -> accion.run());
    }
}
