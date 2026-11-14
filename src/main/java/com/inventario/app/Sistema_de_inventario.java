
package com.inventario.app;

import com.inventario.app.vistas.FrmLogin;
import javax.swing.SwingUtilities;

public class Sistema_de_inventario {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                // Instanciamos y mostramos la ventana de Login
                FrmLogin login = new FrmLogin();
                login.setVisible(true);
            }
        });
    }
}
