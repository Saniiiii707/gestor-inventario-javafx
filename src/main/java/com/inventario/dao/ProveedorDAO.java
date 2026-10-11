package com.inventario.dao;

import com.inventario.modelo.Proveedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.inventario.util.Validaciones;

public class ProveedorDAO {

    public void insertar(Proveedor p) {
        String sql = "INSERT INTO proveedores (nombre, telefono, nombre_normalizado) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getNombre());
            ps.setString(2, p.getTelefono());
            ps.setString(3, Validaciones.normalizarNombre(p.getNombre()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar el proveedor", e);
        }
    }

    public List<Proveedor> listar() {
        String sql = "SELECT id, nombre, telefono FROM proveedores ORDER BY nombre_normalizado";
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                proveedores.add(new Proveedor(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("telefono")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los proveedores", e);
        }
        return proveedores;
    }

    public void actualizar(Proveedor p) {
        String sql = "UPDATE proveedores SET nombre = ?, telefono = ?, nombre_normalizado = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNombre());
            ps.setString(2, p.getTelefono());
            ps.setString(3, Validaciones.normalizarNombre(p.getNombre()));
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el proveedor", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM proveedores WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el proveedor", e);
        }
    }
}
