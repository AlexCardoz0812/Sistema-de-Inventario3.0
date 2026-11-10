package com.inventario.app.servicio;

/**
 *
 * @author DELL
 * Capa de Lógica de Negocio (Servicio) para el Inventario.
 * Centraliza las reglas de validación, control de existencias y reportes.
 * 
*/

import com.inventario.app.dao.ProductoDao;
import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.excepciones.StockInsuficienteException;
import com.inventario.app.modelos.Producto;

import java.util.ArrayList;
import java.util.List;

public class InventarioService {

    private final ProductoDao productoDao;

    public InventarioService() {
        this.productoDao = new ProductoDao();
    }

    // Constructor alternativo para inyección o pruebas
    public InventarioService(ProductoDao productoDao) {
        this.productoDao = productoDao;
    }

    // ==========================================
    // PROCESO 1: GESTIÓN DE PRODUCTOS (CRUD)
    // ==========================================

    public void registrarProducto(Producto producto) throws CodigoDuplicadoException, DatoInvalidoException {
        if (producto == null) {
            throw new DatoInvalidoException("El producto no puede ser nulo.");
        }
        productoDao.insertar(producto);
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
    }

    public void eliminarProducto(String codigo) throws ProductoNoEncontradoException, DatoInvalidoException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DatoInvalidoException("Debe indicar un código de producto a eliminar.");
        }
        productoDao.eliminar(codigo.trim().toUpperCase());
    }

    // ==========================================
    // PROCESO 2: ADMINISTRACIÓN DE EXISTENCIAS
    // ==========================================

    /**
     * Incrementa las existencias físicas de un producto.
     */
    public void registrarEntrada(String codigo, int cantidadEntrada) 
            throws ProductoNoEncontradoException, DatoInvalidoException {
        if (cantidadEntrada <= 0) {
            throw new DatoInvalidoException("La cantidad de entrada debe ser estrictamente mayor a 0.");
        }

        Producto actual = buscarPorCodigo(codigo);
        int nuevoStock = actual.getCantidadDisponible() + cantidadEntrada;
        productoDao.actualizarStock(actual.getCodigo(), nuevoStock);
    }

    /**
     * Disminuye las existencias comprobando que no supere el stock disponible.
     */
    public void registrarSalida(String codigo, int cantidadSalida) 
            throws ProductoNoEncontradoException, StockInsuficienteException, DatoInvalidoException {
        if (cantidadSalida <= 0) {
            throw new DatoInvalidoException("La cantidad de salida debe ser estrictamente mayor a 0.");
        }

        Producto actual = buscarPorCodigo(codigo);
        if (cantidadSalida > actual.getCantidadDisponible()) {
            throw new StockInsuficienteException(String.format(
                    "Operación rechazada: Intenta retirar %d unidades pero solo hay %d disponibles para '%s'.",
                    cantidadSalida, actual.getCantidadDisponible(), actual.getNombre()
            ));
        }

        int nuevoStock = actual.getCantidadDisponible() - cantidadSalida;
        productoDao.actualizarStock(actual.getCodigo(), nuevoStock);
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

    /**
     * Reporte 4.2: Productos agotados (stock 0).
     */
    public List<Producto> obtenerProductosAgotados() {
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : listarProductos()) {
            if (p.getCantidadDisponible() == 0) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Reporte 4.3 y Proceso 3.6: Identificar existencias bajas.
     */
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

    /**
     * Reporte 4.4: Valor Total del Inventario = Σ (precio * cantidadDisponible).
     */
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
}