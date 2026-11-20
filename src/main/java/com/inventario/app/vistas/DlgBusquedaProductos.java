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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class DlgBusquedaProductos extends JDialog {

    private final List<Producto> catalogoCompleto;
    private JTextField txtTermino;
    private JCheckBox chkCodigo;
    private JCheckBox chkNombre;
    private JCheckBox chkDescripcion;
    private JCheckBox chkCategoria;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private JLabel lblContador;
    private String codigoSeleccionado = null;
    private Timer timerBusqueda;

    public DlgBusquedaProductos(Frame owner, List<Producto> productos) {
        super(owner, "Buscador Avanzado de Productos", true);
        this.catalogoCompleto = productos;
        configurarVentana();
        ejecutarBusqueda();
        SwingUtilities.invokeLater(() -> txtTermino.requestFocusInWindow());
    }

    public String getCodigoSeleccionado() {
        return codigoSeleccionado;
    }

    private void configurarVentana() {
        setSize(860, 480);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        SeguridadUtil.vincularCierreConEsc(this, this.getRootPane());

        // Atajo Ctrl+F para volver directo al cuadro de búsqueda
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "enfocarTextoBusqueda");
        this.getRootPane().getActionMap().put("enfocarTextoBusqueda", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtTermino.requestFocusInWindow();
                txtTermino.selectAll();
            }
        });

        timerBusqueda = new Timer(200, e -> ejecutarBusqueda());
        timerBusqueda.setRepeats(false);

        JPanel panelCriterios = new JPanel(new BorderLayout(8, 8));
        panelCriterios.setBorder(BorderFactory.createTitledBorder("Criterios de Búsqueda"));

        JPanel panelTexto = new JPanel(new BorderLayout(8, 0));
        panelTexto.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));

        JLabel lblBuscar = new JLabel("Texto a buscar:");
        lblBuscar.setDisplayedMnemonic(KeyEvent.VK_T);
        lblBuscar.setDisplayedMnemonicIndex(0);
        panelTexto.add(lblBuscar, BorderLayout.WEST);

        txtTermino = new JTextField();
        txtTermino.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtTermino.setToolTipText("Escriba para filtrar en tiempo real (Alt+T o Ctrl+F)");
        lblBuscar.setLabelFor(txtTermino);
        txtTermino.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { timerBusqueda.restart(); }
            @Override
            public void removeUpdate(DocumentEvent e) { timerBusqueda.restart(); }
            @Override
            public void changedUpdate(DocumentEvent e) { timerBusqueda.restart(); }
        });
        panelTexto.add(txtTermino, BorderLayout.CENTER);

        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setMnemonic(KeyEvent.VK_L);
        btnLimpiar.addActionListener(e -> {
            txtTermino.setText("");
            txtTermino.requestFocusInWindow();
            ejecutarBusqueda();
        });
        panelTexto.add(btnLimpiar, BorderLayout.EAST);

        panelCriterios.add(panelTexto, BorderLayout.NORTH);

        JPanel panelChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 4));
        panelChecks.add(new JLabel("Buscar en:"));

        chkCodigo = new JCheckBox("Código", true);
        chkCodigo.setMnemonic(KeyEvent.VK_O);
        chkCodigo.setDisplayedMnemonicIndex(1);

        chkNombre = new JCheckBox("Nombre", true);
        chkNombre.setMnemonic(KeyEvent.VK_N);
        chkNombre.setDisplayedMnemonicIndex(0);

        chkDescripcion = new JCheckBox("Descripción", false);
        chkDescripcion.setMnemonic(KeyEvent.VK_D);
        chkDescripcion.setDisplayedMnemonicIndex(0);

        chkCategoria = new JCheckBox("Categoría", false);
        chkCategoria.setMnemonic(KeyEvent.VK_G);
        chkCategoria.setDisplayedMnemonicIndex(4);

        chkCodigo.addActionListener(e -> ejecutarBusqueda());
        chkNombre.addActionListener(e -> ejecutarBusqueda());
        chkDescripcion.addActionListener(e -> ejecutarBusqueda());
        chkCategoria.addActionListener(e -> ejecutarBusqueda());

        panelChecks.add(chkCodigo);
        panelChecks.add(chkNombre);
        panelChecks.add(chkDescripcion);
        panelChecks.add(chkCategoria);

        panelCriterios.add(panelChecks, BorderLayout.SOUTH);
        SeguridadUtil.habilitarNavegacionFlechas(panelCriterios);
        add(panelCriterios, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Descripción", "Categoría", "Precio ($)", "Existencias", "Total ($)"};
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
                if (e.getClickCount() == 2) {
                    confirmarSeleccion();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tablaResultados);
        scroll.setBorder(BorderFactory.createTitledBorder("Coincidencias Encontradas"));
        add(scroll, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout(10, 0));
        panelSur.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));

        lblContador = new JLabel("Mostrando 0 producto(s)");
        lblContador.setFont(new Font("SansSerif", Font.BOLD, 12));
        panelSur.add(lblContador, BorderLayout.WEST);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnSeleccionar = new JButton("Ubicar en Catálogo");
        btnSeleccionar.setMnemonic(KeyEvent.VK_U);
        btnSeleccionar.addActionListener(e -> confirmarSeleccion());
        panelBotones.add(btnSeleccionar);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setMnemonic(KeyEvent.VK_C);
        btnCerrar.addActionListener(e -> dispose());
        panelBotones.add(btnCerrar);

        SeguridadUtil.habilitarNavegacionFlechas(panelBotones);
        panelSur.add(panelBotones, BorderLayout.EAST);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void ejecutarBusqueda() {
        String termino = txtTermino.getText().trim().toLowerCase();
        boolean bCodigo = chkCodigo.isSelected();
        boolean bNombre = chkNombre.isSelected();
        boolean bDesc = chkDescripcion.isSelected();
        boolean bCat = chkCategoria.isSelected();

        modeloTabla.setRowCount(0);

        if (!termino.isEmpty() && !bCodigo && !bNombre && !bDesc && !bCat) {
            lblContador.setText("Seleccione al menos un criterio de búsqueda (Código, Nombre, Descripción o Categoría).");
            return;
        }

        for (Producto p : catalogoCompleto) {
            boolean coincide = false;

            if (termino.isEmpty()) {
                coincide = true;
            } else {
                if (bCodigo && p.getCodigo() != null && p.getCodigo().toLowerCase().contains(termino)) {
                    coincide = true;
                }
                if (!coincide && bNombre && p.getNombre() != null && p.getNombre().toLowerCase().contains(termino)) {
                    coincide = true;
                }
                if (!coincide && bDesc && p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(termino)) {
                    coincide = true;
                }
                if (!coincide && bCat && p.getCategoria() != null && p.getCategoria().toLowerCase().contains(termino)) {
                    coincide = true;
                }
            }

            if (coincide) {
                modeloTabla.addRow(new Object[]{
                        p.getCodigo(),
                        p.getNombre(),
                        p.getDescripcion(),
                        p.getCategoria(),
                        String.format("%.2f", p.getPrecio()),
                        p.getCantidadDisponible(),
                        String.format("%.2f", p.calcularValorTotal())
                });
            }
        }

        lblContador.setText("Mostrando " + modeloTabla.getRowCount() + " de " + catalogoCompleto.size() + " producto(s)");
    }

    private void confirmarSeleccion() {
        int fila = tablaResultados.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.codigoSeleccionado = (String) modeloTabla.getValueAt(fila, 0);
        dispose();
    }
}