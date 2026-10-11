package com.inventario.dao;

import com.inventario.util.Validaciones;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Database{
    private static final String URL = "jdbc:sqlite:inventario.db?foreign_keys=on";

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL);
    }

    public static void inicializar(){
        List<String> tablas = List.of(
        """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                rol TEXT NOT NULL CHECK (rol IN ('ADMIN', 'VENDEDOR')),
                activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1)),
                intentos_fallidos INTEGER NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
                bloqueado_hasta TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                nombre_normalizado TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS proveedores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                telefono TEXT,
                nombre_normalizado TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS productos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                precio REAL NOT NULL CHECK (precio >= 0),
                stock_actual INTEGER NOT NULL DEFAULT 0,
                stock_minimo INTEGER NOT NULL DEFAULT 5,
                categoria_id INTEGER NOT NULL,
                proveedor_id INTEGER,
                FOREIGN KEY (categoria_id) REFERENCES categorias(id),
                FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS movimientos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                producto_id INTEGER NOT NULL,
                usuario_id INTEGER NOT NULL,
                tipo TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
                cantidad INTEGER NOT NULL,
                fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                descripcion TEXT,
                stock_anterior INTEGER,
                stock_resultante INTEGER,
                FOREIGN KEY (producto_id) REFERENCES productos(id),
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
            )
            """
        );

        try (Connection conn = getConnection();
            Statement stmt = conn.createStatement()){
            for (String sql : tablas){
                stmt.execute(sql);
            }

            agregarColumnaSiFalta(conn, "movimientos", "stock_anterior", "INTEGER");
            agregarColumnaSiFalta(conn, "movimientos", "stock_resultante", "INTEGER");

            // Login: estado de la cuenta y bloqueo temporal tras intentos fallidos
            agregarColumnaSiFalta(conn, "usuarios", "activo",
                    "INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1))");
            agregarColumnaSiFalta(conn, "usuarios", "intentos_fallidos",
                    "INTEGER NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0)");
            agregarColumnaSiFalta(conn, "usuarios", "bloqueado_hasta", "TEXT");

            stmt.execute("DROP INDEX IF EXISTS idx_proveedores_nombre");
            migrarNombreNormalizado(conn, "categorias");
            migrarNombreNormalizado(conn, "proveedores");
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (id, username, password_hash, rol)
                VALUES (1, 'admin', 'provisorio', 'ADMIN')
                """);
            System.out.println("Base de datos lista.");
        }catch(SQLException e){
            throw new RuntimeException("Error al crear las tablas", e);
        }

    }


    private static boolean columnaExiste(Connection conn, String tabla, String columna) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(columna)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void agregarColumnaSiFalta(Connection conn, String tabla, String columna,
                                              String tipo) throws SQLException {
        if (!columnaExiste(conn, tabla, columna)) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + tipo);
            }
        }
    }


    private static void migrarNombreNormalizado(Connection conn, String tabla) throws SQLException {
        agregarColumnaSiFalta(conn, tabla, "nombre_normalizado", "TEXT");


        Map<Integer, String> pendientes = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, nombre FROM " + tabla + " WHERE nombre_normalizado IS NULL");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pendientes.put(rs.getInt("id"), rs.getString("nombre"));
            }
        }


        if (!pendientes.isEmpty()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + tabla + " SET nombre_normalizado = ? WHERE id = ?")) {
                for (Map.Entry<Integer, String> fila : pendientes.entrySet()) {
                    ps.setString(1, Validaciones.normalizarNombre(fila.getValue()));
                    ps.setInt(2, fila.getKey());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        try (Statement st = conn.createStatement()) {
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_" + tabla + "_nombre_normalizado "
                    + "ON " + tabla + "(nombre_normalizado)");
        } catch (SQLException e) {
            throw new SQLException("Hay nombres repetidos en la tabla " + tabla
                    + " (mismo nombre con distintas mayúsculas o tildes). Corrígelos antes de iniciar.", e);
        }
    }
}
