package com.inventario.util;

import at.favre.lib.crypto.bcrypt.BCrypt;


public class Contrasenas {


    private static final int COSTO = 12;

    private Contrasenas() { }


    public static String hashear(String contrasena) {
        return BCrypt.withDefaults().hashToString(COSTO, contrasena.toCharArray());
    }


    public static boolean verificar(String contrasena, String hashGuardado) {
        if (contrasena == null || hashGuardado == null) {
            return false;
        }
        return BCrypt.verifyer().verify(contrasena.toCharArray(), hashGuardado).verified;
    }
}