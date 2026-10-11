package com.inventario;

import com.inventario.controlador.CategoriasController;
import com.inventario.controlador.ConfiguracionInicialController;
import com.inventario.controlador.LoginController;
import com.inventario.controlador.MovimientosController;
import com.inventario.controlador.ProductosController;
import com.inventario.controlador.ProveedoresController;
import com.inventario.dao.Database;
import com.inventario.dao.UsuarioDAO;
import com.inventario.modelo.Usuario;
import com.inventario.vista.CategoriasView;
import com.inventario.vista.ConfiguracionInicialView;
import com.inventario.vista.LoginView;
import com.inventario.vista.MovimientosView;
import com.inventario.vista.ProductosView;
import com.inventario.vista.ProveedoresView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class App extends Application {

    private Stage stage; // una sola ventana: se cambia su contenido (escena) según la etapa

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Database.inicializar();
        stage.setTitle("Gestor de inventario");

        // Primera vez: crear el administrador. Si no, iniciar sesión.
        if (new UsuarioDAO().necesitaConfiguracionInicial()) {
            mostrarConfiguracionInicial();
        } else {
            mostrarLogin();
        }
        stage.show();
    }

    private void mostrarConfiguracionInicial() {
        ConfiguracionInicialView vista = new ConfiguracionInicialView();
        new ConfiguracionInicialController(vista, this::mostrarVentanaPrincipal);
        stage.setScene(new Scene(vista, 380, 520));
    }

    private void mostrarLogin() {
        LoginView vista = new LoginView();
        new LoginController(vista, this::mostrarVentanaPrincipal);
        stage.setScene(new Scene(vista, 340, 260));
    }

    // Se ejecuta cuando alguien inicia sesión o termina de crear el administrador
    private void mostrarVentanaPrincipal(Usuario usuario) {
        // Vistas
        CategoriasView categoriasView = new CategoriasView();
        ProductosView productosView = new ProductosView();
        MovimientosView movimientosView = new MovimientosView();
        ProveedoresView proveedoresView = new ProveedoresView();

        // Controladores
        new CategoriasController(categoriasView);
        new ProveedoresController(proveedoresView);
        ProductosController productosController = new ProductosController(productosView);
        MovimientosController movimientosController = new MovimientosController(movimientosView, usuario);

        Tab tabCategorias = new Tab("Categorías", categoriasView);
        Tab tabProveedores = new Tab("Proveedores", proveedoresView);
        Tab tabProductos = new Tab("Productos", productosView);
        Tab tabMovimientos = new Tab("Movimientos", movimientosView);
        tabCategorias.setClosable(false);
        tabProveedores.setClosable(false);
        tabProductos.setClosable(false);
        tabMovimientos.setClosable(false);

        TabPane pestanas = new TabPane(tabCategorias, tabProveedores, tabProductos, tabMovimientos);

        // Al entrar a una pestaña, se recargan sus datos por si cambiaron en otra
        pestanas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual == tabProductos) {
                productosController.recargar();
            } else if (actual == tabMovimientos) {
                movimientosController.alEntrar();
            }
        });

        stage.setScene(new Scene(pestanas, 820, 480));
        stage.setTitle("Gestor de inventario — " + usuario.getUsername() + " (" + usuario.getRol() + ")");
        stage.sizeToScene();   // la ventana toma el tamaño de la nueva escena
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
