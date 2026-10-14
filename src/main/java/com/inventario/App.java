package com.inventario;

import com.inventario.controlador.CambiarContrasenaController;
import com.inventario.controlador.CategoriasController;
import com.inventario.controlador.ConfiguracionInicialController;
import com.inventario.controlador.LoginController;
import com.inventario.controlador.MovimientosController;
import com.inventario.controlador.ProductosController;
import com.inventario.controlador.ProveedoresController;
import com.inventario.controlador.UsuariosController;
import com.inventario.dao.Database;
import com.inventario.dao.UsuarioDAO;
import com.inventario.modelo.Usuario;
import com.inventario.vista.CambiarContrasenaView;
import com.inventario.vista.CategoriasView;
import com.inventario.vista.ConfiguracionInicialView;
import com.inventario.vista.LoginView;
import com.inventario.vista.Mensajes;
import com.inventario.vista.MovimientosView;
import com.inventario.vista.ProductosView;
import com.inventario.vista.ProveedoresView;
import com.inventario.vista.UsuariosView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        new LoginController(vista, this::alIniciarSesion);
        stage.setScene(new Scene(vista, 340, 260));
        stage.setTitle("Gestor de inventario");
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    // Si un administrador eligió su contraseña, primero tiene que cambiarla
    private void alIniciarSesion(Usuario usuario) {
        if (new UsuarioDAO().debeCambiarContrasena(usuario.getId())) {
            mostrarCambioContrasena(usuario, true);
        } else {
            mostrarVentanaPrincipal(usuario);
        }
    }

    private void mostrarCambioContrasena(Usuario usuario, boolean obligatorio) {
        Scene anterior = stage.getScene();   // en modo voluntario, se vuelve exactamente a donde estaba
        Runnable alTerminar = obligatorio ? () -> mostrarVentanaPrincipal(usuario) : () -> volverA(anterior);
        Runnable alCancelar = obligatorio ? this::mostrarLogin : () -> volverA(anterior);

        CambiarContrasenaView vista = new CambiarContrasenaView();
        new CambiarContrasenaController(vista, usuario, obligatorio, alTerminar, alCancelar);
        stage.setScene(new Scene(vista, 480, 500));
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private void volverA(Scene escena) {
        stage.setScene(escena);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    // Se ejecuta cuando alguien inicia sesión o termina de crear el administrador
    private void mostrarVentanaPrincipal(Usuario usuario) {
        TabPane pestanas = new TabPane();

        // Categorías y Proveedores: solo quien puede editar el catálogo.
        // Para el resto ni siquiera se crean, así que no hay forma de llegar a ellas.
        if (usuario.puedeEditarCatalogo()) {
            CategoriasView categoriasView = new CategoriasView();
            ProveedoresView proveedoresView = new ProveedoresView();
            new CategoriasController(categoriasView, usuario);
            new ProveedoresController(proveedoresView, usuario);

            Tab tabCategorias = new Tab("Categorías", categoriasView);
            Tab tabProveedores = new Tab("Proveedores", proveedoresView);
            tabCategorias.setClosable(false);
            tabProveedores.setClosable(false);
            pestanas.getTabs().addAll(tabCategorias, tabProveedores);
        }

        // Productos y Movimientos: todos
        ProductosView productosView = new ProductosView();
        MovimientosView movimientosView = new MovimientosView();
        ProductosController productosController = new ProductosController(productosView, usuario);
        MovimientosController movimientosController = new MovimientosController(movimientosView, usuario);

        Tab tabProductos = new Tab("Productos", productosView);
        Tab tabMovimientos = new Tab("Movimientos", movimientosView);
        tabProductos.setClosable(false);
        tabMovimientos.setClosable(false);
        pestanas.getTabs().addAll(tabProductos, tabMovimientos);
        recargarAlEntrar(tabProductos, productosController::recargar);
        recargarAlEntrar(tabMovimientos, movimientosController::alEntrar);

        // Usuarios: solo quien puede gestionarlos. Para el resto, la pestaña ni siquiera existe.
        if (usuario.puedeGestionarUsuarios()) {
            UsuariosView usuariosView = new UsuariosView();
            UsuariosController usuariosController = new UsuariosController(usuariosView, usuario);
            Tab tabUsuarios = new Tab("Usuarios", usuariosView);
            tabUsuarios.setClosable(false);
            pestanas.getTabs().add(tabUsuarios);
            recargarAlEntrar(tabUsuarios, usuariosController::recargar);
        }

        BorderPane raiz = new BorderPane(pestanas);   // las pestañas en el centro
        raiz.setTop(crearBarraSesion(usuario));       // la barra arriba
        stage.setScene(new Scene(raiz, 820, 520));
        stage.setTitle("Gestor de inventario");
        stage.sizeToScene();   // la ventana toma el tamaño de la nueva escena
        stage.centerOnScreen();
    }

    // Barra superior: quién está trabajando y las acciones de la sesión
    private HBox crearBarraSesion(Usuario usuario) {
        Label lblSesion = new Label("Sesión: " + usuario.getUsername() + " (" + usuario.getRol() + ")");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);   // empuja los botones hacia la derecha

        Button btnCambiar = new Button("Cambiar mi contraseña");
        btnCambiar.setOnAction(e -> mostrarCambioContrasena(usuario, false));

        Button btnCerrar = new Button("Cerrar sesión");
        btnCerrar.setOnAction(e -> {
            if (Mensajes.confirmar("¿Cerrar la sesión de \"" + usuario.getUsername() + "\"?")) {
                mostrarLogin();
            }
        });

        HBox barra = new HBox(10, lblSesion, espacio, btnCambiar, btnCerrar);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(8, 15, 8, 15));
        barra.setStyle("-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");   // línea debajo
        return barra;
    }

    // Al entrar a una pestaña, se recargan sus datos por si cambiaron en otra
    private void recargarAlEntrar(Tab tab, Runnable recarga) {
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                recarga.run();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
