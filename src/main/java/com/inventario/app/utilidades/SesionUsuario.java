package com.inventario.app.utilidades;

public class SesionUsuario {
    private static String usernameActual = "sistema";
    private static String rolActual = "CONSULTA";

    public static void iniciarSesion(String username, String rol) {
        usernameActual = (username != null && !username.isBlank()) ? username : "sistema";
        rolActual = (rol != null && !rol.isBlank()) ? rol : "CONSULTA";
    }

    public static void cerrarSesion() {
        usernameActual = "sistema";
        rolActual = "CONSULTA";
    }

    public static String getUsernameActual() {
        return usernameActual;
    }

    public static String getRolActual() {
        return rolActual;
    }
}