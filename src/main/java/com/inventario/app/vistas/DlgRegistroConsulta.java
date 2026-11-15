package com.inventario.app.vistas;

import com.inventario.app.dao.UsuarioDAO;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.utilidades.SeguridadUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class DlgRegistroConsulta extends JDialog {

    private JTextField txtNuevoUsuario;
    private JPasswordField txtNuevaPassword;
    private JPasswordField txtConfirmarPassword;
    private JButton btnRegistrar;
    private JButton btnCancelar;
    private String usuarioCreado = null;

    // Constructor estándar (cuando se abre desde el botón "Registrarse (Consulta)")
    public DlgRegistroConsulta(Frame parent) {
        this(parent, "");
    }

    // Constructor con usuario prellenado (cuando se dispara automáticamente desde FrmLogin)
    public DlgRegistroConsulta(Frame parent, String usuarioInicial) {
        super(parent, "Registro de Usuario - Consulta", true);
        configurarVentana();
        setLocationRelativeTo(parent);

        if (usuarioInicial != null && !usuarioInicial.trim().isEmpty()) {
            txtNuevoUsuario.setText(usuarioInicial.trim());
            // Enfoca directo la contraseña al abrirse la ventana
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    txtNuevaPassword.requestFocusInWindow();
                }
            });
        }

        // Accesibilidad completa por teclado
        getRootPane().setDefaultButton(btnRegistrar);
        SeguridadUtil.vincularCierreConEsc(this, getRootPane());
        SeguridadUtil.configurarNavegacionVertical(
                txtNuevoUsuario, txtNuevaPassword, txtConfirmarPassword, btnRegistrar
        );
    }

    private void configurarVentana() {
        setSize(380, 310);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JLabel lblInfo = new JLabel("<html><i>Su cuenta se creará automáticamente con rol de <b>Consulta</b> (solo lectura).</i></html>");
        add(lblInfo, BorderLayout.NORTH);

        JPanel panelCampos = new JPanel(new GridLayout(3, 1, 8, 8));

        // Usuario
        JPanel pUser = new JPanel(new BorderLayout());
        pUser.add(new JLabel("Nombre de usuario:"), BorderLayout.NORTH);
        txtNuevoUsuario = new JTextField();
        pUser.add(txtNuevoUsuario, BorderLayout.CENTER);
        panelCampos.add(pUser);

        // Contraseña
        JPanel pPass = new JPanel(new BorderLayout());
        pPass.add(new JLabel("Contraseña:"), BorderLayout.NORTH);
        txtNuevaPassword = new JPasswordField();
        pPass.add(txtNuevaPassword, BorderLayout.CENTER);
        panelCampos.add(pPass);

        // Confirmar Contraseña
        JPanel pConfirm = new JPanel(new BorderLayout());
        pConfirm.add(new JLabel("Confirmar contraseña:"), BorderLayout.NORTH);
        txtConfirmarPassword = new JPasswordField();
        pConfirm.add(txtConfirmarPassword, BorderLayout.CENTER);
        panelCampos.add(pConfirm);

        add(panelCampos, BorderLayout.CENTER);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        btnRegistrar = new JButton("Crear Cuenta");
        btnRegistrar.setMnemonic(KeyEvent.VK_C);
        btnRegistrar.setDisplayedMnemonicIndex(0);
        btnRegistrar.addActionListener(e -> procesarRegistro());

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setMnemonic(KeyEvent.VK_A);
        btnCancelar.setDisplayedMnemonicIndex(1);
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnRegistrar);
        panelBotones.add(btnCancelar);

        SeguridadUtil.habilitarNavegacionFlechas(panelBotones);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void procesarRegistro() {
        String user = txtNuevoUsuario.getText().trim();
        char[] pass1 = txtNuevaPassword.getPassword();
        char[] pass2 = txtConfirmarPassword.getPassword();

        if (user.isEmpty() || pass1.length == 0 || pass2.length == 0) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (user.equalsIgnoreCase(SeguridadUtil.MASTER_DEFAULT_USER)) {
            JOptionPane.showMessageDialog(this, "Ese nombre de usuario está reservado.", "Error", JOptionPane.ERROR_MESSAGE);
            txtNuevoUsuario.requestFocus();
            return;
        }

        if (!Arrays.equals(pass1, pass2)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            txtConfirmarPassword.setText("");
            txtConfirmarPassword.requestFocus();
            return;
        }

        String passPlana = new String(pass1);
        Arrays.fill(pass1, '0');
        Arrays.fill(pass2, '0');

        UsuarioDAO dao = new UsuarioDAO();

        if (dao.existeUsuario(user)) {
            JOptionPane.showMessageDialog(this, "El usuario '" + user + "' ya está registrado. Elija otro.", "Usuario duplicado", JOptionPane.WARNING_MESSAGE);
            txtNuevoUsuario.requestFocus();
            return;
        }

        String passHash = SeguridadUtil.hashearPassword(passPlana);
        // ROL ASIGNADO DIRECTAMENTE: "CONSULTA"
        Usuario nuevoConsulta = new Usuario(user, passHash, "CONSULTA");

        if (dao.crearUsuario(nuevoConsulta)) {
            this.usuarioCreado = user;

            JOptionPane.showMessageDialog(this, 
                    "Cuenta con rol Consulta creada exitosamente.\nYa puede iniciar sesión.", 
                    "Registro Completado", 
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                    "No se pudo guardar el usuario. Verifique que el servicio de MongoDB esté activo.", 
                    "Error de Conexión", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getUsuarioCreado() {
        return usuarioCreado;
    }
}