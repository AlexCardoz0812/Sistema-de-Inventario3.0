
package com.inventario.app.vistas;

import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.excepciones.StockInsuficienteException;
import com.inventario.app.modelos.Producto;
import com.inventario.app.servicio.InventarioService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class FrmPrincipal extends JFrame {

    private final InventarioService servicio;
    private JTable tablaProductos;
    private DefaultTableModel modeloTabla;
    private JLabel lblValorTotal;
    private JComboBox<String> cmbFiltroCategoria;

    private static final String[] CATEGORIAS_VALIDAS = {
            "Electrónica", "Oficina", "Abarrotes", "Limpieza", "Herramientas"
    };

    public FrmPrincipal() {
        this.servicio = new InventarioService();
        configurarVentana();
        cargarDatosTabla(servicio.listarProductos());
    }

    private void configurarVentana() {
        setTitle("Sistema de Gestión de Inventario - Clave 2");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. Barra Superior: Filtros, Búsqueda y Cierre de Sesión
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
        btnBuscar.addActionListener(e -> buscarProductoPorCodigo());
        panelSuperior.add(btnBuscar);

        JButton btnStockBajo = new JButton("Alerta Stock Bajo (<= 5)");
        btnStockBajo.addActionListener(e -> mostrarAlertaStockBajo());
        panelSuperior.add(btnStockBajo);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> refrescarCatalogo());
        panelSuperior.add(btnRefrescar);

        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        btnCerrarSesion.setBackground(new Color(220, 80, 80));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.addActionListener(e -> cerrarSesion());
        panelSuperior.add(btnCerrarSesion);

        add(panelSuperior, BorderLayout.NORTH);

        // 2. Centro: Tabla de Productos
        String[] columnas = {"Código", "Nombre", "Descripción", "Categoría", "Precio ($)", "Existencias", "Total ($)"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Solo lectura en la tabla
            }
        };

        tablaProductos = new JTable(modeloTabla);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollTabla = new JScrollPane(tablaProductos);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Catálogo de Productos en MongoDB"));
        add(scrollTabla, BorderLayout.CENTER);

        // 3. Barra Lateral Derecha: Acciones CRUD y Movimientos
        JPanel panelAcciones = new JPanel(new GridLayout(6, 1, 8, 8));
        panelAcciones.setBorder(BorderFactory.createTitledBorder("Operaciones"));

        JButton btnRegistrar = new JButton("Registrar Producto");
        btnRegistrar.addActionListener(e -> registrarNuevoProducto());
        panelAcciones.add(btnRegistrar);

        JButton btnModificar = new JButton("Modificar Producto");
        btnModificar.addActionListener(e -> modificarProductoSeleccionado());
        panelAcciones.add(btnModificar);

        JButton btnEliminar = new JButton("Eliminar Producto");
        btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());
        panelAcciones.add(btnEliminar);

        JButton btnEntrada = new JButton("Entrada Stock (+)");
        btnEntrada.addActionListener(e -> gestionarMovimiento(true));
        panelAcciones.add(btnEntrada);

        JButton btnSalida = new JButton("Salida Stock (-)");
        btnSalida.addActionListener(e -> gestionarMovimiento(false));
        panelAcciones.add(btnSalida);

        add(panelAcciones, BorderLayout.EAST);

        // 4. Barra Inferior: Resumen Financiero
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        lblValorTotal = new JLabel("Valor Total del Inventario: $0.00");
        lblValorTotal.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelInferior.add(lblValorTotal);
        add(panelInferior, BorderLayout.SOUTH);
    }

    // ==========================================
    // MÉTODOS DE ACTUALIZACIÓN VISUAL
    // ==========================================

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

    // ==========================================
    // OPERACIONES CRUD Y MOVIMIENTOS
    // ==========================================

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

        // Bucle para mantener la ventana abierta hasta que los datos sean válidos o el usuario cancele
        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this, 
                    formulario, 
                    "Registrar Producto", 
                    JOptionPane.OK_CANCEL_OPTION, 
                    JOptionPane.PLAIN_MESSAGE
            );

            // Si presiona Cancelar o cierra con la 'X', salimos sin hacer nada
            if (opcion != JOptionPane.OK_OPTION) {
                break;
            }

            try {
                // Validación de campos numéricos
                double precio;
                int stock;
                try {
                    precio = Double.parseDouble(txtPrecio.getText().trim());
                } catch (NumberFormatException e) {
                    throw new DatoInvalidoException("El precio debe ser un número válido (ejemplo: 12.50).");
                }

                try {
                    stock = Integer.parseInt(txtStock.getText().trim());
                } catch (NumberFormatException e) {
                    throw new DatoInvalidoException("La cantidad inicial debe ser un número entero (ejemplo: 10).");
                }

                // Creación de la entidad (aquí se disparan las validaciones de negocio del constructor)
                Producto nuevo = new Producto(
                        txtCodigo.getText(),
                        txtNombre.getText(),
                        txtDescripcion.getText(),
                        (String) cmbCat.getSelectedItem(),
                        precio,
                        stock
                );

                // Inserción en MongoDB
                servicio.registrarProducto(nuevo);
                JOptionPane.showMessageDialog(this, "Producto registrado exitosamente en MongoDB.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                refrescarCatalogo();
                break; // Registro exitoso: salimos del bucle y se cierra el formulario

            } catch (CodigoDuplicadoException | DatoInvalidoException e) {
                // Mostramos el error; al no haber un break, el bucle repite y el formulario mantiene lo digitado
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
                int opcion = JOptionPane.showConfirmDialog(
                        this, 
                        formulario, 
                        "Modificar Producto", 
                        JOptionPane.OK_CANCEL_OPTION, 
                        JOptionPane.PLAIN_MESSAGE
                );

                if (opcion != JOptionPane.OK_OPTION) {
                    break;
                }

                try {
                    // 1. Validación del precio numérico
                    double precio;
                    try {
                        precio = Double.parseDouble(txtPrecio.getText().trim());
                    } catch (NumberFormatException e) {
                        throw new DatoInvalidoException("El precio debe ser un número válido.");
                    }

                    if (precio <= 0) {
                        throw new DatoInvalidoException("El precio debe ser un número mayor a 0.00.");
                    }

                    // 2. Validación del nombre
                    String nom = txtNombre.getText().trim();
                    if (nom.isEmpty()) {
                        throw new DatoInvalidoException("El nombre del producto no puede quedar vacío.");
                    }

                    // >>> AQUÍ VA TU CÓDIGO <<<
                    // 3. Validación de la descripción
                    String desc = txtDescripcion.getText().trim();
                    if (desc.isEmpty()) {
                        throw new DatoInvalidoException("La descripción del producto no puede quedar vacía.");
                    }

                    // Asignación de los valores validados
                    actual.setNombre(nom);
                    actual.setDescripcion(desc);
                    actual.setCategoria((String) cmbCat.getSelectedItem());
                    actual.setPrecio(precio);

                    // Guardado en MongoDB
                    servicio.modificarProducto(actual);
                    JOptionPane.showMessageDialog(this, "Producto actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    refrescarCatalogo();
                    break;

                } catch (ProductoNoEncontradoException | DatoInvalidoException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Validación", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (ProductoNoEncontradoException | DatoInvalidoException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarProductoSeleccionado() {
        String codigo = obtenerCodigoSeleccionado();
        if (codigo == null) return;

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de eliminar el producto con código " + codigo + "?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);

        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
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
        String input = JOptionPane.showInputDialog(this, "Ingrese la cantidad para la " + tipo + ":", tipo, JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int cantidad = Integer.parseInt(input.trim());
            if (esEntrada) {
                servicio.registrarEntrada(codigo, cantidad);
                JOptionPane.showMessageDialog(this, "Entrada registrada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                servicio.registrarSalida(codigo, cantidad);
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

    private void filtrarPorCategoria() {
        String categoria = (String) cmbFiltroCategoria.getSelectedItem();
        cargarDatosTabla(servicio.filtrarPorCategoria(categoria));
    }

    private void buscarProductoPorCodigo() {
        String texto = JOptionPane.showInputDialog(
                this, 
                "Ingrese el código o los primeros dígitos/caracteres a buscar:", 
                "Búsqueda por Similitud de Código", 
                JOptionPane.QUESTION_MESSAGE
        );
        
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }

        try {
            List<Producto> coincidencias = servicio.buscarPorSimilitudCodigo(texto.trim());
            
            if (coincidencias.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, 
                        "No se encontraron productos que coincidan con: \"" + texto.trim() + "\"", 
                        "Sin Coincidencias", 
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                cargarDatosTabla(coincidencias);
                JOptionPane.showMessageDialog(
                        this, 
                        "Se encontraron " + coincidencias.size() + " producto(s) coincidente(s).", 
                        "Resultado de Búsqueda", 
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (DatoInvalidoException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mostrarAlertaStockBajo() {
        // 1. Conteo ultra rápido directo en MongoDB
        long totalCriticos = servicio.contarProductosBajos(5);

        if (totalCriticos == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "¡Inventario en orden! No se encontraron productos con existencias críticas (<= 5).",
                    "Alerta de Stock Bajo",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // 2. Traemos únicamente los primeros 50 registros para no saturar memoria
        List<Producto> bajos = servicio.obtenerExistenciasBajasPaginadas(5, 50);

        // 3. Ventana emergente con JTable y Scroll
        JDialog dialogoAlerta = new JDialog(this, "Alerta: Productos con Stock Bajo", true);
        dialogoAlerta.setSize(600, 350);
        dialogoAlerta.setLocationRelativeTo(this);
        dialogoAlerta.setLayout(new BorderLayout(10, 10));

        // Encabezado
        JLabel lblInfo = new JLabel(String.format(
                "  Total de productos críticos: %d (Mostrando primeros %d)", 
                totalCriticos, bajos.size()
        ));
        lblInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblInfo.setForeground(new Color(180, 0, 0));
        dialogoAlerta.add(lblInfo, BorderLayout.NORTH);

        // Tabla interna de stock bajo
        String[] columnas = {"Código", "Nombre", "Categoría", "Stock Actual"};
        DefaultTableModel modeloBajos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (Producto p : bajos) {
            modeloBajos.addRow(new Object[]{
                    p.getCodigo(),
                    p.getNombre(),
                    p.getCategoria(),
                    p.getCantidadDisponible()
            });
        }

        JTable tablaBajos = new JTable(modeloBajos);
        JScrollPane scroll = new JScrollPane(tablaBajos);
        dialogoAlerta.add(scroll, BorderLayout.CENTER);

        // Botón de cerrar
        JButton btnCerrar = new JButton("Entendido");
        btnCerrar.addActionListener(e -> dialogoAlerta.dispose());
        JPanel panelBoton = new JPanel();
        panelBoton.add(btnCerrar);
        dialogoAlerta.add(panelBoton, BorderLayout.SOUTH);

        // Mostramos el diálogo
        dialogoAlerta.setVisible(true);
    }

    private void cerrarSesion() {
        int resp = JOptionPane.showConfirmDialog(this, "¿Desea cerrar la sesión?", "Confirmar Cierre", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            new FrmLogin().setVisible(true);
            this.dispose();
        }
    }
}