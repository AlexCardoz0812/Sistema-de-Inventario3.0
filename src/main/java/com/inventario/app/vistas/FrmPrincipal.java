package com.inventario.app.vistas;

import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.excepciones.StockInsuficienteException;
import com.inventario.app.modelos.Producto;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.servicio.InventarioService;
import com.inventario.app.utilidades.LicenciaUtil;
import com.inventario.app.utilidades.SeguridadUtil;
import com.inventario.app.utilidades.SesionUsuario;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

public class FrmPrincipal extends JFrame {

    private final InventarioService servicio;
    private JTable tablaProductos;
    private JButton btnRegistrar;
    private JButton btnModificar;
    private JButton btnEliminar;
    private JButton btnEntrada;
    private JButton btnSalida;
    private JButton btnHistorial;
    private JButton btnImportarCSV;
    private JButton btnExportarCSV;
    private DefaultTableModel modeloTabla;
    private JLabel lblValorTotal;
    private JLabel lblEstadoUsuario;
    private JLabel lblTituloBarra;
    private JLabel lblCopyright;
    private JComboBox<String> cmbFiltroCategoria;
    private Usuario usuarioAutenticado;

    private static final int LIMITE_STOCK_BAJO = 5;

    private static final String[] CATEGORIAS_VALIDAS = {
            "Electrónica", "Oficina", "Abarrotes", "Limpieza", "Herramientas"
    };

    public FrmPrincipal() {
        this.servicio = new InventarioService();
        this.servicio.sincronizarSaldoInicialSiVacio();
        configurarVentana();
        cargarDatosTabla(servicio.listarProductos());
    }

    public FrmPrincipal(Usuario usuario) {
        this();
        this.usuarioAutenticado = usuario;
        if (usuario != null) {
            String userMayus = usuario.getUsername().toUpperCase();
            String rolMayus = usuario.getRol().toUpperCase();
            String titulo = "Sistema de Gestión de Inventario — " + rolMayus;
            setTitle(titulo);
            if (lblTituloBarra != null) {
                lblTituloBarra.setText("   " + titulo);
            }
            lblEstadoUsuario.setText("Operador: " + userMayus);
            aplicarPermisosPorRol();
        }
        SwingUtilities.invokeLater(this::verificarYMostrarAlertaStockBajoAutomatica);
    }

    public Usuario getUsuarioAutenticado() {
        return this.usuarioAutenticado;
    }

    private void sincronizarSesionActiva() {
        if (this.usuarioAutenticado != null) {
            SesionUsuario.iniciarSesion(
                    this.usuarioAutenticado.getUsername().toUpperCase(),
                    this.usuarioAutenticado.getRol()
            );
        }
    }

    private boolean esUsuarioPrivilegiado() {
        if (usuarioAutenticado == null) return false;
        String rol = usuarioAutenticado.getRol();
        return "MASTER".equalsIgnoreCase(rol) || "ADMIN".equalsIgnoreCase(rol);
    }

    private void configurarIconoVentana() {
        try {
            URL urlIcono = getClass().getResource("/com/inventario/app/vistas/icono.png");
            if (urlIcono == null) {
                urlIcono = getClass().getResource("/icono.png");
            }
            if (urlIcono != null) {
                Image icono = new ImageIcon(urlIcono).getImage();
                setIconImage(icono);
            }
        } catch (Exception ignored) {
        }
    }

