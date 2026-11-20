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

import java.util.ArrayList;
import java.util.List;

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
}