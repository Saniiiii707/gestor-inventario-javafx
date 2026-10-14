package com.inventario.dao;
import com.inventario.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.inventario.util.Validaciones;

public class CategoriaDAO {

    // CREATE
    public void insertar(Categoria c) {
        String sql = "INSERT INTO categorias (nombre, nombre_normalizado) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getNombre());
            ps.setString(2, Validaciones.normalizarNombre(c.getNombre()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar la categoría", e);
        }
    }

    // READ (todas)
    public List<Categoria> listar() {
        String sql = "SELECT id, nombre FROM categorias ORDER BY nombre_normalizado";
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categorias.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar las categorías", e);
        }
        return categorias;
    }

    // UPDATE
    public void actualizar(Categoria c) {
        String sql = "UPDATE categorias SET nombre = ?, nombre_normalizado = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNombre());
            ps.setString(2, Validaciones.normalizarNombre(c.getNombre()));
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la categoría", e);
        }
    }

    // DELETE
    public void eliminar(int id) {
        String sql = "DELETE FROM categorias WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la categoría", e);
        }
    }
}