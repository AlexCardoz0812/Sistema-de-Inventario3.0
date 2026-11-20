package com.inventario.app.vistas;

import com.inventario.app.dao.MovimientoDAO;
import com.inventario.app.modelos.Movimiento;
import com.inventario.app.utilidades.SeguridadUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DlgHistorialMovimientos extends JDialog {

    private final MovimientoDAO movimientoDAO;
    private List<Movimiento> movimientosOriginales;
    private JTextField txtFiltro;
    private JTable tablaMovimientos;
    private DefaultTableModel modeloTabla;
    private Timer timerDebounce;

    public DlgHistorialMovimientos(Frame owner) {
        super(owner, "Historial de Movimientos (Kardex)", true);
        this.movimientoDAO = new MovimientoDAO();
        this.movimientosOriginales = new ArrayList<>();
        configurarVentana();
        cargarMovimientos();
    }

    private void configurarVentana() {
        setSize(900, 480);
        SeguridadUtil.aplicarIcono(this);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        SeguridadUtil.vincularCierreConEsc(this, getRootPane());

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Filtro de Movimientos"));

        JLabel lblFiltro = new JLabel("Buscar (Código, Producto, Tipo, Motivo o Usuario):");
        lblFiltro.setDisplayedMnemonic(KeyEvent.VK_B);
        lblFiltro.setDisplayedMnemonicIndex(0);
        panelSuperior.add(lblFiltro);

        txtFiltro = new JTextField(34);
        txtFiltro.setToolTipText("Filtra automáticamente mientras escribes (Ctrl+F)");
        lblFiltro.setLabelFor(txtFiltro);
        panelSuperior.add(txtFiltro);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "enfocarFiltroKardex");
        getRootPane().getActionMap().put("enfocarFiltroKardex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtFiltro.requestFocusInWindow();
                txtFiltro.selectAll();
            }
        });

        add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Fecha", "Código", "Producto", "Tipo", "Cantidad", "Stock Ant.", "Stock Act.", "Usuario", "Motivo / Detalle"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaMovimientos = new JTable(modeloTabla);
        tablaMovimientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tablaMovimientos), BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setMnemonic(KeyEvent.VK_C);
        btnCerrar.setDisplayedMnemonicIndex(0);
        btnCerrar.addActionListener(e -> dispose());
        panelSur.add(btnCerrar);

        SeguridadUtil.habilitarNavegacionFlechas(panelSur);
        add(panelSur, BorderLayout.SOUTH);

        timerDebounce = new Timer(220, e -> aplicarFiltro());
        timerDebounce.setRepeats(false);

        txtFiltro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                timerDebounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                timerDebounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                timerDebounce.restart();
            }
        });
    }

    private void cargarMovimientos() {
        try {
            List<Movimiento> lista = movimientoDAO.listarTodos();
            movimientosOriginales = lista != null ? lista : new ArrayList<>();
        } catch (Exception ex) {
            movimientosOriginales = new ArrayList<>();
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo cargar el historial de movimientos:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String consulta = txtFiltro.getText() == null ? "" : txtFiltro.getText().trim().toLowerCase();
        modeloTabla.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (Movimiento m : movimientosOriginales) {
            Object rawFecha = invocarGetter(m, "getFecha");
            String fechaTexto = "";
            if (rawFecha instanceof Date) {
                fechaTexto = sdf.format((Date) rawFecha);
            } else if (rawFecha != null) {
                fechaTexto = String.valueOf(rawFecha);
            }

            String codigo = valorTexto(invocarGetter(m, "getCodigoProducto", "getCodigo"));
            String producto = valorTexto(invocarGetter(m, "getNombreProducto", "getNombre"));
            String tipo = valorTexto(invocarGetter(m, "getTipo", "getTipoMovimiento"));
            String cantidad = valorTexto(invocarGetter(m, "getCantidad"));
            String stockAnt = valorTexto(invocarGetter(m, "getStockAnterior"));
            String stockAct = valorTexto(invocarGetter(m, "getStockNuevo", "getStockActual", "getNuevoStock"));
            String usuario = valorTexto(invocarGetter(m, "getUsuario"));
            String motivo = valorTexto(invocarGetter(m, "getMotivo", "getDetalle"));

            String combinado = (fechaTexto + " " + codigo + " " + producto + " " + tipo + " "
                    + cantidad + " " + stockAnt + " " + stockAct + " " + usuario + " " + motivo).toLowerCase();

            if (consulta.isEmpty() || combinado.contains(consulta)) {
                modeloTabla.addRow(new Object[]{
                        fechaTexto, codigo, producto, tipo, cantidad, stockAnt, stockAct, usuario, motivo
                });
            }
        }
    }

    private Object invocarGetter(Object obj, String... nombresMetodo) {
        if (obj == null) return null;
        for (String nombre : nombresMetodo) {
            try {
                Method metodo = obj.getClass().getMethod(nombre);
                return metodo.invoke(obj);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String valorTexto(Object val) {
        return val == null ? "" : String.valueOf(val);
    }
}