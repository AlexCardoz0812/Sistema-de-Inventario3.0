package com.inventario.app.vistas;

import com.inventario.app.dao.UsuarioDAO;
import com.inventario.app.modelos.Usuario;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class FrmLogin extends javax.swing.JFrame {

    //Declaracion de componentes
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    
    public FrmLogin() {
        //contruyendo la ventana
        configurarVentana();
        this.setLocationRelativeTo(null); //centrar en pantalla
    }
    
    private void configurarVentana() {
        //configuracion basica de ventana
        setTitle("Iniciar Sesión - Sistema de Inventario");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10)); //4filas, 1 columna, espaciado
        getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        
        // Campo Usuario
        JPanel panelUsuario = new JPanel(new BorderLayout());
        panelUsuario.add(new JLabel("Usuario: "), BorderLayout.NORTH);
        txtUsuario = new JTextField();
        panelUsuario.add(txtUsuario, BorderLayout.CENTER);
        add(panelUsuario);
        
        //Campo contraseña
        JPanel panelPassword = new JPanel(new BorderLayout());
        panelPassword.add(new JLabel("Contraseña: "), BorderLayout.NORTH);
        txtPassword = new JPasswordField();
        panelPassword.add(txtPassword, BorderLayout.CENTER);
        add(panelPassword);
        
        //Boton
        btnIngresar = new JButton("Iniciar Sesión");
        add(btnIngresar);
        
        //Evento del boton
        btnIngresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intentarLogin();
            }
        });
        
    }
    
    private void intentarLogin() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());
        
        //validacion de campos vacios
        if(usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor llene todos los campos", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        //Llamada al DAO que va a Mongo
        UsuarioDAO dao = new UsuarioDAO();
        Usuario userLogueado = dao.validarLogin(usuario, password);
        
        if(userLogueado != null) {
            JOptionPane.showMessageDialog(this, "Bienvenido " + userLogueado.getUsername() + "!");
            
            //Abrimos ventana principal y cerramos el login
            FrmPrincipal principal = new FrmPrincipal();
            principal.setVisible(true);
            this.dispose();
            
        }else {
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrecta", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