    private void configurarVentana() {
        setUndecorated(true);
        configurarIconoVentana();
        setTitle("Sistema de Gestión de Inventario");
        setSize(980, 640);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(200, 204, 210), 1));

        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrarSesionConEsc");
        actionMap.put("cerrarSesionConEsc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cerrarSesion();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "abrirBuscadorAtajo");
        actionMap.put("abrirBuscadorAtajo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarProductoPorCodigo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "enfocarCategoriaAtajo");
        actionMap.put("enfocarCategoriaAtajo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmbFiltroCategoria.requestFocusInWindow();
                cmbFiltroCategoria.showPopup();
            }
        });

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Búsqueda y Filtros"));

        JLabel lblCategoria = new JLabel("Categoría:");
        lblCategoria.setDisplayedMnemonic(KeyEvent.VK_C);
        lblCategoria.setDisplayedMnemonicIndex(0);
        panelSuperior.add(lblCategoria);

        cmbFiltroCategoria = new JComboBox<>();
        cmbFiltroCategoria.addItem("TODAS");
        for (String cat : CATEGORIAS_VALIDAS) {
            cmbFiltroCategoria.addItem(cat);
        }
        cmbFiltroCategoria.setToolTipText("Filtrar por tipo de mercadería (Alt+C o Ctrl+K)");
        cmbFiltroCategoria.addActionListener(e -> filtrarPorCategoria());
        lblCategoria.setLabelFor(cmbFiltroCategoria);
        panelSuperior.add(cmbFiltroCategoria);

        JButton btnBuscar = new JButton("Buscar Producto");
        btnBuscar.setMnemonic(KeyEvent.VK_B);
        btnBuscar.setDisplayedMnemonicIndex(0);
        btnBuscar.setToolTipText("Abrir buscador avanzado (Alt+B o Ctrl+F)");
        btnBuscar.addActionListener(e -> buscarProductoPorCodigo());
        panelSuperior.add(btnBuscar);

        JPanel contenedorNorte = new JPanel(new BorderLayout(0, 6));
        contenedorNorte.add(crearBarraTituloPersonalizada(), BorderLayout.NORTH);
        contenedorNorte.add(panelSuperior, BorderLayout.CENTER);
        add(contenedorNorte, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Descripción", "Categoría", "Precio ($)", "Existencias", "Total ($)"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        aplicarRenderizadorStockBajo();

        JScrollPane scrollTabla = new JScrollPane(tablaProductos);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Catálogo de Productos (Resaltado rojo = Stock <= 5)"));
        add(scrollTabla, BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new GridLayout(8, 1, 8, 8));
        panelAcciones.setBorder(BorderFactory.createTitledBorder("Operaciones"));

        btnRegistrar = new JButton("Registrar Producto");
        btnRegistrar.setMnemonic(KeyEvent.VK_R);
        btnRegistrar.setDisplayedMnemonicIndex(0);
        btnRegistrar.addActionListener(e -> registrarNuevoProducto());
        panelAcciones.add(btnRegistrar);

        btnModificar = new JButton("Modificar Producto");
        btnModificar.setMnemonic(KeyEvent.VK_M);
        btnModificar.setDisplayedMnemonicIndex(0);
        btnModificar.addActionListener(e -> modificarProductoSeleccionado());
        panelAcciones.add(btnModificar);

        btnEliminar = new JButton("Eliminar Producto");
        btnEliminar.setMnemonic(KeyEvent.VK_E);
        btnEliminar.setDisplayedMnemonicIndex(0);
        btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());
        panelAcciones.add(btnEliminar);

        btnEntrada = new JButton("Entrada Stock (+)");
        btnEntrada.setMnemonic(KeyEvent.VK_N);
        btnEntrada.setDisplayedMnemonicIndex(1);
        btnEntrada.addActionListener(e -> gestionarMovimiento(true));
        panelAcciones.add(btnEntrada);

        btnSalida = new JButton("Salida Stock (-)");
        btnSalida.setMnemonic(KeyEvent.VK_S);
        btnSalida.setDisplayedMnemonicIndex(0);
        btnSalida.addActionListener(e -> gestionarMovimiento(false));
        panelAcciones.add(btnSalida);

        btnHistorial = new JButton("Historial (Kardex)");
        btnHistorial.setMnemonic(KeyEvent.VK_H);
        btnHistorial.setDisplayedMnemonicIndex(0);
        btnHistorial.addActionListener(e -> abrirHistorialMovimientos());
        panelAcciones.add(btnHistorial);

        btnImportarCSV = new JButton("Importar Inventario");
        btnImportarCSV.setMnemonic(KeyEvent.VK_P);
        btnImportarCSV.setDisplayedMnemonicIndex(2);
        btnImportarCSV.setToolTipText("Importar inventario masivamente desde Excel/CSV o descargar plantilla (Solo Master/Admin)");
        btnImportarCSV.addActionListener(e -> gestionarImportacionCSV());
        panelAcciones.add(btnImportarCSV);

        btnExportarCSV = new JButton("Exportar Inventario");
        btnExportarCSV.setMnemonic(KeyEvent.VK_X);
        btnExportarCSV.setDisplayedMnemonicIndex(1);
        btnExportarCSV.setToolTipText("Exportar todo el inventario actual de productos a Excel/CSV (Solo Master/Admin)");
        btnExportarCSV.addActionListener(e -> exportarCatalogoACSV());
        panelAcciones.add(btnExportarCSV);

        SeguridadUtil.habilitarNavegacionFlechas(panelAcciones);
        add(panelAcciones, BorderLayout.EAST);

        JPanel panelInferior = new JPanel(new BorderLayout(15, 0));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));

        lblEstadoUsuario = new JLabel("Operador: INVITADO");
        lblEstadoUsuario.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblEstadoUsuario.setForeground(new Color(90, 90, 90));
        panelInferior.add(lblEstadoUsuario, BorderLayout.WEST);

        lblCopyright = new JLabel("", SwingConstants.CENTER);
        lblCopyright.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCopyright.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCopyright.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mostrarAcercaDe();
            }
        });
        actualizarTextoPieLicencia();
        panelInferior.add(lblCopyright, BorderLayout.CENTER);

        lblValorTotal = new JLabel("Valor Total del Inventario: $0.00");
        lblValorTotal.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelInferior.add(lblValorTotal, BorderLayout.EAST);

        add(panelInferior, BorderLayout.SOUTH);
    }

    private void actualizarTextoPieLicencia() {
        LicenciaUtil.ResultadoVerificacion lic = LicenciaUtil.verificarEstado();
        if (lic.getEstado() == LicenciaUtil.EstadoLicencia.PRUEBA_VIGENTE) {
            lblCopyright.setText("v2.0.0 | Prueba: " + lic.getDiasRestantes() + " días restantes (Clic para Activar) | © 2026 Alex Software");
            lblCopyright.setForeground(new Color(29, 78, 216));
            lblCopyright.setToolTipText("Versión de prueba activa. Haz clic para ingresar tu llave comercial.");
        } else {
            lblCopyright.setText("v2.0.0 | © 2026 Alex Software. Todos los derechos reservados.");
            lblCopyright.setForeground(new Color(110, 110, 110));
            lblCopyright.setToolTipText("Licencia Comercial Activada — Haz clic para ver detalles");
        }
    }

    private void aplicarRenderizadorStockBajo() {
        Color fondoAlerta = new Color(254, 226, 226);
        Color textoAlerta = new Color(153, 27, 27);

        DefaultTableCellRenderer renderizador = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                int stock = 999;
                try {
                    Object valStock = table.getModel().getValueAt(row, 5);
                    if (valStock != null) {
                        stock = Integer.parseInt(valStock.toString().trim());
                    }
                } catch (NumberFormatException ignored) {
                }

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else if (stock <= LIMITE_STOCK_BAJO) {
                    c.setBackground(fondoAlerta);
                    c.setForeground(textoAlerta);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }

                if (stock == 0) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }

                return c;
            }
        };

        tablaProductos.setDefaultRenderer(Object.class, renderizador);
    }

    private void exportarCatalogoACSV() {
        if (!esUsuarioPrivilegiado()) {
            JOptionPane.showMessageDialog(this, "Acceso denegado: Solo usuarios MASTER o ADMIN pueden exportar el inventario.", "Seguridad", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Inventario");
        chooser.setSelectedFile(new File("inventario_" + LocalDate.now() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoDestino = chooser.getSelectedFile();
            if (!archivoDestino.getName().toLowerCase().endsWith(".csv")) {
                archivoDestino = new File(archivoDestino.getAbsolutePath() + ".csv");
            }
            try {
                int totalExportados = servicio.exportarCatalogoCSV(archivoDestino);
                JOptionPane.showMessageDialog(
                        this,
                        "Se exportaron " + totalExportados + " producto(s) exitosamente a:\n" + archivoDestino.getAbsolutePath(),
                        "Exportación Completada",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error al exportar el inventario:\n" + ex.getMessage(),
                        "Error de Exportación",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void gestionarImportacionCSV() {
        if (!esUsuarioPrivilegiado()) {
            JOptionPane.showMessageDialog(this, "Acceso denegado: Solo usuarios MASTER o ADMIN pueden importar inventarios.", "Seguridad", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object[] opciones = {"Seleccionar archivo CSV", "Descargar Plantilla CSV", "Cancelar"};
        int eleccion = JOptionPane.showOptionDialog(
                this,
                "<html><body style='width: 340px;'>"
                        + "<b>Importación Masiva de Inventario (CSV)</b><br><br>"
                        + "El archivo debe contener las columnas:<br>"
                        + "<code>Codigo ; Nombre ; Descripcion ; Categoria ; Precio ; Existencias</code><br><br>"
                        + "¿Qué desea hacer?</body></html>",
                "Importar Inventario desde Archivo",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        if (eleccion == 1) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Guardar Plantilla de Inventario");
            chooser.setSelectedFile(new File("plantilla_inventario.csv"));
            chooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File archivoDestino = chooser.getSelectedFile();
                if (!archivoDestino.getName().toLowerCase().endsWith(".csv")) {
                    archivoDestino = new File(archivoDestino.getAbsolutePath() + ".csv");
                }
                try {
                    servicio.generarPlantillaCSV(archivoDestino);
                    JOptionPane.showMessageDialog(this, "Plantilla guardada en:\n" + archivoDestino.getAbsolutePath(), "Plantilla Generada", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "No se pudo guardar la plantilla:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (eleccion == 0) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccionar Archivo CSV de Productos");
            chooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File archivoOrigen = chooser.getSelectedFile();
                try {
                    sincronizarSesionActiva();
                    InventarioService.ResultadoImportacion res = servicio.importarDesdeCSV(archivoOrigen);
                    refrescarCatalogo();

                    StringBuilder resumen = new StringBuilder();
                    resumen.append("Proceso de importación finalizado.\n\n");
                    resumen.append("• Productos importados: ").append(res.getImportados()).append("\n");
                    resumen.append("• Filas omitidas / con error: ").append(res.getOmitidos()).append("\n");

                    if (!res.getDetallesErrores().isEmpty()) {
                        resumen.append("\nDetalle de filas omitidas (primeras 10):\n");
                        int limite = Math.min(10, res.getDetallesErrores().size());
                        for (int i = 0; i < limite; i++) {
                            resumen.append(" - ").append(res.getDetallesErrores().get(i)).append("\n");
                        }
                    }

                    JOptionPane.showMessageDialog(
                            this,
                            resumen.toString(),
                            "Resultado de Importación",
                            res.getOmitidos() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error al importar el archivo:\n" + ex.getMessage(), "Error de Importación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private JPanel crearBarraTituloPersonalizada() {
        Color fondoBarra = new Color(243, 244, 246);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(fondoBarra);
        barra.setPreferredSize(new Dimension(0, 34));
        barra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 223, 228)));

        lblTituloBarra = new JLabel("   Sistema de Gestión de Inventario");
        lblTituloBarra.setForeground(new Color(31, 41, 55));
        lblTituloBarra.setFont(new Font("Segoe UI", Font.BOLD, 12));
        barra.add(lblTituloBarra, BorderLayout.WEST);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBotones.setOpaque(false);

        JButton btnMin = crearBotonControl("—", fondoBarra, new Color(224, 228, 234), new Color(31, 41, 55), new Color(17, 24, 39), "Minimizar");
        btnMin.addActionListener(e -> setState(Frame.ICONIFIED));

        JButton btnMax = crearBotonControl("☐", fondoBarra, new Color(224, 228, 234), new Color(31, 41, 55), new Color(17, 24, 39), "Maximizar / Restaurar");
        btnMax.addActionListener(e -> {
            if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                setExtendedState(Frame.NORMAL);
            } else {
                Rectangle areaUtil = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                setMaximizedBounds(areaUtil);
                setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        });

        JButton btnCerrar = crearBotonControl("✕", fondoBarra, new Color(232, 17, 35), new Color(31, 41, 55), Color.WHITE, "Cerrar ventana");
        btnCerrar.addActionListener(e -> dispose());

        panelBotones.add(btnMin);
        panelBotones.add(btnMax);
        panelBotones.add(btnCerrar);
        barra.add(panelBotones, BorderLayout.EAST);

        final Point[] puntoInicial = {null};
        barra.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                puntoInicial[0] = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnMax.doClick();
                }
            }
        });
        barra.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (puntoInicial[0] != null && (getExtendedState() & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH) {
                    Point ubicacionActual = getLocation();
                    setLocation(
                            ubicacionActual.x + e.getX() - puntoInicial[0].x,
                            ubicacionActual.y + e.getY() - puntoInicial[0].y
                    );
                }
            }
        });

        return barra;
    }

    private JButton crearBotonControl(String simbolo, Color colorBase, Color colorHover, Color textoBase, Color textoHover, String tooltip) {
        final float[] progreso = {0f};
        final boolean[] encima = {false};

        JButton btn = new JButton(simbolo) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int r = (int) (colorBase.getRed() + (colorHover.getRed() - colorBase.getRed()) * progreso[0]);
                int gr = (int) (colorBase.getGreen() + (colorHover.getGreen() - colorBase.getGreen()) * progreso[0]);
                int b = (int) (colorBase.getBlue() + (colorHover.getBlue() - colorBase.getBlue()) * progreso[0]);

                g2.setColor(new Color(r, gr, b));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btn.setPreferredSize(new Dimension(46, 33));
        btn.setForeground(textoBase);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Timer animador = new Timer(15, e -> {
            if (encima[0] && progreso[0] < 1f) {
                progreso[0] = Math.min(1f, progreso[0] + 0.18f);
                btn.setForeground(progreso[0] > 0.5f ? textoHover : textoBase);
                btn.repaint();
            } else if (!encima[0] && progreso[0] > 0f) {
                progreso[0] = Math.max(0f, progreso[0] - 0.18f);
                btn.setForeground(progreso[0] > 0.5f ? textoHover : textoBase);
                btn.repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima[0] = true;
                animador.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima[0] = false;
                animador.start();
            }
        });

        return btn;
    }

    private void mostrarAcercaDe() {
        LicenciaUtil.ResultadoVerificacion lic = LicenciaUtil.verificarEstado();
        boolean esTrial = lic.getEstado() == LicenciaUtil.EstadoLicencia.PRUEBA_VIGENTE;

        String estadoTexto = esTrial
                ? "<span style='color:#1d4ed8;'><b>Versión de Prueba (" + lic.getDiasRestantes() + " días restantes)</b></span>"
                : "<span style='color:#15803d;'><b>Licencia Comercial Activada (Permanente)</b></span>";

        String mensaje = "<html><body style='width: 340px; font-family: Segoe UI, sans-serif;'>"
                + "<h2 style='margin-bottom: 4px;'>Sistema de Gestión de Inventario</h2>"
                + "<p style='margin-top: 0; color: #555;'><b>Versión 2.0.0</b> — " + estadoTexto + "</p>"
                + "<hr>"
                + "<p><b>ID de Equipo:</b> <code>" + lic.getHardwareId() + "</code></p>"
                + "<p><b>Copyright © 2026 Alex Software.</b><br>"
                + "Todos los derechos reservados.</p>"
                + "<p style='font-size: 11px; color: #666;'>Queda prohibida la reproducción, distribución o "
                + "ingeniería inversa total o parcial sin autorización expresa del titular.</p>"
                + "<p style='font-size: 11px;'>Motor de datos: MongoDB | Runtime: Java 17</p>"
                + "</body></html>";

        if (esTrial) {
            Object[] opciones = {"Activar Licencia", "Cerrar"};
            int resp = JOptionPane.showOptionDialog(
                    this,
                    mensaje,
                    "Acerca de Sistema de Inventario v2.0.0",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );
            if (resp == 0) {
                DlgActivacionLicencia dlg = new DlgActivacionLicencia(this, lic, false);
                dlg.setVisible(true);
                if (dlg.isActivadoExitosamente()) {
                    actualizarTextoPieLicencia();
                }
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    mensaje,
                    "Acerca de Sistema de Inventario v2.0.0",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void aplicarPermisosPorRol() {
        if (usuarioAutenticado == null) return;
        String rol = usuarioAutenticado.getRol().toUpperCase();
        switch (rol) {
            case "MASTER":
            case "ADMIN":
                btnRegistrar.setEnabled(true);
                btnModificar.setEnabled(true);
                btnEliminar.setEnabled(true);
                btnEntrada.setEnabled(true);
                btnSalida.setEnabled(true);
                btnHistorial.setEnabled(true);
                btnImportarCSV.setEnabled(true);
                btnExportarCSV.setEnabled(true);
                break;
            case "OPERADOR":
                btnRegistrar.setEnabled(true);
                btnModificar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEntrada.setEnabled(true);
                btnSalida.setEnabled(true);
                btnHistorial.setEnabled(true);
                btnImportarCSV.setEnabled(false);
                btnExportarCSV.setEnabled(false);
                break;
            case "CONSULTA":
                btnRegistrar.setEnabled(false);
                btnModificar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEntrada.setEnabled(false);
                btnSalida.setEnabled(false);
                btnHistorial.setEnabled(false);
                btnImportarCSV.setEnabled(false);
                btnExportarCSV.setEnabled(false);
                break;
            default:
                btnRegistrar.setEnabled(false);
                btnModificar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEntrada.setEnabled(false);
                btnSalida.setEnabled(false);
                btnHistorial.setEnabled(false);
                btnImportarCSV.setEnabled(false);
                btnExportarCSV.setEnabled(false);
                break;
        }
    }

    private void cargarDatosTabla(List<Producto> lista) {
        modeloTabla.setRowCount(0);
        for (Producto p : lista) {
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
        tablaProductos.repaint();
        actualizarTotalInventario();
    }

    private void refrescarCatalogo() {
        cmbFiltroCategoria.setSelectedItem("TODAS");
        cargarDatosTabla(servicio.listarProductos());
    }

    private void actualizarTotalInventario() {
        double total = servicio.calcularValorTotalInventario();
        lblValorTotal.setText(String.format("Valor Total del Inventario: $%.2f", total));
    }

    private String obtenerCodigoSeleccionado() {
        int fila = tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) modeloTabla.getValueAt(fila, 0);
    }

    private void registrarNuevoProducto() {
        JTextField txtCodigo = new JTextField();
        JTextField txtNombre = new JTextField();
        JTextField txtDescripcion = new JTextField();
        JComboBox<String> cmbCat = new JComboBox<>(CATEGORIAS_VALIDAS);
        JTextField txtPrecio = new JTextField();
        JTextField txtStock = new JTextField();

        Object[] formulario = {
                "Código:", txtCodigo,
                "Nombre:", txtNombre,
                "Descripción:", txtDescripcion,
                "Categoría:", cmbCat,
                "Precio ($):", txtPrecio,
                "Cantidad Inicial:", txtStock
        };

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(this, formulario, "Registrar Producto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) break;
            try {
                sincronizarSesionActiva();
                double precio = Double.parseDouble(txtPrecio.getText().trim());
                int stock = Integer.parseInt(txtStock.getText().trim());
                Producto nuevo = new Producto(txtCodigo.getText(), txtNombre.getText(), txtDescripcion.getText(), (String) cmbCat.getSelectedItem(), precio, stock);
                servicio.registrarProducto(nuevo);
                JOptionPane.showMessageDialog(this, "Producto registrado exitosamente en MongoDB.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refrescarCatalogo();
                ubicarProductoEnTabla(nuevo.getCodigo());
                break;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Verifique los valores numéricos (precio y cantidad).", "Error", JOptionPane.WARNING_MESSAGE);
            } catch (CodigoDuplicadoException | DatoInvalidoException | IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Validación", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void modificarProductoSeleccionado() {
        String codigo = obtenerCodigoSeleccionado();
        if (codigo == null) return;

        try {
            Producto actual = servicio.buscarPorCodigo(codigo);
            JTextField txtNombre = new JTextField(actual.getNombre());
            JTextField txtDescripcion = new JTextField(actual.getDescripcion());
            JComboBox<String> cmbCat = new JComboBox<>(CATEGORIAS_VALIDAS);
            cmbCat.setSelectedItem(actual.getCategoria());
            JTextField txtPrecio = new JTextField(String.valueOf(actual.getPrecio()));

            Object[] formulario = {
                    "Código (No modificable): " + actual.getCodigo(),
                    "Nombre:", txtNombre,
                    "Descripción:", txtDescripcion,
                    "Categoría:", cmbCat,
                    "Precio ($):", txtPrecio
            };

            while (true) {
                int opcion = JOptionPane.showConfirmDialog(this, formulario, "Modificar Producto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (opcion != JOptionPane.OK_OPTION) break;
                try {
                    sincronizarSesionActiva();
                    double precio = Double.parseDouble(txtPrecio.getText().trim());
                    if (precio <= 0) throw new DatoInvalidoException("El precio debe ser un número mayor a 0.00.");
                    String nom = txtNombre.getText().trim();
                    if (nom.isEmpty()) throw new DatoInvalidoException("El nombre no puede quedar vacío.");
                    String desc = txtDescripcion.getText().trim();
                    if (desc.isEmpty()) throw new DatoInvalidoException("La descripción no puede quedar vacía.");

                    actual.setNombre(nom);
                    actual.setDescripcion(desc);
                    actual.setCategoria((String) cmbCat.getSelectedItem());
                    actual.setPrecio(precio);

                    servicio.modificarProducto(actual);
                    JOptionPane.showMessageDialog(this, "Producto actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    refrescarCatalogo();
                    ubicarProductoEnTabla(actual.getCodigo());
                    break;
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "El precio debe ser un número válido.", "Error", JOptionPane.WARNING_MESSAGE);
                } catch (ProductoNoEncontradoException | DatoInvalidoException | IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Validación", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (ProductoNoEncontradoException | DatoInvalidoException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarProductoSeleccionado() {
        if (!esUsuarioPrivilegiado()) {
            JOptionPane.showMessageDialog(this, "Acceso denegado: Su rol no tiene privilegios para eliminar productos.", "Seguridad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String codigo = obtenerCodigoSeleccionado();
        if (codigo == null) return;
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar el producto con código " + codigo + "?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                sincronizarSesionActiva();
                servicio.eliminarProducto(codigo);
                JOptionPane.showMessageDialog(this, "Producto eliminado de MongoDB.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refrescarCatalogo();
            } catch (ProductoNoEncontradoException | DatoInvalidoException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gestionarMovimiento(boolean esEntrada) {
        String codigo = obtenerCodigoSeleccionado();
        if (codigo == null) return;
        String tipo = esEntrada ? "Entrada (+)" : "Salida (-)";

        JTextField txtCantidad = new JTextField();
        JTextField txtMotivo = new JTextField();
        Object[] formulario = {
                "Cantidad para la " + tipo + ":", txtCantidad,
                "Motivo / Referencia (opcional):", txtMotivo
        };

        int opcion = JOptionPane.showConfirmDialog(this, formulario, tipo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION || txtCantidad.getText().trim().isEmpty()) return;

        try {
            sincronizarSesionActiva();
            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            String motivo = txtMotivo.getText().trim();

            if (esEntrada) {
                servicio.registrarEntrada(codigo, cantidad, motivo);
                JOptionPane.showMessageDialog(this, "Entrada registrada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refrescarCatalogo();
                ubicarProductoEnTabla(codigo);
            } else {
                servicio.registrarSalida(codigo, cantidad, motivo);
                JOptionPane.showMessageDialog(this, "Salida registrada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refrescarCatalogo();
                ubicarProductoEnTabla(codigo);

                Producto actualizado = servicio.buscarPorCodigo(codigo);
                if (actualizado.getCantidadDisponible() <= LIMITE_STOCK_BAJO) {
                    String estado = actualizado.getCantidadDisponible() == 0
                            ? "AGOTADO (0 unidades)"
                            : "STOCK BAJO (" + actualizado.getCantidadDisponible() + " unidades)";
                    JOptionPane.showMessageDialog(
                            this,
                            "¡Alerta de Inventario Crítico!\n\n"
                                    + "El producto '" + actualizado.getNombre() + "' (" + actualizado.getCodigo() + ") "
                                    + "ha quedado en " + estado + ".\n"
                                    + "La fila permanecerá resaltada en rojo en el catálogo.",
                            "Aviso de Stock Bajo (<= 5)",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número entero.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (StockInsuficienteException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
        } catch (ProductoNoEncontradoException | DatoInvalidoException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirHistorialMovimientos() {
        if (usuarioAutenticado != null && "CONSULTA".equalsIgnoreCase(usuarioAutenticado.getRol())) {
            JOptionPane.showMessageDialog(this, "Acceso denegado: El rol CONSULTA no tiene permisos para visualizar el Kardex.", "Seguridad", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sincronizarSesionActiva();
        DlgHistorialMovimientos dlg = new DlgHistorialMovimientos(this);
        dlg.setVisible(true);
    }

    private void filtrarPorCategoria() {
        String categoria = (String) cmbFiltroCategoria.getSelectedItem();
        cargarDatosTabla(servicio.filtrarPorCategoria(categoria));
    }

    private void buscarProductoPorCodigo() {
        DlgBusquedaProductos dlg = new DlgBusquedaProductos(this, servicio.listarProductos());
        dlg.setVisible(true);

        String codigoElegido = dlg.getCodigoSeleccionado();
        if (codigoElegido != null) {
            ubicarProductoEnTabla(codigoElegido);
        }
    }

    private void ubicarProductoEnTabla(String codigoElegido) {
        if (codigoElegido == null) return;
        cmbFiltroCategoria.setSelectedItem("TODAS");
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            if (codigoElegido.equalsIgnoreCase(String.valueOf(modeloTabla.getValueAt(i, 0)))) {
                tablaProductos.setRowSelectionInterval(i, i);
                tablaProductos.scrollRectToVisible(tablaProductos.getCellRect(i, 0, true));
                break;
            }
        }
    }

    private void verificarYMostrarAlertaStockBajoAutomatica() {
        long totalCriticos = servicio.contarProductosBajos(LIMITE_STOCK_BAJO);
        if (totalCriticos == 0) {
            return;
        }

        List<Producto> bajos = servicio.obtenerExistenciasBajasPaginadas(LIMITE_STOCK_BAJO, 50);
        JDialog dialogoAlerta = new JDialog(this, "Aviso Automático: Productos con Stock Bajo (<= 5)", true);
        dialogoAlerta.setSize(640, 370);
        dialogoAlerta.setLocationRelativeTo(this);
        dialogoAlerta.setLayout(new BorderLayout(10, 10));
        SeguridadUtil.vincularCierreConEsc(dialogoAlerta, dialogoAlerta.getRootPane());

        JPanel panelEncabezado = new JPanel(new BorderLayout());
        panelEncabezado.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        JLabel lblInfo = new JLabel(String.format("Atención: Se detectaron %d producto(s) con existencias críticas (<= 5 unidades):", totalCriticos));
        lblInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblInfo.setForeground(new Color(180, 0, 0));
        panelEncabezado.add(lblInfo, BorderLayout.CENTER);
        dialogoAlerta.add(panelEncabezado, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Categoría", "Stock Actual"};
        DefaultTableModel modeloBajos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        for (Producto p : bajos) {
            modeloBajos.addRow(new Object[]{p.getCodigo(), p.getNombre(), p.getCategoria(), p.getCantidadDisponible()});
        }
        JTable tablaBajos = new JTable(modeloBajos);
        tablaBajos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaBajos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaBajos.getSelectedRow() != -1) {
                    String cod = String.valueOf(modeloBajos.getValueAt(tablaBajos.getSelectedRow(), 0));
                    dialogoAlerta.dispose();
                    ubicarProductoEnTabla(cod);
                }
            }
        });
        dialogoAlerta.add(new JScrollPane(tablaBajos), BorderLayout.CENTER);

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnUbicar = new JButton("Ubicar en Catálogo");
        btnUbicar.setMnemonic(KeyEvent.VK_U);
        btnUbicar.addActionListener(e -> {
            int fila = tablaBajos.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(dialogoAlerta, "Seleccione un producto de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String cod = String.valueOf(modeloBajos.getValueAt(fila, 0));
            dialogoAlerta.dispose();
            ubicarProductoEnTabla(cod);
        });
        panelBoton.add(btnUbicar);

        JButton btnCerrar = new JButton("Entendido");
        btnCerrar.setMnemonic(KeyEvent.VK_E);
        btnCerrar.setDisplayedMnemonicIndex(0);
        btnCerrar.addActionListener(e -> dialogoAlerta.dispose());
        panelBoton.add(btnCerrar);

        dialogoAlerta.add(panelBoton, BorderLayout.SOUTH);
        SeguridadUtil.habilitarNavegacionFlechas(panelBoton);
        dialogoAlerta.setVisible(true);
        tablaProductos.repaint();
    }

    private void cerrarSesion() {
        int resp = JOptionPane.showConfirmDialog(this, "¿Desea cerrar la sesión actual?", "Confirmar Cierre de Sesión", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            this.dispose();
            for (java.awt.Window ventana : java.awt.Window.getWindows()) {
                if (ventana instanceof FrmLogin) {
                    ventana.setVisible(true);
                    ventana.toFront();
                    ventana.requestFocus();
                    break;
                }
            }
        }
    }
}