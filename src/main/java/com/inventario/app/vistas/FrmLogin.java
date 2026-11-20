package com.inventario.app.vistas;

import com.inventario.app.dao.UsuarioDAO;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.utilidades.SeguridadUtil;
import com.inventario.app.utilidades.SesionUsuario;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class FrmLogin extends JDialog {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnRegistrarse;

    public FrmLogin() {
        super(crearFrameSoporte(), "Iniciar Sesión - Sistema de Gestión de Inventario", false);
        configurarVentana();
        this.setLocationRelativeTo(null);
        this.getRootPane().setDefaultButton(this.btnIngresar);
        SeguridadUtil.configurarNavegacionVertical(txtUsuario, txtPassword, btnIngresar, btnRegistrarse);
    }

    private static JFrame crearFrameSoporte() {
        JFrame soporte = new JFrame("Sistema de Inventario");
        SeguridadUtil.aplicarIcono(soporte);
        soporte.setUndecorated(true);
        soporte.setSize(0, 0);
        soporte.setLocationRelativeTo(null);
        soporte.setVisible(true);
        return soporte;
    }

    private void configurarVentana() {
        SeguridadUtil.aplicarIcono(this);
        setSize(370, 315);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Interceptar clic en la X para pedir confirmación antes de salir
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmarSalidaSistema();
            }
        });

        // Interceptar tecla ESC para pedir la misma confirmación
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "confirmarSalida");
        this.getRootPane().getActionMap().put("confirmarSalida", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmarSalidaSistema();
            }
        });

        setLayout(new BorderLayout(0, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 40, 12, 40));

        JPanel panelFormulario = new JPanel(new GridLayout(4, 1, 10, 10));

        JPanel panelUsuario = new JPanel(new BorderLayout());
        panelUsuario.add(new JLabel("Usuario: "), BorderLayout.NORTH);
        txtUsuario = new JTextField();
        ((AbstractDocument) txtUsuario.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string != null) {
                    string = string.toUpperCase();
                }
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text != null) {
                    text = text.toUpperCase();
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
        panelUsuario.add(txtUsuario, BorderLayout.CENTER);
        panelFormulario.add(panelUsuario);

        JPanel panelPassword = new JPanel(new BorderLayout());
        panelPassword.add(new JLabel("Contraseña: "), BorderLayout.NORTH);
        txtPassword = new JPasswordField();
        panelPassword.add(txtPassword, BorderLayout.CENTER);
        panelFormulario.add(panelPassword);

        btnIngresar = new JButton("Iniciar Sesión");
        btnIngresar.setMnemonic(KeyEvent.VK_I);
        btnIngresar.setDisplayedMnemonicIndex(0);
        btnIngresar.setToolTipText("Presione ALT + I o ENTER para ingresar");
        panelFormulario.add(btnIngresar);

        btnRegistrarse = new JButton("Registrarse (Consulta)");
        btnRegistrarse.setMnemonic(KeyEvent.VK_R);
        btnRegistrarse.setDisplayedMnemonicIndex(0);
        btnRegistrarse.setToolTipText("Presione ALT + R para crear una cuenta");
        panelFormulario.add(btnRegistrarse);

        add(panelFormulario, BorderLayout.CENTER);

        JLabel lblVersion = new JLabel("v2.0.0 | © 2026 Alex Software", SwingConstants.CENTER);
        lblVersion.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblVersion.setForeground(new Color(120, 120, 120));
        add(lblVersion, BorderLayout.SOUTH);

        btnIngresar.addActionListener(e -> intentarLogin());
        btnRegistrarse.addActionListener(e -> abrirVentanaRegistroDirecto());
    }

    private void confirmarSalidaSistema() {
        Icon iconoRojo = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Círculo rojo de advertencia
                g2.setColor(new Color(220, 38, 38));
                g2.fillOval(x + 2, y + 2, 32, 32);

                // Signo de exclamación blanco
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x + 16, y + 9, 4, 12, 2, 2);
                g2.fillOval(x + 16, y + 23, 4, 4);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 36;
            }

            @Override
            public int getIconHeight() {
                return 36;
            }
        };

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro que desea salir del Sistema de Inventario?",
                "Confirmar Salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                iconoRojo
        );
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void abrirVentanaRegistroDirecto() {
        DlgRegistroConsulta dlg = new DlgRegistroConsulta((Frame) this.getOwner(), "");
        dlg.setVisible(true);
        if (dlg.getUsuarioCreado() != null) {
            txtUsuario.setText(dlg.getUsuarioCreado().toUpperCase());
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }

    private void intentarLogin() {
        String usuario = txtUsuario.getText().trim().toLowerCase();
        char[] passwordChars = txtPassword.getPassword();

        if (usuario.isEmpty() || passwordChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Por favor llene todos los campos", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordPlana = new String(passwordChars);
        Arrays.fill(passwordChars, '0');
        String passwordHasheada = SeguridadUtil.hashearPassword(passwordPlana);

        try {
            UsuarioDAO dao = new UsuarioDAO();
            Usuario userLogueado = dao.validarLogin(usuario, passwordHasheada);

            if (userLogueado != null) {
                SesionUsuario.iniciarSesion(userLogueado.getUsername().toUpperCase(), userLogueado.getRol());

                txtUsuario.setText("");
                txtPassword.setText("");

                if ("MASTER".equalsIgnoreCase(userLogueado.getRol())) {
                    mostrarDialogoOpcionesMaster(userLogueado);
                    return;
                }

                FrmPrincipal principal = new FrmPrincipal(userLogueado);
                principal.setVisible(true);

            } else {
                if (dao.existeUsuario(usuario)) {
                    JOptionPane.showMessageDialog(this, "Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
                    txtPassword.setText("");
                    txtPassword.requestFocus();
                } else {
                    JOptionPane.showMessageDialog(this, "El usuario '" + usuario.toUpperCase() + "' no está registrado. Se abrirá el registro.", "Usuario no encontrado", JOptionPane.INFORMATION_MESSAGE);

                    DlgRegistroConsulta dlg = new DlgRegistroConsulta((Frame) this.getOwner(), usuario);
                    dlg.setVisible(true);

                    if (dlg.getUsuarioCreado() != null) {
                        txtUsuario.setText(dlg.getUsuarioCreado().toUpperCase());
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión a MongoDB.\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoOpcionesMaster(Usuario userLogueado) {
        JDialog dlgMaster = new JDialog(this, "Panel de Control Master", true);
        SeguridadUtil.aplicarIcono(dlgMaster);
        dlgMaster.setSize(520, 160);
        dlgMaster.setLocationRelativeTo(this);
        dlgMaster.setLayout(new BorderLayout(10, 15));

        JLabel lblMensaje = new JLabel("<html><center>Bienvenido Administrador Master (<b>" + userLogueado.getUsername().toUpperCase() + "</b>).<br>¿Qué desea hacer?</center></html>", SwingConstants.CENTER);
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMensaje.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        dlgMaster.add(lblMensaje, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton btnInventario = new JButton("Ir al Inventario");
        btnInventario.setMnemonic(KeyEvent.VK_I);
        btnInventario.setDisplayedMnemonicIndex(0);
        btnInventario.addActionListener(e -> {
            SesionUsuario.iniciarSesion(userLogueado.getUsername().toUpperCase(), userLogueado.getRol());
            dlgMaster.dispose();
            FrmPrincipal principal = new FrmPrincipal(userLogueado);
            principal.setVisible(true);
        });

        JButton btnUsuarios = new JButton("Gestionar Usuarios y Roles");
        btnUsuarios.setMnemonic(KeyEvent.VK_G);
        btnUsuarios.setDisplayedMnemonicIndex(0);
        btnUsuarios.addActionListener(e -> {
            dlgMaster.dispose();
            DlgGestionUsuarios dlg = new DlgGestionUsuarios((JFrame) this.getOwner());
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

        dlgMaster.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                btnInventario.requestFocusInWindow();
            }
        });
        dlgMaster.setVisible(true);
    }
}