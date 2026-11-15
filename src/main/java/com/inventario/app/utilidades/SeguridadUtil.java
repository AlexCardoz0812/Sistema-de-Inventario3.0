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

    public static final String MASTER_DEFAULT_USER = "admin";
    public static final String MASTER_DEFAULT_HASH = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";

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

    public static void habilitarNavegacionFlechas(JComponent panel) {
        Set<AWTKeyStroke> teclasAdelante = new HashSet<>(
                panel.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        teclasAdelante.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_RIGHT, 0));
        teclasAdelante.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DOWN, 0));
        panel.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, teclasAdelante);

        Set<AWTKeyStroke> teclasAtras = new HashSet<>(
                panel.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        teclasAtras.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_LEFT, 0));
        teclasAtras.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_UP, 0));
        panel.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, teclasAtras);

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