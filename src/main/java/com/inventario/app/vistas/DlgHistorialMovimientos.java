package com.inventario.app.vistas;

import com.inventario.app.modelos.Movimiento;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.servicio.MovimientoServicio;
import com.inventario.app.utilidades.SeguridadUtil;
import com.inventario.app.utilidades.SesionUsuario;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DlgHistorialMovimientos extends JDialog {

    private final MovimientoServicio movimientoServicio;
    private List<Movimiento> listaCompleta;
    private JTable tablaHistorial;
    private DefaultTableModel modeloTabla;
    private JComboBox<String> cmbTipo;
    private JTextField txtFiltro;
    private JButton btnExportarCSV;
    private Timer timerBusqueda;

    public DlgHistorialMovimientos(Frame owner) {
        super(owner, "Historial de Movimientos (Kardex y Auditoría)", true);
        this.movimientoServicio = new MovimientoServicio();
        this.listaCompleta = new ArrayList<>();
        configurarVentana();
        aplicarPermisosExportacion();
        cargarDatos();
        SwingUtilities.invokeLater(() -> txtFiltro.requestFocusInWindow());
    }

    private boolean esUsuarioPrivilegiado() {
        String rol = null;
        if (getOwner() instanceof FrmPrincipal) {
            Usuario u = ((FrmPrincipal) getOwner()).getUsuarioAutenticado();
            if (u != null) {
                rol = u.getRol();
            }
        }
        if (rol == null || rol.trim().isEmpty()) {
            rol = SesionUsuario.getRolActual();
        }
        return "MASTER".equalsIgnoreCase(rol) || "ADMIN".equalsIgnoreCase(rol);
    }

    private void aplicarPermisosExportacion() {
        boolean autorizado = esUsuarioPrivilegiado();
        btnExportarCSV.setEnabled(autorizado);
        if (!autorizado) {
            btnExportarCSV.setToolTipText("Acceso restringido: Solo usuarios MASTER o ADMIN pueden exportar el Kardex");
        } else {
            btnExportarCSV.setToolTipText("Exportar movimientos filtrados a Excel (.csv)");
        }
    }

    private void configurarVentana() {
        setSize(980, 520);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        SeguridadUtil.vincularCierreConEsc(this, this.getRootPane());

        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "enfocarBuscarKardex");
        actionMap.put("enfocarBuscarKardex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtFiltro.requestFocusInWindow();
                txtFiltro.selectAll();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "enfocarTipoKardex");
        actionMap.put("enfocarTipoKardex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmbTipo.requestFocusInWindow();
                cmbTipo.showPopup();
            }
        });

        timerBusqueda = new Timer(250, e -> aplicarFiltros());
        timerBusqueda.setRepeats(false);

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelFiltros.setBorder(BorderFactory.createTitledBorder("Filtros de Auditoría (Búsqueda en Tiempo Real)"));

        JLabel lblTipo = new JLabel("Tipo:");
        lblTipo.setDisplayedMnemonic(KeyEvent.VK_T);
        lblTipo.setDisplayedMnemonicIndex(0);
        panelFiltros.add(lblTipo);

        cmbTipo = new JComboBox<>(new String[]{
                "TODOS", "SALDO_INICIAL", "ENTRADA", "SALIDA", "CREACION", "MODIFICACION", "ELIMINACION"
        });
        cmbTipo.setToolTipText("Filtrar por tipo de movimiento (Alt+T o Ctrl+K)");
        cmbTipo.addActionListener(e -> aplicarFiltros());
        lblTipo.setLabelFor(cmbTipo);
        panelFiltros.add(cmbTipo);

        JLabel lblBuscar = new JLabel("Buscar (Código / Producto / Usuario):");
        lblBuscar.setDisplayedMnemonic(KeyEvent.VK_B);
        lblBuscar.setDisplayedMnemonicIndex(0);
        panelFiltros.add(lblBuscar);

        txtFiltro = new JTextField(22);
        txtFiltro.setToolTipText("Buscar en tiempo real (Alt+B o Ctrl+F)");
        lblBuscar.setLabelFor(txtFiltro);
        txtFiltro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { timerBusqueda.restart(); }
            @Override
            public void removeUpdate(DocumentEvent e) { timerBusqueda.restart(); }
            @Override
            public void changedUpdate(DocumentEvent e) { timerBusqueda.restart(); }
        });
        panelFiltros.add(txtFiltro);

        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setMnemonic(KeyEvent.VK_L);
        btnLimpiar.addActionListener(e -> {
            cmbTipo.setSelectedItem("TODOS");
            txtFiltro.setText("");
            txtFiltro.requestFocusInWindow();
            aplicarFiltros();
        });
        panelFiltros.add(btnLimpiar);

        JButton btnRefrescar = new JButton("Actualizar");
        btnRefrescar.setMnemonic(KeyEvent.VK_A);
        btnRefrescar.addActionListener(e -> cargarDatos());
        panelFiltros.add(btnRefrescar);

        add(panelFiltros, BorderLayout.NORTH);

        String[] columnas = {
                "Fecha y Hora", "Tipo", "Código", "Producto",
                "Cant.", "Stock Ant.", "Stock Nuevo", "Usuario", "Motivo"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaHistorial = new JTable(modeloTabla);
        tablaHistorial.getColumnModel().getColumn(0).setPreferredWidth(130);
        tablaHistorial.getColumnModel().getColumn(1).setPreferredWidth(105);
        tablaHistorial.getColumnModel().getColumn(2).setPreferredWidth(80);
        tablaHistorial.getColumnModel().getColumn(3).setPreferredWidth(160);
        tablaHistorial.getColumnModel().getColumn(4).setPreferredWidth(55);
        tablaHistorial.getColumnModel().getColumn(5).setPreferredWidth(70);
        tablaHistorial.getColumnModel().getColumn(6).setPreferredWidth(80);
        tablaHistorial.getColumnModel().getColumn(7).setPreferredWidth(90);
        tablaHistorial.getColumnModel().getColumn(8).setPreferredWidth(180);

        add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        btnExportarCSV = new JButton("Exportar Kardex (.csv)");
        btnExportarCSV.setMnemonic(KeyEvent.VK_E);
        btnExportarCSV.addActionListener(e -> exportarACSV());
        panelSur.add(btnExportarCSV);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setMnemonic(KeyEvent.VK_C);
        btnCerrar.addActionListener(e -> dispose());
        panelSur.add(btnCerrar);

        SeguridadUtil.habilitarNavegacionFlechas(panelSur);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        try {
            listaCompleta = movimientoServicio.obtenerHistorialCompleto();
            aplicarFiltros();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar el historial: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltros() {
        String tipoSel = (String) cmbTipo.getSelectedItem();
        String texto = txtFiltro.getText().trim().toLowerCase();

        modeloTabla.setRowCount(0);
        for (Movimiento m : listaCompleta) {
            boolean coincideTipo = "TODOS".equals(tipoSel) || tipoSel.equalsIgnoreCase(m.getTipo());
            boolean coincideTexto = texto.isEmpty()
                    || (m.getCodigoProducto() != null && m.getCodigoProducto().toLowerCase().contains(texto))
                    || (m.getNombreProducto() != null && m.getNombreProducto().toLowerCase().contains(texto))
                    || (m.getUsuario() != null && m.getUsuario().toLowerCase().contains(texto));

            if (coincideTipo && coincideTexto) {
                modeloTabla.addRow(new Object[]{
                        m.getFechaHora(),
                        m.getTipo(),
                        m.getCodigoProducto(),
                        m.getNombreProducto(),
                        m.getCantidad(),
                        m.getStockAnterior(),
                        m.getStockNuevo(),
                        m.getUsuario(),
                        m.getMotivo()
                });
            }
        }
    }

    private void exportarACSV() {
        if (!esUsuarioPrivilegiado()) {
            JOptionPane.showMessageDialog(this, "Acceso denegado: Solo usuarios MASTER o ADMIN pueden exportar el Kardex.", "Seguridad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (modeloTabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay registros visibles para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar Historial Kardex");
        chooser.setSelectedFile(new File("Historial_Kardex.csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getAbsolutePath() + ".csv");
            }
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
                pw.write('\ufeff');
                pw.println("Fecha y Hora;Tipo;Codigo;Producto;Cantidad;Stock Anterior;Stock Nuevo;Usuario;Motivo");
                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    StringBuilder fila = new StringBuilder();
                    for (int j = 0; j < modeloTabla.getColumnCount(); j++) {
                        String val = String.valueOf(modeloTabla.getValueAt(i, j)).replace(";", ",");
                        fila.append(val);
                        if (j < modeloTabla.getColumnCount() - 1) fila.append(";");
                    }
                    pw.println(fila);
                }
                JOptionPane.showMessageDialog(this, "Reporte exportado en:\n" + archivo.getAbsolutePath(), "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}