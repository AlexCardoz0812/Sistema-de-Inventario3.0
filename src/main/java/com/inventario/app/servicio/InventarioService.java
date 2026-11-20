package com.inventario.app.servicio;

import com.inventario.app.dao.MovimientoDAO;
import com.inventario.app.dao.ProductoDao;
import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.excepciones.StockInsuficienteException;
import com.inventario.app.modelos.Movimiento;
import com.inventario.app.modelos.Producto;
import com.inventario.app.utilidades.SesionUsuario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventarioService {

    private final ProductoDao productoDao;
    private final MovimientoDAO movimientoDAO;

    public InventarioService() {
        this.productoDao = new ProductoDao();
        this.movimientoDAO = new MovimientoDAO();
    }

    // Constructor alternativo para inyección o pruebas
    public InventarioService(ProductoDao productoDao) {
        this.productoDao = productoDao;
        this.movimientoDAO = new MovimientoDAO();
    }

    // ==========================================
    // PROCESO 1: GESTIÓN DE PRODUCTOS (CRUD)
    // ==========================================

    public void registrarProducto(Producto producto) throws CodigoDuplicadoException, DatoInvalidoException {
        if (producto == null) {
            throw new DatoInvalidoException("El producto no puede ser nulo.");
        }
        productoDao.insertar(producto);

        movimientoDAO.registrar(new Movimiento(
                producto.getCodigo(),
                producto.getNombre(),
                "CREACION",
                producto.getCantidadDisponible(),
                0,
                producto.getCantidadDisponible(),
                SesionUsuario.getUsernameActual(),
                "Alta inicial en catálogo"
        ));
    }

    public List<Producto> listarProductos() {
        return productoDao.obtenerTodos();
    }

    public Producto buscarPorCodigo(String codigo) throws ProductoNoEncontradoException, DatoInvalidoException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DatoInvalidoException("Debe ingresar un código para buscar.");
        }
        Producto p = productoDao.buscarPorCodigo(codigo.trim().toUpperCase());
        if (p == null) {
            throw new ProductoNoEncontradoException("No existe ningún producto con el código: " + codigo.trim().toUpperCase());
        }
        return p;
    }

    public void modificarProducto(Producto producto) throws ProductoNoEncontradoException, DatoInvalidoException {
        if (producto == null) {
            throw new DatoInvalidoException("El producto a modificar no puede ser nulo.");
        }
        productoDao.actualizar(producto);

        movimientoDAO.registrar(new Movimiento(
                producto.getCodigo(),
                producto.getNombre(),
                "MODIFICACION",
                0,
                producto.getCantidadDisponible(),
                producto.getCantidadDisponible(),
                SesionUsuario.getUsernameActual(),
                String.format("Edición de ficha (Precio: $%.2f, Cat: %s)", producto.getPrecio(), producto.getCategoria())
        ));
    }

    public void eliminarProducto(String codigo) throws ProductoNoEncontradoException, DatoInvalidoException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DatoInvalidoException("Debe indicar un código de producto a eliminar.");
        }
        // Obtenemos los datos antes de borrarlo para dejar constancia exacta en el Kardex
        Producto existente = buscarPorCodigo(codigo);
        productoDao.eliminar(existente.getCodigo());

        movimientoDAO.registrar(new Movimiento(
                existente.getCodigo(),
                existente.getNombre(),
                "ELIMINACION",
                existente.getCantidadDisponible(),
                existente.getCantidadDisponible(),
                0,
                SesionUsuario.getUsernameActual(),
                "Baja definitiva del catálogo"
        ));
    }

    // ==========================================
    // PROCESO 2: ADMINISTRACIÓN DE EXISTENCIAS
    // ==========================================

    public void registrarEntrada(String codigo, int cantidadEntrada)
            throws ProductoNoEncontradoException, DatoInvalidoException {
        registrarEntrada(codigo, cantidadEntrada, "");
    }

    public void registrarEntrada(String codigo, int cantidadEntrada, String motivo)
            throws ProductoNoEncontradoException, DatoInvalidoException {
        if (cantidadEntrada <= 0) {
            throw new DatoInvalidoException("La cantidad de entrada debe ser estrictamente mayor a 0.");
        }

        Producto actual = buscarPorCodigo(codigo);
        int stockAnterior = actual.getCantidadDisponible();
        int nuevoStock = stockAnterior + cantidadEntrada;
        productoDao.actualizarStock(actual.getCodigo(), nuevoStock);

        String detalle = (motivo == null || motivo.trim().isEmpty()) ? "Ingreso de mercancía" : motivo.trim();
        movimientoDAO.registrar(new Movimiento(
                actual.getCodigo(),
                actual.getNombre(),
                "ENTRADA",
                cantidadEntrada,
                stockAnterior,
                nuevoStock,
                SesionUsuario.getUsernameActual(),
                detalle
        ));
    }

    public void registrarSalida(String codigo, int cantidadSalida)
            throws ProductoNoEncontradoException, StockInsuficienteException, DatoInvalidoException {
        registrarSalida(codigo, cantidadSalida, "");
    }

    public void registrarSalida(String codigo, int cantidadSalida, String motivo)
            throws ProductoNoEncontradoException, StockInsuficienteException, DatoInvalidoException {
        if (cantidadSalida <= 0) {
            throw new DatoInvalidoException("La cantidad de salida debe ser estrictamente mayor a 0.");
        }

        Producto actual = buscarPorCodigo(codigo);
        int stockAnterior = actual.getCantidadDisponible();
        if (cantidadSalida > stockAnterior) {
            throw new StockInsuficienteException(String.format(
                    "Operación rechazada: Intenta retirar %d unidades pero solo hay %d disponibles para '%s'.",
                    cantidadSalida, stockAnterior, actual.getNombre()
            ));
        }

        int nuevoStock = stockAnterior - cantidadSalida;
        productoDao.actualizarStock(actual.getCodigo(), nuevoStock);

        String detalle = (motivo == null || motivo.trim().isEmpty()) ? "Despacho / Salida de stock" : motivo.trim();
        movimientoDAO.registrar(new Movimiento(
                actual.getCodigo(),
                actual.getNombre(),
                "SALIDA",
                cantidadSalida,
                stockAnterior,
                nuevoStock,
                SesionUsuario.getUsernameActual(),
                detalle
        ));
    }

    // ==========================================
    // PROCESOS 3 Y 4: BÚSQUEDAS, FILTROS Y REPORTES
    // ==========================================

    public List<Producto> filtrarPorCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty() || categoria.equalsIgnoreCase("TODAS")) {
            return listarProductos();
        }
        return productoDao.buscarPorCategoria(categoria.trim());
    }

    public List<Producto> obtenerProductosAgotados() {
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : listarProductos()) {
            if (p.getCantidadDisponible() == 0) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public List<Producto> obtenerExistenciasBajas(int limiteMinimo) throws DatoInvalidoException {
        if (limiteMinimo < 0) {
            throw new DatoInvalidoException("El límite de existencias no puede ser negativo.");
        }
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : listarProductos()) {
            if (p.getCantidadDisponible() <= limiteMinimo) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public double calcularValorTotalInventario() {
        double total = 0.0;
        for (Producto p : listarProductos()) {
            total += p.calcularValorTotal();
        }
        return total;
    }

    public long contarProductosBajos(int limiteMinimo) {
        return productoDao.contarBajosStock(limiteMinimo);
    }

    public List<Producto> obtenerExistenciasBajasPaginadas(int limiteMinimo, int maxResultados) {
        return productoDao.obtenerBajosStock(limiteMinimo, maxResultados);
    }

    public List<Producto> buscarPorSimilitudCodigo(String texto) throws DatoInvalidoException {
        if (texto == null || texto.trim().isEmpty()) {
            throw new DatoInvalidoException("Debe ingresar al menos un valor de búsqueda.");
        }
        return productoDao.buscarPorCodigoSimilitud(texto.trim());
    }

    // ==========================================
    // AUDITORÍA INICIAL (MIGRACIÓN DE CATÁLOGO)
    // ==========================================

    public void sincronizarSaldoInicialSiVacio() {
        if (movimientoDAO.listarTodos().isEmpty()) {
            for (Producto p : listarProductos()) {
                movimientoDAO.registrar(new Movimiento(
                        p.getCodigo(),
                        p.getNombre(),
                        "SALDO_INICIAL",
                        p.getCantidadDisponible(),
                        0,
                        p.getCantidadDisponible(),
                        "sistema",
                        "Carga inicial de catálogo existente"
                ));
            }
        }
    }

    // ==========================================
    // PROCESO 5: IMPORTACIÓN Y EXPORTACIÓN CSV
    // ==========================================

    public static class ResultadoImportacion {
        private int importados;
        private int omitidos;
        private final List<String> detallesErrores = new ArrayList<>();

        public int getImportados() {
            return importados;
        }

        public int getOmitidos() {
            return omitidos;
        }

        public List<String> getDetallesErrores() {
            return detallesErrores;
        }
    }

    public void generarPlantillaCSV(File archivoDestino) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivoDestino), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.write("Codigo;Nombre;Descripcion;Categoria;Precio;Existencias");
            writer.newLine();
            writer.write("PROD-001;Teclado Mecánico RGB;Teclado retroiluminado español;Electrónica;45.50;15");
            writer.newLine();
            writer.write("PROD-002;Resma Papel Carta;Paquete de 500 hojas bond;Oficina;5.25;40");
            writer.newLine();
        }
    }

    public int exportarCatalogoCSV(File archivoDestino) throws IOException {
        List<Producto> productos = listarProductos();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivoDestino), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.write("Codigo;Nombre;Descripcion;Categoria;Precio;Existencias;ValorTotal");
            writer.newLine();

            for (Producto p : productos) {
                String linea = String.format(Locale.US, "%s;%s;%s;%s;%.2f;%d;%.2f",
                        limpiarCampoCSV(p.getCodigo()),
                        limpiarCampoCSV(p.getNombre()),
                        limpiarCampoCSV(p.getDescripcion()),
                        limpiarCampoCSV(p.getCategoria()),
                        p.getPrecio(),
                        p.getCantidadDisponible(),
                        p.calcularValorTotal());
                writer.write(linea);
                writer.newLine();
            }
        }
        return productos.size();
    }

    private String limpiarCampoCSV(String valor) {
        if (valor == null) return "";
        return valor.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    public ResultadoImportacion importarDesdeCSV(File archivoOrigen) throws IOException, DatoInvalidoException {
        if (archivoOrigen == null || !archivoOrigen.exists()) {
            throw new DatoInvalidoException("El archivo CSV seleccionado no existe.");
        }

        ResultadoImportacion resultado = new ResultadoImportacion();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivoOrigen), StandardCharsets.UTF_8))) {

            String linea;
            int numeroLinea = 0;

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (numeroLinea == 1 && linea.startsWith("\ufeff")) {
                    linea = linea.substring(1);
                }

                String lineaLimpia = linea.trim();
                if (lineaLimpia.isEmpty()) {
                    continue;
                }

                if (numeroLinea == 1 && lineaLimpia.toLowerCase().startsWith("c")) {
                    String primeraCol = lineaLimpia.split("[;,]")[0].trim().toLowerCase();
                    if (primeraCol.equals("codigo") || primeraCol.equals("código")) {
                        continue;
                    }
                }

                String separador = lineaLimpia.contains(";") ? ";" : ",";
                String[] partes = lineaLimpia.split(separador, -1);

                if (partes.length < 6) {
                    resultado.omitidos++;
                    resultado.detallesErrores.add("Fila " + numeroLinea + ": Faltan columnas (se esperan 6: Codigo, Nombre, Descripcion, Categoria, Precio, Existencias).");
                    continue;
                }

                try {
                    String codigo = partes[0].trim();
                    String nombre = partes[1].trim();
                    String descripcion = partes[2].trim();
                    String categoria = normalizarCategoria(partes[3].trim());
                    double precio = Double.parseDouble(partes[4].trim().replace(",", "."));
                    int stock = Integer.parseInt(partes[5].trim());

                    Producto nuevo = new Producto(codigo, nombre, descripcion, categoria, precio, stock);
                    registrarProducto(nuevo);
                    resultado.importados++;

                } catch (NumberFormatException e) {
                    resultado.omitidos++;
                    resultado.detallesErrores.add("Fila " + numeroLinea + ": Precio o existencias con formato numérico inválido.");
                } catch (CodigoDuplicadoException | DatoInvalidoException | IllegalArgumentException e) {
                    resultado.omitidos++;
                    resultado.detallesErrores.add("Fila " + numeroLinea + " (" + partes[0].trim() + "): " + e.getMessage());
                }
            }
        }

        return resultado;
    }

    private String normalizarCategoria(String catEntrada) {
        if (catEntrada == null || catEntrada.isEmpty()) return "Oficina";
        String lower = catEntrada.toLowerCase();
        if (lower.startsWith("elec")) return "Electrónica";
        if (lower.startsWith("ofic")) return "Oficina";
        if (lower.startsWith("abar")) return "Abarrotes";
        if (lower.startsWith("limp")) return "Limpieza";
        if (lower.startsWith("herr")) return "Herramientas";
        return catEntrada;
    }
}