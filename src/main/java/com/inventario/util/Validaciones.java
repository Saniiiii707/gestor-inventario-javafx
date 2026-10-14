package com.inventario.util;
import java.text.Normalizer;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

public class Validaciones {

    public static String nombreValido(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El " + campo + " no puede estar vacío.");
        }
        String limpio = texto.trim();
        if (!limpio.matches(".*\\p{L}.*")) {
            throw new IllegalArgumentException("El " + campo + " debe contener al menos una letra.");
        }
        return limpio;
    }

    public static String telefonoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (!limpio.matches("[0-9+()\\-\\s]{5,20}")) {
            throw new IllegalArgumentException(
                "El teléfono solo puede tener números, espacios y los símbolos + - ( ), entre 5 y 20 caracteres.");
        }
        return limpio;
    }

    public static String normalizarNombre(String texto) {
        String compuesto = Normalizer.normalize(texto.trim(), Normalizer.Form.NFC);
        StringBuilder clave = new StringBuilder();
        for (char c : compuesto.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == 'ñ') {
                clave.append(c);
            } else {
                String separado = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                clave.append(separado.replaceAll("\\p{M}", ""));
            }
        }
        return clave.toString().replaceAll("\\s+", " ");
    }


    public static String usuarioValido(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío.");
        }
        String limpio = texto.trim().toLowerCase(Locale.ROOT);
        if (!limpio.matches("[a-z][a-z0-9._]{2,29}")) {
            throw new IllegalArgumentException("Nombre de usuario no válido.");
        }
        return limpio;
    }


    public static String contrasenaValida(String texto) {
        if (texto == null || texto.isBlank() || texto.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (texto.getBytes(StandardCharsets.UTF_8).length > Contrasenas.MAX_BYTES) {
            throw new IllegalArgumentException(
                "La contraseña es demasiado larga (máximo " + Contrasenas.MAX_BYTES + " caracteres; la ñ y las letras con tilde cuentan doble).");
        }
        return texto;
    }
}
