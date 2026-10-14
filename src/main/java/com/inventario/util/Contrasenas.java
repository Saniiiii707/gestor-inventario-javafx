package com.inventario.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.charset.StandardCharsets;


public class Contrasenas {


    private static final int COSTO = 12;
    public static final int MAX_BYTES = 72;   // límite de BCrypt (bytes, no caracteres)

    private Contrasenas() { }


    public static String hashear(String contrasena) {
        return BCrypt.withDefaults().hashToString(COSTO, contrasena.toCharArray());
    }


    public static boolean verificar(String contrasena, String hashGuardado) {
        if (contrasena == null || hashGuardado == null) {
            return false;
        }
        // BCrypt lanza una excepción con más de 72 bytes; para quien inicia sesión, una contraseña
        // así simplemente es incorrecta (cuenta para el bloqueo y queda en el registro)
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return false;
        }
        return BCrypt.verifyer().verify(contrasena.toCharArray(), hashGuardado).verified;
    }
}