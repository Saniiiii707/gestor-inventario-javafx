package com.inventario.dao;

import com.inventario.modelo.Movimiento;
import com.inventario.modelo.TipoMovimiento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {

    public void registrar(Movimiento m) {
        if (m.getTipo() == TipoMovimiento.AJUSTE) {
            throw new IllegalArgumentException(
                "Para registrar un ajuste usa registrarAjuste(...).");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int resultante = actualizarStock(conn, m);
                int anterior = m.getTipo() == TipoMovimiento.ENTRADA
                        ? resultante - m.getCantidad()
                        : resultante + m.getCantidad();
                insertarMovimiento(conn, m, anterior, resultante);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el movimiento", e);
        }
    }

    // El usuario informa el stock contado; la diferencia se calcula aquí, dentro
    // de la misma transacción, y se guarda como cantidad (con signo).
    public void registrarAjuste(int productoId, int usuarioId, int stockContado,
                                int stockVisto, String descripcion) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int stockActual = leerStock(conn, productoId);
                if (stockActual != stockVisto) {
                    throw new IllegalStateException(
                        "El stock cambió desde que elegiste el producto (ahora es " + stockActual
                        + "). Vuelve a elegirlo y a contar.");
                }
                int diferencia = stockContado - stockActual;
                if (diferencia == 0) {
                    throw new IllegalStateException(
                        "El stock contado coincide con el del sistema; no hay nada que ajustar.");
                }
                actualizarStockAjuste(conn, productoId, stockContado, stockActual);
                Movimiento ajuste = new Movimiento(
                        productoId, usuarioId, TipoMovimiento.AJUSTE, diferencia, descripcion);
                insertarMovimiento(conn, ajuste, stockActual, stockContado);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el ajuste", e);
        }
    }

    private int leerStock(Connection conn, int productoId) throws SQLException {
        String sql = "SELECT stock_actual FROM productos WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("El producto no existe.");
                }
                return rs.getInt("stock_actual");
            }
        }
    }

    // Solo actualiza si el stock sigue siendo el mismo que se leyó (control optimista).
    private void actualizarStockAjuste(Connection conn, int productoId, int stockContado, int stockLeido)
            throws SQLException {
        String sql = "UPDATE productos SET stock_actual = ? WHERE id = ? AND stock_actual = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockContado);
            ps.setInt(2, productoId);
            ps.setInt(3, stockLeido);
            int filasModificadas = ps.executeUpdate();
            if (filasModificadas == 0) {
                throw new IllegalStateException(
                    "El stock cambió mientras se registraba el ajuste. Vuelve a intentarlo.");
            }
        }
    }

    // Devuelve el stock resultante (RETURNING); sin fila, el UPDATE no aplicó.
    private int actualizarStock(Connection conn, Movimiento m) throws SQLException {
        TipoMovimiento tipo = m.getTipo();
        if (tipo == TipoMovimiento.SALIDA) {
            String sql = "UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ? AND stock_actual >= ? "
                    + "RETURNING stock_actual";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, m.getCantidad());
                ps.setInt(2, m.getProductoId());
                ps.setInt(3, m.getCantidad());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("No hay stock suficiente para registrar esta salida.");
                    }
                    return rs.getInt("stock_actual");
                }
            }
        } else if (tipo == TipoMovimiento.ENTRADA) {
            String sql = "UPDATE productos SET stock_actual = stock_actual + ? WHERE id = ? RETURNING stock_actual";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, m.getCantidad());
                ps.setInt(2, m.getProductoId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("El producto no existe.");
                    }
                    return rs.getInt("stock_actual");
                }
            }
        }
        throw new IllegalArgumentException("Tipo de movimiento no soportado: " + tipo);
    }

    private void insertarMovimiento(Connection conn, Movimiento m, int stockAnterior, int stockResultante)
            throws SQLException {
        String sql = """
            INSERT INTO movimientos (producto_id, usuario_id, tipo, cantidad, descripcion, fecha,
                                     stock_anterior, stock_resultante)
            VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime'), ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getProductoId());
            ps.setInt(2, m.getUsuarioId());
            ps.setString(3, m.getTipo().name());
            ps.setInt(4, m.getCantidad());
            ps.setString(5, m.getDescripcion());
            ps.setInt(6, stockAnterior);
            ps.setInt(7, stockResultante);
            ps.executeUpdate();
        }
    }

    public List<Movimiento> listar() {
        String sql = """
            SELECT m.id, m.producto_id, p.nombre AS producto, m.usuario_id,
                   m.tipo, m.cantidad, m.descripcion, m.fecha,
                   m.stock_anterior, m.stock_resultante
            FROM movimientos m
            JOIN productos p ON p.id = m.producto_id
            ORDER BY m.id DESC
            LIMIT 200
            """;
        List<Movimiento> movimientos = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                movimientos.add(new Movimiento(
                    rs.getInt("id"),
                    rs.getInt("producto_id"),
                    rs.getString("producto"),
                    rs.getInt("usuario_id"),
                    TipoMovimiento.valueOf(rs.getString("tipo")),
                    rs.getInt("cantidad"),
                    rs.getString("descripcion"),
                    rs.getString("fecha"),
                    leerEnteroOpcional(rs, "stock_anterior"),
                    leerEnteroOpcional(rs, "stock_resultante")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los movimientos", e);
        }
        return movimientos;
    }

    // getInt devuelve 0 para NULL; aquí se conserva el NULL (movimientos anteriores al kardex)
    private Integer leerEnteroOpcional(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }
}