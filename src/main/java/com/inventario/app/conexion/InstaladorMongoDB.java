package com.inventario.app.conexion;

import javax.swing.JOptionPane;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InstaladorMongoDB {

    private static final String NOMBRE_MSI = "mongodb-windows-x86_64-5.0.35-signed.msi";

    public static boolean verificarOInstalarMongo() {
        // 1. Verifica si MongoDB ya está escuchando en localhost:27017
        if (puertoEstaActivo("127.0.0.1", 27017)) {
            return true;
        }

        // 2. Intenta iniciar el servicio por si ya está instalado pero detenido
        try {
            Process startService = new ProcessBuilder("cmd.exe", "/c", "net start MongoDB").start();
            startService.waitFor();
            if (puertoEstaActivo("127.0.0.1", 27017)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // 3. Busca el instalador .msi tanto en la raíz como dentro de la carpeta app\ de jpackage
        File instaladorMsi = new File("recursos_bd" + File.separator + NOMBRE_MSI);
        if (!instaladorMsi.exists()) {
            instaladorMsi = new File("app" + File.separator + "recursos_bd" + File.separator + NOMBRE_MSI);
        }

        if (!instaladorMsi.exists()) {
            JOptionPane.showMessageDialog(null,
                    "El motor de base de datos MongoDB no está activo y no se encontró el instalador en:\n"
                    + instaladorMsi.getAbsolutePath(),
                    "Base de Datos Requerida", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        int respuesta = JOptionPane.showConfirmDialog(null,
                "Es la primera vez que ejecuta el sistema y requiere instalar el motor de base de datos.\n"
                + "¿Desea instalarlo automáticamente ahora? (Acepte el permiso de Windows cuando aparezca)",
                "Configuración Inicial", JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            try {
                String comandoPowerShell = String.format(
                    "Start-Process msiexec.exe -ArgumentList '/q /i \"%s\" INSTALLLOCATION=\"C:\\Program Files\\MongoDB\\Server\\5.0\\\" ADDLOCAL=\"ServerService\" SHOULD_INSTALL_COMPASS=\"0\"' -Verb RunAs -Wait",
                    instaladorMsi.getAbsolutePath()
                );
                Process p = new ProcessBuilder("powershell.exe", "-Command", comandoPowerShell).start();
                p.waitFor();

                // Asegura que el servicio recién instalado quede iniciado
                new ProcessBuilder("cmd.exe", "/c", "net start MongoDB").start().waitFor();
                return puertoEstaActivo("127.0.0.1", 27017);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error al instalar MongoDB: " + e.getMessage());
            }
        }
        return false;
    }

    private static boolean puertoEstaActivo(String host, int puerto) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}