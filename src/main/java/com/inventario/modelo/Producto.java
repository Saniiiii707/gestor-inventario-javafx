package com.inventario.modelo;

import com.inventario.util.Validaciones;

public class Producto {

    // Tope del stock total: muy por debajo del máximo de un int (2.147.483.647)
    public static final int MAX_STOCK = 1_000_000_000;

    private int id;
    private String nombre;
    private double precio;
    private int stockActual;
    private int stockMinimo;
    private int categoriaId;
    private String categoriaNombre;    // solo para mostrar, viene del JOIN
    private Integer proveedorId;       // opcional: null si no tiene proveedor
    private String proveedorNombre;    // solo para mostrar, viene del LEFT JOIN

    // Producto nuevo (aún sin id; el stock lo manejan los movimientos)
    public Producto(String nombre, double precio, int stockMinimo,
                    int categoriaId, Integer proveedorId) {
        setNombre(nombre);
        setPrecio(precio);
        setStockMinimo(stockMinimo);
        this.categoriaId = categoriaId;
        this.proveedorId = proveedorId;
    }

    // Producto leído de la base
    public Producto(int id, String nombre, double precio, int stockActual, int stockMinimo,
                    int categoriaId, String categoriaNombre,
                    Integer proveedorId, String proveedorNombre) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.proveedorId = proveedorId;
        this.proveedorNombre = proveedorNombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = Validaciones.nombreValido(nombre, "nombre del producto");
    }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) {
        // isFinite rechaza Infinity y NaN, que Double.parseDouble acepta
        if (!Double.isFinite(precio) || precio < 0) {
            throw new IllegalArgumentException("El precio debe ser un número válido, mayor o igual a cero.");
        }
        this.precio = precio;
    }

    public int getStockActual() { return stockActual; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) {
        if (stockMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }
        this.stockMinimo = stockMinimo;
    }

    public int getCategoriaId() { return categoriaId; }

    public String getCategoriaNombre() { return categoriaNombre; }

    public Integer getProveedorId() { return proveedorId; }

    public String getProveedorNombre() { return proveedorNombre; }

    // La regla de negocio vive aquí: llegar al mínimo ya cuenta como stock bajo
    public boolean esStockBajo(int stock) {
        return stock <= stockMinimo;
    }

    public boolean tieneStockBajo() {
        return esStockBajo(stockActual);
    }
}
