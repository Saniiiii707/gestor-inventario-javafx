package com.inventario.dao;

import com.inventario.modelo.ResultadoAcceso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class RegistroAccesoDAO {

    static final String USUARIO_DESCONOCIDO = "(desconocido)";

    public void registrar(String usuarioIntentado, Integer usuarioId, ResultadoAcceso resultado) {
        String sql = "INSERT INTO registro_accesos (usuario_intentado, usuario_id, resultado) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Si el usuario no existe, lo escrito puede ser una contraseña tipeada en el campo
            // equivocado: no se guarda. El intento igual queda registrado con su fecha y resultado.
            ps.setString(1, usuarioId == null ? USUARIO_DESCONOCIDO : usuarioIntentado);
            if (usuarioId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, usuarioId);
            }
            ps.setString(3, resultado.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el acceso", e);
        }
    }
}