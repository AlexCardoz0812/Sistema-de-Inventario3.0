package com.inventario.app.vistas;

import com.inventario.app.modelos.Producto;
import com.inventario.app.utilidades.SeguridadUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class DlgBusquedaProductos extends JDialog {

    private final List<Producto> productosOriginales;
    private JTextField txtFiltro;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private JButton btnSeleccionar;
    private JButton btnCancelar;
    private Timer timerDebounce;
    private String codigoSeleccionado;

    public DlgBusquedaProductos(Frame owner, List<Producto> productos) {
        super(owner, "Buscar Producto", true);
        this.productosOriginales = productos != null ? productos : new ArrayList<>();
        configurarVentana();
        cargarProductos(this.productosOriginales);
    }

    public String getCodigoSeleccionado() {
        return codigoSeleccionado;
    }

    private void configurarVentana() {
        setSize(780, 460);
        SeguridadUtil.aplicarIcono(this);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        SeguridadUtil.vincularCierreConEsc(this, getRootPane());

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Búsqueda en Tiempo Real"));

        JLabel lblBuscar = new JLabel("Código, Nombre o Categoría:");
        lblBuscar.setDisplayedMnemonic(KeyEvent.VK_B);
        lblBuscar.setDisplayedMnemonicIndex(0);
        panelSuperior.add(lblBuscar);

        txtFiltro = new JTextField(32);
        txtFiltro.setToolTipText("Escribe para filtrar al instante (Ctrl+F para enfocar)");
        lblBuscar.setLabelFor(txtFiltro);
        panelSuperior.add(txtFiltro);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "enfocarFiltro");
        getRootPane().getActionMap().put("enfocarFiltro", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtFiltro.requestFocusInWindow();
                txtFiltro.selectAll();
            }
        });

        add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Descripción", "Categoría", "Precio ($)", "Existencias"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaResultados = new JTable(modeloTabla);
        tablaResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaResultados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaResultados.getSelectedRow() != -1) {
                    confirmarSeleccion();
                }
            }
        });
        tablaResultados.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && tablaResultados.getSelectedRow() != -1) {
                    e.consume();
                    confirmarSeleccion();
                }
            }
        });

        add(new JScrollPane(tablaResultados), BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        btnSeleccionar = new JButton("Seleccionar");
        btnSeleccionar.setMnemonic(KeyEvent.VK_S);
        btnSeleccionar.setDisplayedMnemonicIndex(0);
        btnSeleccionar.addActionListener(e -> confirmarSeleccion());
        panelSur.add(btnSeleccionar);

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setMnemonic(KeyEvent.VK_C);
        btnCancelar.setDisplayedMnemonicIndex(0);
        btnCancelar.addActionListener(e -> dispose());
        panelSur.add(btnCancelar);

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

        txtFiltro.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && modeloTabla.getRowCount() > 0) {
                    tablaResultados.requestFocusInWindow();
                    tablaResultados.setRowSelectionInterval(0, 0);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && modeloTabla.getRowCount() > 0) {
                    if (tablaResultados.getSelectedRow() == -1) {
                        tablaResultados.setRowSelectionInterval(0, 0);
                    }
                    confirmarSeleccion();
                }
            }
        });
    }

    private void aplicarFiltro() {
        String consulta = txtFiltro.getText() == null ? "" : txtFiltro.getText().trim().toLowerCase();
        if (consulta.isEmpty()) {
            cargarProductos(productosOriginales);
            return;
        }

        List<Producto> filtrados = new ArrayList<>();
        for (Producto p : productosOriginales) {
            String cod = p.getCodigo() != null ? p.getCodigo().toLowerCase() : "";
            String nom = p.getNombre() != null ? p.getNombre().toLowerCase() : "";
            String cat = p.getCategoria() != null ? p.getCategoria().toLowerCase() : "";
            String desc = p.getDescripcion() != null ? p.getDescripcion().toLowerCase() : "";

            if (cod.contains(consulta) || nom.contains(consulta) || cat.contains(consulta) || desc.contains(consulta)) {
                filtrados.add(p);
            }
        }
        cargarProductos(filtrados);
    }

    private void cargarProductos(List<Producto> lista) {
        modeloTabla.setRowCount(0);
        for (Producto p : lista) {
            modeloTabla.addRow(new Object[]{
                    p.getCodigo(),
                    p.getNombre(),
                    p.getDescripcion(),
                    p.getCategoria(),
                    String.format("%.2f", p.getPrecio()),
                    p.getCantidadDisponible()
            });
        }
        if (modeloTabla.getRowCount() > 0) {
            tablaResultados.setRowSelectionInterval(0, 0);
        }
    }

    private void confirmarSeleccion() {
        int fila = tablaResultados.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.codigoSeleccionado = String.valueOf(modeloTabla.getValueAt(fila, 0));
        dispose();
    }
}