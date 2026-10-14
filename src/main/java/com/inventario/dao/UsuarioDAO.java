package com.inventario.dao;

import com.inventario.modelo.Rol;
import com.inventario.modelo.Usuario;
import com.inventario.util.Contrasenas;
import com.inventario.util.Validaciones;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import com.inventario.modelo.ResultadoAcceso;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static final String HASH_PROVISORIO = "provisorio";
    private static final int MAX_INTENTOS = 5;
    public static final int MINUTOS_BLOQUEO = 5;

    private static final String HASH_DE_RELLENO = Contrasenas.hashear("relleno-para-igualar-tiempos");

    private final RegistroAccesoDAO registro = new RegistroAccesoDAO();

    private record Credenciales(int id, String username, String hash, Rol rol,
                            boolean activo, boolean bloqueado) { }

    public boolean necesitaConfiguracionInicial() {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE password_hash <> ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, HASH_PROVISORIO);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al revisar los usuarios", e);
        }
    }

    public void crearAdministradorInicial(Usuario admin, String contrasena) {
        if (!admin.esAdmin()) {
            throw new IllegalArgumentException("El primer usuario debe ser administrador.");
        }

        String hash = Contrasenas.hashear(Validaciones.contrasenaValida(contrasena));

        String sql = """
            UPDATE usuarios
            SET username = ?, password_hash = ?, rol = 'ADMIN', activo = 1,
                intentos_fallidos = 0, bloqueado_hasta = NULL
            WHERE id = 1 AND password_hash = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, admin.getUsername());
            ps.setString(2, hash);
            ps.setString(3, HASH_PROVISORIO);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException("El administrador inicial ya fue creado.");
            }
            admin.setId(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el administrador", e);
        }
    }

    // Lista para la pestaña Usuarios. No lee el hash: no hace falta para mostrar.
    public List<Usuario> listar() {
        String sql = "SELECT id, username, rol, activo FROM usuarios ORDER BY username";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("username"),
                        Rol.valueOf(rs.getString("rol")),
                        rs.getInt("activo") == 1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los usuarios", e);
        }
        return usuarios;
    }

    // Crea un usuario nuevo con su contraseña inicial
    public void crear(Usuario usuario, String contrasena) {
        // El hash (lento a propósito) se calcula antes de abrir la conexión
        String hash = Contrasenas.hashear(Validaciones.contrasenaValida(contrasena));
        String sql = "INSERT INTO usuarios (username, password_hash, rol, activo, debe_cambiar_contrasena) "
                + "VALUES (?, ?, ?, 1, 1)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getUsername());
            ps.setString(2, hash);
            ps.setString(3, usuario.getRol().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el usuario", e);
        }
    }

    public Optional<Usuario> autenticar(String username, String contrasena) {
        if (username == null || contrasena == null) {
            return Optional.empty();
        }
        String buscado = username.trim().toLowerCase(Locale.ROOT);

        Optional<Credenciales> encontrado = buscarCredenciales(buscado);
        if (encontrado.isEmpty()) {
            Contrasenas.verificar(contrasena, HASH_DE_RELLENO);
            registro.registrar(buscado, null, ResultadoAcceso.USUARIO_INEXISTENTE);
            return Optional.empty();
        }
        Credenciales c = encontrado.get();
        boolean contrasenaCorrecta = Contrasenas.verificar(contrasena, c.hash());

        if (c.bloqueado()) {
            registro.registrar(buscado, c.id(), ResultadoAcceso.CUENTA_BLOQUEADA);
            return Optional.empty();
        }
        if (!c.activo()) {
            registro.registrar(buscado, c.id(), ResultadoAcceso.CUENTA_INACTIVA);
            return Optional.empty();
        }
        if (!contrasenaCorrecta) {
            boolean seBloqueo = registrarFallo(c.id());
            registro.registrar(buscado, c.id(),
                    seBloqueo ? ResultadoAcceso.BLOQUEO_ACTIVADO : ResultadoAcceso.CONTRASENA_INCORRECTA);
            return Optional.empty();
        }
        reiniciarIntentos(c.id());
        registro.registrar(buscado, c.id(), ResultadoAcceso.CORRECTO);
        return Optional.of(new Usuario(c.id(), c.username(), c.rol(), true));
    }

    private Optional<Credenciales> buscarCredenciales(String username) {
        String sql = """
            SELECT id, username, password_hash, rol, activo,
                   COALESCE(bloqueado_hasta > datetime('now', 'localtime'), 0) AS bloqueado
            FROM usuarios WHERE username = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Credenciales(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Rol.valueOf(rs.getString("rol")),
                        rs.getInt("activo") == 1,
                        rs.getInt("bloqueado") == 1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el usuario", e);
        }
    }

    private boolean registrarFallo(int id) {
        String sql = """
            UPDATE usuarios SET
                bloqueado_hasta = CASE WHEN intentos_fallidos + 1 >= ?
                                       THEN datetime('now', 'localtime', ?) ELSE NULL END,
                intentos_fallidos = CASE WHEN intentos_fallidos + 1 >= ?
                                         THEN 0 ELSE intentos_fallidos + 1 END
            WHERE id = ?
            RETURNING bloqueado_hasta IS NOT NULL AS bloqueo
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, MAX_INTENTOS);
            ps.setString(2, "+" + MINUTOS_BLOQUEO + " minutes");
            ps.setInt(3, MAX_INTENTOS);
            ps.setInt(4, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("bloqueo") == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el intento fallido", e);
        }
    }

    private void reiniciarIntentos(int id) {
        String sql = "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al reiniciar los intentos", e);
        }
    }

    // --- Administración de usuarios (el permiso se comprueba en el controlador) ---

    // Condición SQL: además del usuario que se modifica, existe al menos otro ADMIN activo.
    // Es un texto fijo del código (no viene del usuario), por eso se puede concatenar.
    private static final String QUEDA_OTRO_ADMIN =
            "(SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = 1 AND id <> ?) >= 1";

    public void cambiarRol(int id, Rol nuevoRol) {
        // Pasar a ADMIN siempre se puede; dejar de serlo, solo si queda otro ADMIN activo
        String sql = "UPDATE usuarios SET rol = ? WHERE id = ? AND (? = 'ADMIN' OR " + QUEDA_OTRO_ADMIN + ")";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoRol.name());
            ps.setInt(2, id);
            ps.setString(3, nuevoRol.name());
            ps.setInt(4, id);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el rol", e);
        }
    }

    public void desactivar(int id) {
        String sql = "UPDATE usuarios SET activo = 0 WHERE id = ? AND " + QUEDA_OTRO_ADMIN;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al desactivar el usuario", e);
        }
    }

    // Reactivar también quita un bloqueo que hubiera quedado pendiente
    public void reactivar(int id) {
        String sql = "UPDATE usuarios SET activo = 1, intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al reactivar el usuario", e);
        }
    }

    // Para quien olvidó su contraseña. También desbloquea la cuenta.
    public void restablecerContrasena(int id, String nuevaContrasena) {
        String valida = Validaciones.contrasenaValida(nuevaContrasena);

        // El hash no se puede revertir; para saber si es la misma contraseña se verifica
        // la nueva contra el hash guardado, igual que en el login.
        if (Contrasenas.verificar(valida, leerHash(id))) {
            throw new IllegalStateException("La nueva contraseña debe ser distinta de la actual.");
        }

        String hash = Contrasenas.hashear(valida);
        String sql = "UPDATE usuarios SET password_hash = ?, intentos_fallidos = 0, bloqueado_hasta = NULL, "
                + "debe_cambiar_contrasena = 1 WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al restablecer la contraseña", e);
        }
    }

    // Lee el hash guardado. Devuelve null si el usuario no existe.
    private String leerHash(int id) {
        String sql = "SELECT password_hash FROM usuarios WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("password_hash") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer el usuario", e);
        }
    }

    // True si el usuario debe elegir una contraseña nueva antes de usar la app
    public boolean debeCambiarContrasena(int id) {
        String sql = "SELECT debe_cambiar_contrasena FROM usuarios WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("debe_cambiar_contrasena") == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer el usuario", e);
        }
    }

    // La propia persona cambia su contraseña. Exige la actual: así, quien encuentre
    // la sesión abierta no puede cambiarla sin conocerla.
    public void cambiarContrasena(int id, String actual, String nueva) {
        String hashActual = leerHash(id);
        if (!Contrasenas.verificar(actual, hashActual)) {
            throw new ContrasenaIncorrectaException();
        }
        Validaciones.contrasenaValida(nueva);
        if (Contrasenas.verificar(nueva, hashActual)) {
            // Misma regla y mismo tipo de excepción que en restablecerContrasena
            throw new IllegalStateException("La nueva contraseña debe ser distinta de la actual.");
        }
        String hashNuevo = Contrasenas.hashear(nueva);

        String sql = "UPDATE usuarios SET password_hash = ?, debe_cambiar_contrasena = 0 WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hashNuevo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar la contraseña", e);
        }
    }
}
