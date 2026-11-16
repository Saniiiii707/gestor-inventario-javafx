package com.inventario.vista;

import java.util.function.BiPredicate;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

// Campo "Buscar..." que filtra las opciones de un desplegable mientras se escribe
final class BuscadorCombo {

    private BuscadorCombo() {}

    static <T> TextField crear(ComboBox<T> combo, BiPredicate<T, String> coincide) {
        FilteredList<T> filtrados = new FilteredList<>(combo.getItems());
        combo.setItems(filtrados);
        combo.setPlaceholder(new Label("Sin resultados"));

        TextField campo = new TextField();
        campo.setPromptText("Buscar...");
        campo.setPrefWidth(110);
        campo.textProperty().addListener((obs, anterior, texto) -> {
            T elegido = combo.getValue();
            filtrados.setPredicate(texto.isBlank() ? null : item -> coincide.test(item, texto));
            if (combo.getValue() == null && elegido != null) {
                combo.setValue(elegido); // filtrar no debe borrar lo que ya estaba elegido
            }
            if (!texto.isBlank() && combo.getScene() != null) {
                combo.show();
            }
        });
        // Enter elige la primera coincidencia, para no tener que usar el mouse
        campo.setOnAction(e -> {
            if (!filtrados.isEmpty()) {
                combo.setValue(filtrados.get(0));
                combo.hide();
            }
        });
        return campo;
    }
}
