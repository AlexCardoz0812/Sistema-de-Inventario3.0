package com.inventario.app.vistas;

import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.excepciones.StockInsuficienteException;
import com.inventario.app.modelos.Producto;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.servicio.InventarioService;
import com.inventario.app.utilidades.SeguridadUtil;
import com.inventario.app.utilidades.SesionUsuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private DefaultTableModel modeloTabla;
    private JLabel lblValorTotal;
    private JLabel lblEstadoUsuario;
    private JComboBox<String> cmbFiltroCategoria;
    private Usuario usuarioAutenticado;

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
            setTitle("Sistema de Gestión de Inventario v2.0.0 - Operador: " + usuario.getUsername());
            lblEstadoUsuario.setText("Operador: " + usuario.getUsername() + " (" + usuario.getRol() + ")");
            aplicarPermisosPorRol();
        }
    }

    public Usuario getUsuarioAutenticado() {
        return this.usuarioAutenticado;
    }

    /**
     * Garantiza que el registro de auditoría (Kardex) firme con el usuario
     * dueño de esta ventana, incluso si hay múltiples sesiones abiertas.
     */
    private void sincronizarSesionActiva() {
        if (this.usuarioAutenticado != null) {
            SesionUsuario.iniciarSesion(
                    this.usuarioAutenticado.getUsername(),
                    this.usuarioAutenticado.getRol()
            );
        }
    }

    private void configurarVentana() {
        setTitle("Sistema de Gestión de Inventario v2.0.0");
        setSize(980, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cerrarSesionConEsc");
        this.getRootPane().getActionMap().put("cerrarSesionConEsc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cerrarSesion();
            }
        });

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Búsqueda y Filtros"));

        panelSuperior.add(new JLabel("Categoría:"));
        cmbFiltroCategoria = new JComboBox<>();
        cmbFiltroCategoria.addItem("TODAS");
        for (String cat : CATEGORIAS_VALIDAS) {
            cmbFiltroCategoria.addItem(cat);
        }
        cmbFiltroCategoria.addActionListener(e -> filtrarPorCategoria());
        panelSuperior.add(cmbFiltroCategoria);

        JButton btnBuscar = new JButton("Buscar por Código");
        btnBuscar.setMnemonic(KeyEvent.VK_B);
        btnBuscar.setDisplayedMnemonicIndex(0);
        btnBuscar.addActionListener(e -> buscarProductoPorCodigo());
        panelSuperior.add(btnBuscar);

        JButton btnStockBajo = new JButton("Alerta Stock Bajo (<= 5)");
        btnStockBajo.setMnemonic(KeyEvent.VK_A);
        btnStockBajo.setDisplayedMnemonicIndex(0);
        btnStockBajo.addActionListener(e -> mostrarAlertaStockBajo());
        panelSuperior.add(btnStockBajo);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.setMnemonic(KeyEvent.VK_F);
        btnRefrescar.setDisplayedMnemonicIndex(2);
        btnRefrescar.addActionListener(e -> refrescarCatalogo());
        panelSuperior.add(btnRefrescar);

        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        btnCerrarSesion.setBackground(new Color(220, 80, 80));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.setMnemonic(KeyEvent.VK_C);
        btnCerrarSesion.setDisplayedMnemonicIndex(0);
        btnCerrarSesion.addActionListener(e -> cerrarSesion());
        panelSuperior.add(btnCerrarSesion);

        SeguridadUtil.habilitarNavegacionFlechas(panelSuperior);
        add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Descripción", "Categoría", "Precio ($)", "Existencias", "Total ($)"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollTabla = new JScrollPane(tablaProductos);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Catálogo de Productos en MongoDB"));
        add(scrollTabla, BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new GridLayout(6, 1, 8, 8));
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

        SeguridadUtil.habilitarNavegacionFlechas(panelAcciones);
        add(panelAcciones, BorderLayout.EAST);

        JPanel panelInferior = new JPanel(new BorderLayout(15, 0));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));

        lblEstadoUsuario = new JLabel("Operador: Invitado / Local");
        lblEstadoUsuario.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblEstadoUsuario.setForeground(new Color(90, 90, 90));
        panelInferior.add(lblEstadoUsuario, BorderLayout.WEST);

        JLabel lblCopyright = new JLabel("v2.0.0 | © 2026 Alex Software. Todos los derechos reservados.", SwingConstants.CENTER);
        lblCopyright.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCopyright.setForeground(new Color(110, 110, 110));
        lblCopyright.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCopyright.setToolTipText("Haz clic para ver información de licencia y versión");
        lblCopyright.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mostrarAcercaDe();
            }
        });
        panelInferior.add(lblCopyright, BorderLayout.CENTER);

        lblValorTotal = new JLabel("Valor Total del Inventario: $0.00");
        lblValorTotal.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelInferior.add(lblValorTotal, BorderLayout.EAST);

        add(panelInferior, BorderLayout.SOUTH);
    }

    private void mostrarAcercaDe() {
        String mensaje = "<html><body style='width: 330px; font-family: Segoe UI, sans-serif;'>"
                + "<h2 style='margin-bottom: 4px;'>Sistema de Gestión de Inventario</h2>"
                + "<p style='margin-top: 0; color: #555;'><b>Versión 2.0.0</b> (Edición Comercial)</p>"
                + "<hr>"
                + "<p><b>Copyright © 2026 Alex Software.</b><br>"
                + "Todos los derechos reservados.</p>"
                + "<p style='font-size: 11px; color: #666;'>Queda prohibida la reproducción, distribución o "
                + "ingeniería inversa total o parcial sin autorización expresa del titular.</p>"
                + "<p style='font-size: 11px;'>Motor de datos: MongoDB | Runtime: Java 17</p>"
                + "</body></html>";

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Acerca de Sistema de Inventario v2.0.0",
                JOptionPane.INFORMATION_MESSAGE
        );
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
                break;
            case "OPERADOR":
                btnRegistrar.setEnabled(true);
                btnModificar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEntrada.setEnabled(true);
                btnSalida.setEnabled(true);
                btnHistorial.setEnabled(true);
                break;
            case "CONSULTA":
                btnRegistrar.setEnabled(false);
                btnModificar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEntrada.setEnabled(false);
                btnSalida.setEnabled(false);
                btnHistorial.setEnabled(true);
                break;
            default:
                btnRegistrar.setEnabled(false);
                btnModificar.setEnabled(false);
                btnEliminar.setEnabled(false);
                btnEntrada.setEnabled(false);
                btnSalida.setEnabled(false);
                btnHistorial.setEnabled(false);
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
        if (usuarioAutenticado != null && !"MASTER".equalsIgnoreCase(usuarioAutenticado.getRol()) && !"ADMIN".equalsIgnoreCase(usuarioAutenticado.getRol())) {
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
            } else {
                servicio.registrarSalida(codigo, cantidad, motivo);
                JOptionPane.showMessageDialog(this, "Salida registrada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            refrescarCatalogo();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número entero.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (StockInsuficienteException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
        } catch (ProductoNoEncontradoException | DatoInvalidoException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirHistorialMovimientos() {
        DlgHistorialMovimientos dlg = new DlgHistorialMovimientos(this);
        dlg.setVisible(true);
    }

    private void filtrarPorCategoria() {
        String categoria = (String) cmbFiltroCategoria.getSelectedItem();
        cargarDatosTabla(servicio.filtrarPorCategoria(categoria));
    }

    private void buscarProductoPorCodigo() {
        String texto = JOptionPane.showInputDialog(this, "Ingrese el código o los primeros dígitos/caracteres a buscar:", "Búsqueda por Similitud de Código", JOptionPane.QUESTION_MESSAGE);
        if (texto == null || texto.trim().isEmpty()) return;
        try {
            List<Producto> coincidencias = servicio.buscarPorSimilitudCodigo(texto.trim());
            if (coincidencias.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron productos que coincidan con: \"" + texto.trim() + "\"", "Sin Coincidencias", JOptionPane.INFORMATION_MESSAGE);
            } else {
                cargarDatosTabla(coincidencias);
                JOptionPane.showMessageDialog(this, "Se encontraron " + coincidencias.size() + " producto(s) coincidente(s).", "Resultado de Búsqueda", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (DatoInvalidoException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mostrarAlertaStockBajo() {
        long totalCriticos = servicio.contarProductosBajos(5);
        if (totalCriticos == 0) {
            JOptionPane.showMessageDialog(this, "¡Inventario en orden! No se encontraron productos con existencias críticas (<= 5).", "Alerta de Stock Bajo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Producto> bajos = servicio.obtenerExistenciasBajasPaginadas(5, 50);
        JDialog dialogoAlerta = new JDialog(this, "Alerta: Productos con Stock Bajo", true);
        dialogoAlerta.setSize(600, 350);
        dialogoAlerta.setLocationRelativeTo(this);
        dialogoAlerta.setLayout(new BorderLayout(10, 10));
        SeguridadUtil.vincularCierreConEsc(dialogoAlerta, dialogoAlerta.getRootPane());
        JLabel lblInfo = new JLabel(String.format("  Total de productos críticos: %d (Mostrando primeros %d)", totalCriticos, bajos.size()));
        lblInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblInfo.setForeground(new Color(180, 0, 0));
        dialogoAlerta.add(lblInfo, BorderLayout.NORTH);

        String[] columnas = {"Código", "Nombre", "Categoría", "Stock Actual"};
        DefaultTableModel modeloBajos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        for (Producto p : bajos) {
            modeloBajos.addRow(new Object[]{p.getCodigo(), p.getNombre(), p.getCategoria(), p.getCantidadDisponible()});
        }
        JTable tablaBajos = new JTable(modeloBajos);
        dialogoAlerta.add(new JScrollPane(tablaBajos), BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Entendido");
        btnCerrar.setMnemonic(KeyEvent.VK_E);
        btnCerrar.setDisplayedMnemonicIndex(0);
        btnCerrar.addActionListener(e -> dialogoAlerta.dispose());

        JPanel panelBoton = new JPanel();
        panelBoton.add(btnCerrar);
        dialogoAlerta.add(panelBoton, BorderLayout.SOUTH);
        SeguridadUtil.habilitarNavegacionFlechas(panelBoton);
        dialogoAlerta.setVisible(true);
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