package com.inventario.app.vistas;

import com.inventario.app.utilidades.LicenciaUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class DlgActivacionLicencia extends JDialog {

    private boolean activadoExitosamente = false;
    private JTextField txtLlave;

    public DlgActivacionLicencia(Frame owner, LicenciaUtil.ResultadoVerificacion estadoActual, boolean bloqueoObligatorio) {
        super(owner, "Activación de Licencia — Sistema de Inventario", true);
        configurarVentana(estadoActual, bloqueoObligatorio);
    }

    public boolean isActivadoExitosamente() {
        return activadoExitosamente;
    }

    private void configurarVentana(LicenciaUtil.ResultadoVerificacion info, boolean bloqueoObligatorio) {
        setSize(520, 340);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setDefaultCloseOperation(bloqueoObligatorio ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        configurarAtajoEscape(bloqueoObligatorio);

        JPanel panelNorte = new JPanel(new BorderLayout(6, 6));
        panelNorte.setBorder(BorderFactory.createEmptyBorder(15, 18, 5, 18));

        JLabel lblTitulo = new JLabel();
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel lblSubtitulo = new JLabel();
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        switch (info.getEstado()) {
            case ACTIVADA:
                lblTitulo.setText("✔ Licencia Comercial Activada");
                lblTitulo.setForeground(new Color(22, 101, 52));
                lblSubtitulo.setText("Este equipo cuenta con una licencia permanente activa.");
                break;
            case PRUEBA_VIGENTE:
                lblTitulo.setText("Versión de Prueba (" + info.getDiasRestantes() + " días restantes)");
                lblTitulo.setForeground(new Color(30, 64, 175));
                lblSubtitulo.setText("Envíe su ID de Equipo al proveedor para adquirir su llave permanente.");
                break;
            case RELOJ_MANIPULADO:
                lblTitulo.setText("⚠ Error de Validación de Fecha del Sistema");
                lblTitulo.setForeground(new Color(185, 28, 28));
                lblSubtitulo.setText("Se detectó un cambio en el reloj de Windows. Corrija la fecha o active su licencia.");
                break;
            default:
                lblTitulo.setText("Período de Prueba de 30 Días Finalizado");
                lblTitulo.setForeground(new Color(185, 28, 28));
                lblSubtitulo.setText("Ingrese su llave de activación para continuar utilizando el sistema.");
                break;
        }

        panelNorte.add(lblTitulo, BorderLayout.NORTH);
        panelNorte.add(lblSubtitulo, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridBagLayout());
        panelCentro.setBorder(BorderFactory.createEmptyBorder(5, 18, 5, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panelCentro.add(new JLabel("1. Copie su ID de Equipo y envíelo para generar su clave:"), gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 1.0;
        JTextField txtHardwareId = new JTextField(info.getHardwareId());
        txtHardwareId.setEditable(false);
        txtHardwareId.setFont(new Font("Consolas", Font.BOLD, 14));
        txtHardwareId.setBackground(new Color(243, 244, 246));
        panelCentro.add(txtHardwareId, gbc);

        gbc.gridx = 1; gbc.weightx = 0.0;
        JButton btnCopiar = new JButton("Copiar ID");
        btnCopiar.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(info.getHardwareId()), null);
            JOptionPane.showMessageDialog(this, "ID de Equipo copiado al portapapeles.", "Copiado", JOptionPane.INFORMATION_MESSAGE);
        });
        panelCentro.add(btnCopiar, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panelCentro.add(new JLabel("2. Ingrese su Llave de Activación (Ej. ALEX-XXXX-XXXX-XXXX):"), gbc);

        gbc.gridy = 3;
        txtLlave = new JTextField();
        txtLlave.setFont(new Font("Consolas", Font.BOLD, 14));
        txtLlave.setEnabled(info.getEstado() != LicenciaUtil.EstadoLicencia.ACTIVADA);
        panelCentro.add(txtLlave, gbc);

        add(panelCentro, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));

        JButton btnActivar = new JButton("Activar Licencia");
        btnActivar.setBackground(new Color(22, 163, 74));
        btnActivar.setForeground(Color.WHITE);
        btnActivar.setEnabled(info.getEstado() != LicenciaUtil.EstadoLicencia.ACTIVADA);
        btnActivar.addActionListener(e -> procesarActivacion());
        panelSur.add(btnActivar);

        JButton btnSecundario = new JButton(bloqueoObligatorio ? "Salir del Sistema" : "Continuar Prueba");
        btnSecundario.addActionListener(e -> ejecutarAccionSalida(bloqueoObligatorio));
        panelSur.add(btnSecundario);

        add(panelSur, BorderLayout.SOUTH);
    }

    private void configurarAtajoEscape(boolean bloqueoObligatorio) {
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "salirConEsc");
        this.getRootPane().getActionMap().put("salirConEsc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarAccionSalida(bloqueoObligatorio);
            }
        });
    }

    private void ejecutarAccionSalida(boolean bloqueoObligatorio) {
        if (bloqueoObligatorio && !activadoExitosamente) {
            System.exit(0);
        } else {
            dispose();
        }
    }

    private void procesarActivacion() {
        String llave = txtLlave.getText().trim();
        if (LicenciaUtil.activarLicencia(llave)) {
            this.activadoExitosamente = true;
            JOptionPane.showMessageDialog(
                    this,
                    "¡Activación exitosa!\nEl sistema ahora opera en Edición Comercial Permanente.",
                    "Licencia Activada",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "La llave ingresada no es válida para este ID de Equipo.",
                    "Clave Inválida",
                    JOptionPane.ERROR_MESSAGE
            );
            txtLlave.selectAll();
            txtLlave.requestFocus();
        }
    }
}