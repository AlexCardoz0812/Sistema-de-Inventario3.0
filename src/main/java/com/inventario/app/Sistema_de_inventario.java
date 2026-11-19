package com.inventario.app;

import com.inventario.app.conexion.ConexionMongoDB;
import com.inventario.app.conexion.InstaladorMongoDB;
import com.inventario.app.vistas.FrmLogin;
import javax.swing.SwingUtilities;

public class Sistema_de_inventario {

    public static void main(String[] args) {
        // 1. Verifica que MongoDB esté activo en el puerto 27017 o lo instala/inicia automáticamente
        if (InstaladorMongoDB.verificarOInstalarMongo()) {
            // 2. Abre la conexión, crea config.properties en AppData y genera el usuario MASTER si la BD es nueva
            ConexionMongoDB.getDatabase();
        }

        // 3. Muestra la interfaz gráfica de inicio de sesión
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FrmLogin login = new FrmLogin();
                login.setVisible(true);
            }
        });
    }
}


