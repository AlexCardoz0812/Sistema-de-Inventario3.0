package com.inventario.app.vistas;

import com.inventario.app.dao.UsuarioDAO;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.utilidades.SeguridadUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class FrmLogin extends javax.swing.JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnRegistrarse;

    public FrmLogin() {
        configurarVentana();
        this.setLocationRelativeTo(null);
        
        // 1. Vincular ENTER por defecto al botón Ingresar
        this.getRootPane().setDefaultButton(this.btnIngresar);

        // 2. Vincular ESC para salir
        SeguridadUtil.vincularCierreConEsc(this, this.getRootPane());

        // 3. Activar navegación con flechas Arriba y Abajo entre campos y botones
        SeguridadUtil.configurarNavegacionVertical(txtUsuario, txtPassword, btnIngresar, btnRegistrarse);
    }

    private void configurarVentana() {
        setTitle("Iniciar Sesión - Sistema de Inventario");
        setSize(360, 290);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // Campo Usuario
        JPanel panelUsuario = new JPanel(new BorderLayout());
        panelUsuario.add(new JLabel("Usuario: "), BorderLayout.NORTH);
        txtUsuario = new JTextField();
        panelUsuario.add(txtUsuario, BorderLayout.CENTER);
        add(panelUsuario);

        // Campo Contraseña
        JPanel panelPassword = new JPanel(new BorderLayout());
        panelPassword.add(new JLabel("Contraseña: "), BorderLayout.NORTH);
        txtPassword = new JPasswordField();
        panelPassword.add(txtPassword, BorderLayout.CENTER);
        add(panelPassword);

        // Botón Ingresar con atajo ALT + I
        btnIngresar = new JButton("Iniciar Sesión");
        btnIngresar.setMnemonic(KeyEvent.VK_I);
        btnIngresar.setDisplayedMnemonicIndex(0);
        btnIngresar.setToolTipText("Presione ALT + I o ENTER para ingresar");
        add(btnIngresar);

        // Botón Registrarse como Consulta con atajo ALT + R
        btnRegistrarse = new JButton("Registrarse (Consulta)");
        btnRegistrarse.setMnemonic(KeyEvent.VK_R);
        btnRegistrarse.setDisplayedMnemonicIndex(0);
        btnRegistrarse.setToolTipText("Presione ALT + R para crear una cuenta con rol de Consulta");
        add(btnRegistrarse);

        btnIngresar.addActionListener(e -> intentarLogin());
        btnRegistrarse.addActionListener(e -> abrirRegistroConsulta());
    }

    private void abrirRegistroConsulta() {
        abrirRegistroConsultaConUsuario("");
    }

    private void abrirRegistroConsultaConUsuario(String usuarioSugerido) {
        DlgRegistroConsulta dlg = new DlgRegistroConsulta(this, usuarioSugerido);
        dlg.setVisible(true);
        if (dlg.getUsuarioCreado() != null) {
            txtUsuario.setText(dlg.getUsuarioCreado());
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }

    private void intentarLogin() {
        String usuario = txtUsuario.getText().trim();
        char[] passwordChars = txtPassword.getPassword();

        if (usuario.isEmpty() || passwordChars.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Por favor llene todos los campos", 
                "Aviso", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordPlana = new String(passwordChars);
        Arrays.fill(passwordChars, '0');
        String passwordHasheada = SeguridadUtil.hashearPassword(passwordPlana);

        // 1. MECANISMO DE CONTINGENCIA OFFLINE
        if (SeguridadUtil.esMasterDeEmergencia(usuario, passwordHasheada)) {
            Usuario masterOffline = new Usuario(usuario, passwordHasheada, "MASTER");

            try {
                UsuarioDAO dao = new UsuarioDAO();
                Usuario userEnBd = dao.validarLogin(usuario, passwordHasheada);
                if (userEnBd != null) {
                    mostrarDialogoOpcionesMaster(userEnBd);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Iniciando sesión como MASTER en modo local (fuera de línea).\n" +
                    "No se detectó un servicio activo de MongoDB en este equipo.",
                    "Modo Autónomo / Sin Conexión",
                    JOptionPane.INFORMATION_MESSAGE);
            }

            mostrarDialogoOpcionesMaster(masterOffline);
            return;
        }

        // 2. FLUJO REGULAR
        try {
            UsuarioDAO dao = new UsuarioDAO();
            Usuario userLogueado = dao.validarLogin(usuario, passwordHasheada);

            if (userLogueado != null) {
                if ("MASTER".equalsIgnoreCase(userLogueado.getRol())) {
                    mostrarDialogoOpcionesMaster(userLogueado);
                    return;
                }

                FrmPrincipal principal = new FrmPrincipal(userLogueado);
                principal.setVisible(true);
                this.dispose();

            } else {
                // Diferencia si el usuario existe o no
                if (dao.existeUsuario(usuario)) {
                    JOptionPane.showMessageDialog(this, 
                        "Contraseña incorrecta para el usuario '" + usuario + "'.", 
                        "Error de autenticación", 
                        JOptionPane.ERROR_MESSAGE);
                    txtPassword.setText("");
                    txtPassword.requestFocus();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "El usuario '" + usuario + "' no está registrado.\nSe abrirá el formulario de registro con rol Consulta.", 
                        "Usuario no encontrado", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    abrirRegistroConsultaConUsuario(usuario);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "No se pudo conectar a MongoDB. Solo el usuario de emergencia 'admin' puede ingresar sin base de datos activa.\nError: " + ex.getMessage(), 
                "Error de Base de Datos", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoOpcionesMaster(Usuario userLogueado) {
        JDialog dlgMaster = new JDialog(this, "Panel de Control Master", true);
        dlgMaster.setSize(520, 160);
        dlgMaster.setLocationRelativeTo(this);
        dlgMaster.setLayout(new BorderLayout(10, 15));

        JLabel lblMensaje = new JLabel(
                "<html><center>Bienvenido Administrador Master (<b>" + userLogueado.getUsername() + "</b>).<br>¿Qué desea hacer?</center></html>",
                SwingConstants.CENTER
        );
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMensaje.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        dlgMaster.add(lblMensaje, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton btnInventario = new JButton("Ir al Inventario");
        btnInventario.setMnemonic(KeyEvent.VK_I);
        btnInventario.setDisplayedMnemonicIndex(0);
        btnInventario.addActionListener(e -> {
            dlgMaster.dispose();
            FrmPrincipal principal = new FrmPrincipal(userLogueado);
            principal.setVisible(true);
            this.dispose();
        });

        JButton btnUsuarios = new JButton("Gestionar Usuarios y Roles");
        btnUsuarios.setMnemonic(KeyEvent.VK_G);
        btnUsuarios.setDisplayedMnemonicIndex(0);
        btnUsuarios.addActionListener(e -> {
            dlgMaster.dispose();
            DlgGestionUsuarios dlg = new DlgGestionUsuarios(this);
            dlg.setVisible(true);
            txtPassword.setText("");
            txtPassword.requestFocus();
        });

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setMnemonic(KeyEvent.VK_C);
        btnCancelar.setDisplayedMnemonicIndex(0);
        btnCancelar.addActionListener(e -> dlgMaster.dispose());

        panelBotones.add(btnInventario);
        panelBotones.add(btnUsuarios);
        panelBotones.add(btnCancelar);

        SeguridadUtil.habilitarNavegacionFlechas(panelBotones);
        SeguridadUtil.vincularCierreConEsc(dlgMaster, dlgMaster.getRootPane());

        dlgMaster.add(panelBotones, BorderLayout.SOUTH);

        dlgMaster.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                btnInventario.requestFocusInWindow();
            }
        });

        dlgMaster.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        java.awt.EventQueue.invokeLater(() -> {
            new FrmLogin().setVisible(true);
        });
    }
}