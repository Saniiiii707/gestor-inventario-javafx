package com.inventario.vista;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

// Botón "Exportar CSV": guarda lo que muestra la tabla (con sus filtros y orden) en un archivo
// que Excel en español abre directo: separador ";", coma decimal y BOM para las tildes.
final class ExportarCsv {

    private ExportarCsv() {}

    static <T> Button crearBoton(TableView<T> tabla, String nombreSugerido) {
        Button boton = new Button("Exportar CSV");
        boton.setOnAction(e -> exportar(tabla, nombreSugerido));
        return boton;
    }

    private static <T> void exportar(TableView<T> tabla, String nombreSugerido) {
        if (tabla.getItems().isEmpty()) {
            Mensajes.mostrar(Alert.AlertType.WARNING, "No hay filas para exportar.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar como CSV");
        selector.setInitialFileName(nombreSugerido + ".csv");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File archivo = selector.showSaveDialog(tabla.getScene().getWindow());
        if (archivo == null) {
            return; // canceló
        }
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getPath() + ".csv");
            // El diálogo confirmó el reemplazo del nombre sin ".csv", no de este: hay que preguntar
            if (archivo.exists() && !Mensajes.confirmar("Ya existe " + archivo.getName() + ". ¿Reemplazarlo?")) {
                return;
            }
        }

        try {
            List<String> lineas = new ArrayList<>();
            List<String> celdas = new ArrayList<>();
            for (TableColumn<T, ?> col : tabla.getColumns()) {
                celdas.add(celda(col.getText()));
            }
            lineas.add(String.join(";", celdas));
            for (int fila = 0; fila < tabla.getItems().size(); fila++) {
                celdas.clear();
                for (TableColumn<T, ?> col : tabla.getColumns()) {
                    celdas.add(celda(col.getCellData(fila)));
                }
                lineas.add(String.join(";", celdas));
            }
            // El BOM inicial: sin él, Excel muestra mal las tildes y la ñ
            Files.writeString(archivo.toPath(), "\uFEFF" + String.join("\r\n", lineas) + "\r\n",
                    StandardCharsets.UTF_8);
            Mensajes.mostrar(Alert.AlertType.INFORMATION,
                    "Se exportaron " + tabla.getItems().size() + " fila(s) a " + archivo.getName() + ".");
        } catch (IOException ex) {
            // En Windows lo más común es que Excel tenga el archivo abierto y bloqueado
            Mensajes.mostrar(Alert.AlertType.ERROR, "No se pudo guardar " + archivo.getName()
                    + ". ¿Está abierto en otro programa, como Excel? Ciérralo e inténtalo de nuevo."
                    + "\n\nDetalle: " + ex.getMessage());
        } catch (RuntimeException ex) {
            // Por ejemplo, un precio inválido en la base (NaN o infinito, solo editándola a mano)
            Mensajes.mostrar(Alert.AlertType.ERROR, "No se pudo exportar. " + Mensajes.traducirError(ex));
        }
    }

    static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Double d) {
            // Sin notación científica (1.25E7) y con coma decimal, como espera Excel en español
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString().replace('.', ',');
        }
        if (valor instanceof Number) {
            return valor.toString();
        }
        String texto = valor.toString();
        // Evita que Excel ejecute como fórmula un texto escrito por el usuario (inyección CSV)
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
