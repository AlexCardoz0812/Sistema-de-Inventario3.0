
package com.inventario.app.utilidades;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Encriptador {
    //Convierte cuelquier texto a un hash SH-256 (Irreversible)
    public static String hashear(String textoPlano) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(textoPlano.getBytes(StandardCharsets.UTF_8));
            
            //Convertir bytes a texto hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }catch (Exception e) {
            throw new RuntimeException("Error al encriptar contraseña", e);
        }
    }
}
