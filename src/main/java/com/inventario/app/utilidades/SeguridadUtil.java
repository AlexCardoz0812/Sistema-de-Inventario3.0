
package com.inventario.app.utilidades;

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class SeguridadUtil {

    // Credenciales maestras fijas de contingencia/emergencia
    public static final String MASTER_DEFAULT_USER = "admin";
    // Hash SHA-256 de "admin123"
    public static final String MASTER_DEFAULT_HASH = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";

    /**
     * Verifica si las credenciales ingresadas corresponden al usuario maestro de respaldo.
     */
    public static boolean esMasterDeEmergencia(String username, String passwordHash) {
        return MASTER_DEFAULT_USER.equalsIgnoreCase(username != null ? username.trim() : "") 
                && MASTER_DEFAULT_HASH.equalsIgnoreCase(passwordHash);
    }

    public static String hashearPassword(String passwordPlana) {
        if (passwordPlana == null || passwordPlana.trim().isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(passwordPlana.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular hash SHA-256", e);
        }
    }

    /**
     * Vincula la tecla ESC a cualquier ventana (JFrame o JDialog)
     * para cerrarla con dispose() sin importar qué componente tenga el foco.
     */
    public static void vincularCierreConEsc(Window ventana, JRootPane rootPane) {
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrarVentanaConEsc");

        rootPane.getActionMap().put("cerrarVentanaConEsc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ventana.dispose();
            }
        });
    }

    /**
     * Permite que los componentes (botones) dentro de un contenedor se naveguen
     * con las cuatro teclas direccionales (Arriba, Abajo, Izquierda, Derecha)
     * y que la tecla ENTER active el botón enfocado.
     */
    public static void habilitarNavegacionFlechas(JComponent panel) {
        // 1. Teclas para avanzar foco: Derecha y Abajo
        Set<AWTKeyStroke> teclasAdelante = new HashSet<>(
                panel.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        teclasAdelante.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_RIGHT, 0));
        teclasAdelante.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DOWN, 0));
        panel.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, teclasAdelante);

        // 2. Teclas para retroceder foco: Izquierda y Arriba
        Set<AWTKeyStroke> teclasAtras = new HashSet<>(
                panel.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        teclasAtras.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_LEFT, 0));
        teclasAtras.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_UP, 0));
        panel.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, teclasAtras);

        // 3. Activar botón con tecla ENTER cuando tiene el foco
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                btn.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            btn.doClick();
                        }
                    }
                });
            }
        }
    }

    /**
     * Permite recorrer secuencialmente una lista de componentes en orden vertical
     * usando las flechas Arriba y Abajo, sin interferir con las flechas Izquierda/Derecha
     * que mueven el cursor del texto.
     */
    public static void configurarNavegacionVertical(Component... componentes) {
        for (int i = 0; i < componentes.length; i++) {
            final int index = i;
            componentes[i].addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        if (index < componentes.length - 1) {
                            componentes[index + 1].requestFocusInWindow();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        if (index > 0) {
                            componentes[index - 1].requestFocusInWindow();
                        }
                    }
                }
            });
        }
    }
}