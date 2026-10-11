package com.inventario.vista;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// Se muestra solo la primera vez, cuando todavía no existe el administrador
public class ConfiguracionInicialView extends VBox {

    private final TextField campoUsuario = new TextField();
    private final PasswordField campoContrasena = new PasswordField();
    private final PasswordField campoRepetir = new PasswordField();
    private final Button btnCrear = new Button("Crear administrador");

    public ConfiguracionInicialView() {
        super(10);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        Label titulo = new Label("Bienvenido");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label explicacion = new Label(
                "Es la primera vez que se usa la aplicación.\n"
                + "Crea la cuenta de administrador para empezar.");
        explicacion.setWrapText(true);
        Label reglas = new Label("Contraseña: al menos 8 caracteres. Mejor una frase larga que una palabra rara.");
        reglas.setWrapText(true);
        reglas.setStyle("-fx-text-fill: gray;");
        Label reglasUsuario = new Label(
                "El usuario debe tener de 3 a 30 caracteres y empezar con una letra.\n"
                + "Puede contener letras sin tilde, números, punto (.) y guion bajo (_).\n"
                + "No puede tener espacios: usa un punto o un guion bajo en su lugar.\n"
                + "Ejemplos: ana, ana.gimenez, ana_g");
        reglasUsuario.setWrapText(true);
        reglasUsuario.setStyle("-fx-text-fill: gray;");

        campoUsuario.setPromptText("Nombre de usuario");
        campoContrasena.setPromptText("Contraseña");
        campoRepetir.setPromptText("Repetir contraseña");
        btnCrear.setDefaultButton(true);
        btnCrear.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(titulo, explicacion, campoUsuario, reglasUsuario,
                campoContrasena, campoRepetir, reglas, btnCrear);
    }

    // --- Lo que el controlador puede pedirle a la vista ---

    public String getUsuarioIngresado() { return campoUsuario.getText(); }
    public String getContrasenaIngresada() { return campoContrasena.getText(); }
    public String getRepeticionIngresada() { return campoRepetir.getText(); }

    // Tras un error: borra las dos contraseñas (por seguridad) y vuelve a la primera
    public void limpiarContrasenas() {
        campoContrasena.clear();
        campoRepetir.clear();
        campoContrasena.requestFocus();
    }

    // Tras un error en el usuario: borra solo ese campo y deja el cursor ahí
    public void limpiarUsuario() {
        campoUsuario.clear();
        campoUsuario.requestFocus();
    }

    public void setOnCrear(Runnable accion) {
        btnCrear.setOnAction(e -> accion.run());
    }
}
