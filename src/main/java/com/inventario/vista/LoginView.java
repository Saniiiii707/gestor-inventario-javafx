package com.inventario.vista;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {

    private final TextField campoUsuario = new TextField();
    private final PasswordField campoContrasena = new PasswordField(); // muestra puntos, no el texto
    private final Button btnEntrar = new Button("Entrar");

    public LoginView() {
        super(10);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        Label titulo = new Label("Gestor de inventario");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitulo = new Label("Inicia sesión para continuar");

        campoUsuario.setPromptText("Usuario");
        campoContrasena.setPromptText("Contraseña");
        btnEntrar.setDefaultButton(true);           // Enter equivale a hacer clic en Entrar
        btnEntrar.setMaxWidth(Double.MAX_VALUE);    // botón del ancho del formulario

        getChildren().addAll(titulo, subtitulo, campoUsuario, campoContrasena, btnEntrar);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public String getUsuarioIngresado() { return campoUsuario.getText(); }
    public String getContrasenaIngresada() { return campoContrasena.getText(); }

    // Tras un intento fallido: borra solo la contraseña y deja el cursor ahí
    public void limpiarContrasena() {
        campoContrasena.clear();
        campoContrasena.requestFocus();
    }

    public void setOnEntrar(Runnable accion) {
        btnEntrar.setOnAction(e -> accion.run());
    }
}
