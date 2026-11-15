
package com.inventario.app.vistas;

import com.inventario.app.dao.UsuarioDAO;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.utilidades.SeguridadUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class DlgGestionUsuarios extends JDialog {

    // Componentes del panel lateral izquierdo (Board)
    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloLista;

    // Componentes del formulario derecho
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRoles;
    private JLabel lblRolActual;

    // Botones de acción
    private JButton btnCrear;
    private JButton btnModificar;
    private JButton btnLimpiar;
    private JButton btnEliminar;

    private final UsuarioDAO usuarioDAO;

    public DlgGestionUsuarios(JFrame padre) {
        super(padre, "Panel Master - Gestión y Asignación de Roles", true);
        this.usuarioDAO = new UsuarioDAO();
        configurarInterfaz();
        cargarListaUsuarios();
        
        // Vincula la tecla ESC para cerrar el diálogo desde cualquier elemento enfocado
        SeguridadUtil.vincularCierreConEsc(this, this.getRootPane());
    }

    private void configurarInterfaz() {
        setSize(760, 380);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(12, 12));

        // =====================================================================
        // 1. PANEL LATERAL IZQUIERDO (BOARD DE SELECCIÓN)
        // =====================================================================
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        panelIzquierdo.setPreferredSize(new Dimension(220, 0));
        panelIzquierdo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 0),
                BorderFactory.createTitledBorder("Usuarios del Sistema")
        ));

        modeloLista = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloLista);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaUsuarios.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Evento de selección al hacer clic en un usuario de la lista
        listaUsuarios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarUsuarioSeleccionado();
            }
        });

        JScrollPane scrollLista = new JScrollPane(listaUsuarios);
        panelIzquierdo.add(scrollLista, BorderLayout.CENTER);

        add(panelIzquierdo, BorderLayout.WEST);

        // =====================================================================
        // 2. PANEL CENTRAL / DERECHO (FORMULARIO DE EDICIÓN Y CREACIÓN)
        // =====================================================================
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 10, 10),
                BorderFactory.createTitledBorder("Detalle y Privilegios del Usuario")
        ));

        JPanel panelCampos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 0: Nombre de Usuario
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        panelCampos.add(new JLabel("Nombre de Usuario:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtUsername = new JTextField();
        panelCampos.add(txtUsername, gbc);

        // Fila 1: Contraseña
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        panelCampos.add(new JLabel("Contraseña:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtPassword = new JPasswordField();
        panelCampos.add(txtPassword, gbc);

        // Subetiqueta informativa sobre la contraseña al editar
        gbc.gridx = 1;
        gbc.gridy = 2;
        JLabel lblNotaPass = new JLabel("*(Al modificar: deje vacío si desea conservar la actual)");
        lblNotaPass.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblNotaPass.setForeground(new Color(110, 110, 110));
        panelCampos.add(lblNotaPass, gbc);

        // Fila 3: Rol / Privilegio
        gbc.gridx = 0;
        gbc.gridy = 3;
        panelCampos.add(new JLabel("Rol / Privilegio:"), gbc);

        gbc.gridx = 1;
        cmbRoles = new JComboBox<>(new String[]{"OPERADOR", "ADMIN", "CONSULTA", "MASTER"});
        panelCampos.add(cmbRoles, gbc);

        // Fila 4: Indicador de Rol Actual
        gbc.gridx = 0;
        gbc.gridy = 4;
        panelCampos.add(new JLabel("Estado:"), gbc);

        gbc.gridx = 1;
        lblRolActual = new JLabel("Modo: Creación de nuevo usuario");
        lblRolActual.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRolActual.setForeground(new Color(40, 120, 40));
        panelCampos.add(lblRolActual, gbc);

        panelCentral.add(panelCampos, BorderLayout.CENTER);
        add(panelCentral, BorderLayout.CENTER);

        // =====================================================================
        // 3. PANEL INFERIOR (BOTONERA CON MNEMÓNICOS Y NAVEGACIÓN DIRECCIONAL)
        // =====================================================================
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        // ALT + C -> Crear Nuevo
        btnCrear = new JButton("Crear Nuevo");
        btnCrear.setMnemonic(KeyEvent.VK_C);
        btnCrear.setDisplayedMnemonicIndex(0); // Subraya la 'C'
        btnCrear.setToolTipText("Presione ALT + C para crear");
        btnCrear.addActionListener(e -> crearUsuario());
        panelInferior.add(btnCrear);

        // ALT + G -> Guardar Cambios
        btnModificar = new JButton("Guardar Cambios");
        btnModificar.setMnemonic(KeyEvent.VK_G);
        btnModificar.setDisplayedMnemonicIndex(0); // Subraya la 'G'
        btnModificar.setToolTipText("Presione ALT + G para guardar modificaciones");
        btnModificar.setEnabled(false); // Desactivado por defecto hasta seleccionar de la lista
        btnModificar.addActionListener(e -> modificarUsuario());
        panelInferior.add(btnModificar);

        // ALT + E -> Eliminar Usuario
        btnEliminar = new JButton("Eliminar Usuario");
        btnEliminar.setMnemonic(KeyEvent.VK_E);
        btnEliminar.setDisplayedMnemonicIndex(0); // Subraya la 'E'
        btnEliminar.setToolTipText("Presione ALT + E para eliminar el usuario seleccionado");
        btnEliminar.setEnabled(false); // Desactivado hasta seleccionar un usuario
        btnEliminar.addActionListener(e -> eliminarUsuarioSeleccionado());
        panelInferior.add(btnEliminar);

        // ALT + L -> Limpiar
        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setMnemonic(KeyEvent.VK_L);
        btnLimpiar.setDisplayedMnemonicIndex(0); // Subraya la 'L'
        btnLimpiar.setToolTipText("Presione ALT + L para limpiar campos");
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        panelInferior.add(btnLimpiar);

        // ALT + R -> Cerrar
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setMnemonic(KeyEvent.VK_R);
        btnCerrar.setDisplayedMnemonicIndex(5); // Subraya la 'r' final de "Cerrar"
        btnCerrar.setToolTipText("Presione ALT + R o ESC para cerrar");
        btnCerrar.addActionListener(e -> dispose());
        panelInferior.add(btnCerrar);

        // Habilita que las flechas direccionales muevan el foco y ENTER ejecute el botón
        SeguridadUtil.habilitarNavegacionFlechas(panelInferior);

        add(panelInferior, BorderLayout.SOUTH);
    }

    // =========================================================================
    // LÓGICA DE NEGOCIO Y EVENTOS
    // =========================================================================

    private void cargarListaUsuarios() {
        modeloLista.clear();
        List<Usuario> usuarios = usuarioDAO.listarUsuarios();
        for (Usuario u : usuarios) {
            modeloLista.addElement(u.getUsername());
        }
    }

    private void cargarUsuarioSeleccionado() {
        String seleccionado = listaUsuarios.getSelectedValue();
        if (seleccionado == null) return;

        List<Usuario> usuarios = usuarioDAO.listarUsuarios();
        for (Usuario u : usuarios) {
            if (u.getUsername().equals(seleccionado)) {
                txtUsername.setText(u.getUsername());
                txtUsername.setEditable(false); // Protege la clave primaria
                txtPassword.setText("");
                cmbRoles.setSelectedItem(u.getRol());

                lblRolActual.setText("Modificando: " + u.getUsername() + " (Rol actual: " + u.getRol() + ")");
                lblRolActual.setForeground(new Color(180, 80, 0));

                btnCrear.setEnabled(false);
                btnModificar.setEnabled(true);
                // Protege la cuenta maestra principal de ser eliminada desde la UI
                btnEliminar.setEnabled(!SeguridadUtil.MASTER_DEFAULT_USER.equalsIgnoreCase(u.getUsername()));
                break;
            }
        }
    }

    private void limpiarFormulario() {
        listaUsuarios.clearSelection();
        txtUsername.setText("");
        txtUsername.setEditable(true);
        txtPassword.setText("");
        cmbRoles.setSelectedIndex(0);

        lblRolActual.setText("Modo: Creación de nuevo usuario");
        lblRolActual.setForeground(new Color(40, 120, 40));

        btnCrear.setEnabled(true);
        btnModificar.setEnabled(false);
        btnEliminar.setEnabled(false);
        txtUsername.requestFocus();
    }

    private void crearUsuario() {
        String username = txtUsername.getText().trim();
        char[] passChars = txtPassword.getPassword();
        String rol = (String) cmbRoles.getSelectedItem();

        if (username.isEmpty() || passChars.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Para registrar un usuario debe ingresar el nombre y la contraseña inicial.",
                    "Campos Incompletos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passPlana = new String(passChars);
        Arrays.fill(passChars, '0');
        String passHash = SeguridadUtil.hashearPassword(passPlana);

        Usuario nuevo = new Usuario(username, passHash, rol);
        if (usuarioDAO.crearUsuario(nuevo)) {
            JOptionPane.showMessageDialog(this,
                    "¡Usuario '" + username + "' creado exitosamente con rol " + rol + "!",
                    "Operación Exitosa",
                    JOptionPane.INFORMATION_MESSAGE);
            cargarListaUsuarios();
            limpiarFormulario();
        } else {
            JOptionPane.showMessageDialog(this,
                    "El nombre de usuario '" + username + "' ya existe en MongoDB.",
                    "Usuario Duplicado",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarUsuario() {
        String username = txtUsername.getText().trim();
        char[] passChars = txtPassword.getPassword();
        String nuevoRol = (String) cmbRoles.getSelectedItem();

        if (username.isEmpty()) {
            return;
        }

        String nuevoHash = null;
        if (passChars.length > 0) {
            String passPlana = new String(passChars);
            Arrays.fill(passChars, '0');
            nuevoHash = SeguridadUtil.hashearPassword(passPlana);
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmar actualización para el usuario '" + username + "'?\nNuevo rol: " + nuevoRol
                        + (nuevoHash != null ? "\n(Se actualizará la contraseña)" : "\n(Contraseña sin cambios)"),
                "Confirmar Modificación",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            if (usuarioDAO.actualizarUsuario(username, nuevoHash, nuevoRol)) {
                JOptionPane.showMessageDialog(this,
                        "Usuario '" + username + "' actualizado correctamente en MongoDB.",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                cargarListaUsuarios();
                limpiarFormulario();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Error al actualizar los datos en la base de datos.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarUsuarioSeleccionado() {
        String username = txtUsername.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario de la lista para eliminar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (SeguridadUtil.MASTER_DEFAULT_USER.equalsIgnoreCase(username)) {
            JOptionPane.showMessageDialog(this,
                    "El usuario administrador principal ('" + username + "') no puede ser eliminado.",
                    "Acción no permitida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de eliminar permanentemente al usuario '" + username + "'?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            if (usuarioDAO.eliminarUsuario(username)) {
                JOptionPane.showMessageDialog(this,
                        "Usuario '" + username + "' eliminado correctamente.",
                        "Operación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);
                cargarListaUsuarios();
                limpiarFormulario();
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se pudo eliminar el usuario. Verifique la conexión con MongoDB.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}