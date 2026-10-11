package com.inventario.modelo;

public class Movimiento {

    private static final int MAX_DESCRIPCION = 200;

    private final int id;
    private final int productoId;
    private final String productoNombre; // solo para mostrar, viene del JOIN
    private final int usuarioId;
    private final TipoMovimiento tipo;
    private final int cantidad;
    private final String descripcion;    // opcional: puede ser null
    private final String fecha;
    // Kardex: los calcula el DAO; null si el movimiento es anterior al kardex
    private final Integer stockAnterior;
    private final Integer stockResultante;

    // Movimiento nuevo (id y fecha los asigna la base)
    public Movimiento(int productoId, int usuarioId, TipoMovimiento tipo,
                      int cantidad, String descripcion) {
        if (tipo == null) {
            throw new IllegalArgumentException("Elige el tipo de movimiento.");
        }
        if (tipo == TipoMovimiento.AJUSTE) {
            if (cantidad == 0) {
                throw new IllegalArgumentException("La diferencia del ajuste debe ser distinta de cero.");
            }
        } else {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
            }
        }
        this.id = 0;
        this.productoId = productoId;
        this.productoNombre = null;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.descripcion = normalizarDescripcion(descripcion);
        if (tipo == TipoMovimiento.AJUSTE && this.descripcion == null) {
            throw new IllegalArgumentException("La descripción es obligatoria para un ajuste.");
        }
        this.fecha = null;
        this.stockAnterior = null;
        this.stockResultante = null;
    }

    // Movimiento leído de la base
    public Movimiento(int id, int productoId, String productoNombre, int usuarioId,
                      TipoMovimiento tipo, int cantidad, String descripcion, String fecha,
                      Integer stockAnterior, Integer stockResultante) {
        this.id = id;
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.stockAnterior = stockAnterior;
        this.stockResultante = stockResultante;
    }

    // Vacío se guarda como null; si hay texto, se recorta y se limita su largo
    private static String normalizarDescripcion(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpia = texto.trim();
        if (limpia.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException(
                "La descripción no puede superar los " + MAX_DESCRIPCION + " caracteres.");
        }
        return limpia;
    }

    public int getId() { return id; }
    public int getProductoId() { return productoId; }
    public String getProductoNombre() { return productoNombre; }
    public int getUsuarioId() { return usuarioId; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public String getDescripcion() { return descripcion; }
    public String getFecha() { return fecha; }
    public Integer getStockAnterior() { return stockAnterior; }
    public Integer getStockResultante() { return stockResultante; }
}