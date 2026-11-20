package com.inventario.app;

import com.inventario.app.conexion.ConexionMongoDB;
import com.inventario.app.conexion.InstaladorMongoDB;
import com.inventario.app.utilidades.LicenciaUtil;
import com.inventario.app.vistas.DlgActivacionLicencia;
import com.inventario.app.vistas.FrmLogin;
import javax.swing.SwingUtilities;

public class Sistema_de_inventario {

    public static void main(String[] args) {
        // 1. Verifica que MongoDB esté activo en el puerto 27017 o lo instala/inicia automáticamente
        if (InstaladorMongoDB.verificarOInstalarMongo()) {
            // 2. Abre la conexión, crea config.properties en AppData y genera el usuario MASTER si la BD es nueva
            ConexionMongoDB.getDatabase();
        }

        // 3. Valida el estado de la licencia (30 días de prueba o activación permanente) y lanza la interfaz
        SwingUtilities.invokeLater(() -> {
            LicenciaUtil.ResultadoVerificacion lic = LicenciaUtil.verificarEstado();
            if (!lic.puedeUsarSistema()) {
                DlgActivacionLicencia dlgActivacion = new DlgActivacionLicencia(null, lic, true);
                dlgActivacion.setVisible(true);
                if (!dlgActivacion.isActivadoExitosamente()) {
                    System.exit(0);
                    return;
                }
            }

            FrmLogin login = new FrmLogin();
            login.setVisible(true);
        });
    }
}