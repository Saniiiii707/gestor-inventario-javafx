package com.inventario.vista;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class Mensajes {

    public static void mostrar(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    public static boolean confirmar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alerta.setHeaderText(null);
        return alerta.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    // Convierte los errores técnicos de la base en mensajes para el usuario
    public static String traducirError(RuntimeException ex) {
        String detalle = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        if (detalle != null) {
            if (detalle.contains("UNIQUE")) {
                return "Ya existe un registro con ese nombre.";
            }
            if (detalle.contains("FOREIGN KEY")) {
                return "No se puede completar: hay datos relacionados (por ejemplo, tiene productos asociados).";
            }
            if (detalle.contains("CHECK")) {
                return "Algún valor no cumple las reglas de la base de datos.";
            }
        }
        return "Ocurrió un error: " + detalle;
    }
}