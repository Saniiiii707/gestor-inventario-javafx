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

public class UsuarioDAO {

    private static final String HASH_PROVISORIO = "provisorio";

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

    public Optional<Usuario> autenticar(String username, String contrasena) {
        if (username == null || contrasena == null) {
            return Optional.empty();
        }
        String sql = "SELECT id, username, password_hash, rol, activo FROM usuarios WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                boolean activo = rs.getInt("activo") == 1;
                if (!activo || !Contrasenas.verificar(contrasena, rs.getString("password_hash"))) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(
                        rs.getInt("id"),
                        rs.getString("username"),
                        Rol.valueOf(rs.getString("rol")),
                        true));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar el usuario", e);
        }
    }
}
