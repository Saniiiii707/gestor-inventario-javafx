package com.inventario.dao;

import com.inventario.modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public void insertar(Producto p) {
        String sql = """
            INSERT INTO productos (nombre, precio, stock_minimo, categoria_id, proveedor_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setInt(3, p.getStockMinimo());
            ps.setInt(4, p.getCategoriaId());
            asignarProveedor(ps, 5, p.getProveedorId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar el producto", e);
        }
    }

    public List<Producto> listar() {
        String sql = """
            SELECT p.id, p.nombre, p.precio, p.stock_actual, p.stock_minimo,
                   p.categoria_id, c.nombre AS categoria,
                   p.proveedor_id, pr.nombre AS proveedor
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN proveedores pr ON pr.id = p.proveedor_id
            ORDER BY p.nombre
            """;
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int proveedorLeido = rs.getInt("proveedor_id");
                Integer proveedorId = rs.wasNull() ? null : Integer.valueOf(proveedorLeido);

                productos.add(new Producto(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("stock_actual"),
                    rs.getInt("stock_minimo"),
                    rs.getInt("categoria_id"),
                    rs.getString("categoria"),
                    proveedorId,
                    rs.getString("proveedor")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los productos", e);
        }
        return productos;
    }

    public void actualizar(Producto p) {
        String sql = """
            UPDATE productos
            SET nombre = ?, precio = ?, stock_minimo = ?, categoria_id = ?, proveedor_id = ?
            WHERE id = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setInt(3, p.getStockMinimo());
            ps.setInt(4, p.getCategoriaId());
            asignarProveedor(ps, 5, p.getProveedorId());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el producto", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM productos WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el producto", e);
        }
    }

    // Guarda un NULL de verdad cuando el producto no tiene proveedor
    private void asignarProveedor(PreparedStatement ps, int posicion, Integer proveedorId)
            throws SQLException {
        if (proveedorId == null) {
            ps.setNull(posicion, Types.INTEGER);
        } else {
            ps.setInt(posicion, proveedorId);
        }
    }
}
