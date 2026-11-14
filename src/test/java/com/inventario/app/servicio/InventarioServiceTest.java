/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventario.app.servicio;

/**
 *
 * @author DELL
 */

import com.inventario.app.dao.ProductoDao;
import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.excepciones.StockInsuficienteException;
import com.inventario.app.modelos.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para InventarioService utilizando JUnit 5.
 * Se emplea un Double de Prueba (Stub) de ProductoDao para aislar 
 * la lógica de negocio de la base de datos MongoDB.
 */
class InventarioServiceTest {

    private InventarioService servicio;
    private FakeProductoDao fakeDao;

    // Stub en memoria que reemplaza las llamadas a MongoDB
    private static class FakeProductoDao extends ProductoDao {
        private final List<Producto> baseDatosEnMemoria = new ArrayList<>();

        @Override
        public void insertar(Producto producto) throws CodigoDuplicadoException {
            if (buscarPorCodigo(producto.getCodigo()) != null) {
                throw new CodigoDuplicadoException("Código duplicado: " + producto.getCodigo());
            }
            baseDatosEnMemoria.add(producto);
        }

        @Override
        public Producto buscarPorCodigo(String codigo) {
            return baseDatosEnMemoria.stream()
                    .filter(p -> p.getCodigo().equalsIgnoreCase(codigo))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Producto> obtenerTodos() {
            return new ArrayList<>(baseDatosEnMemoria);
        }

        @Override
        public void actualizarStock(String codigo, int nuevoStock) throws ProductoNoEncontradoException {
            Producto actual = buscarPorCodigo(codigo);
            if (actual == null) {
                throw new ProductoNoEncontradoException("No existe: " + codigo);
            }
            actual.setCantidadDisponible(nuevoStock);
        }
    }

    @BeforeEach
    void setUp() throws DatoInvalidoException, CodigoDuplicadoException {
        fakeDao = new FakeProductoDao();
        // Inyectamos el DAO simulado mediante el constructor que creamos en InventarioService
        servicio = new InventarioService(fakeDao);

        // Datos semilla para las pruebas
        fakeDao.insertar(new Producto("PROD-01", "Laptop HP", "Core i5 16GB", "Electrónica", 650.00, 10));
        fakeDao.insertar(new Producto("PROD-02", "Mouse Óptico", "Inalámbrico USB", "Electrónica", 15.00, 4));
        fakeDao.insertar(new Producto("PROD-03", "Cuaderno Espiral", "100 Páginas raya", "Oficina", 2.50, 0));
    }

    @Nested
    @DisplayName("Pruebas de Cálculo Financiero y Reportes")
    class PruebasCalculos {

        @Test
        @DisplayName("Debe calcular correctamente el valor total del inventario acumulado")
        void testCalcularValorTotalInventario() {
            // PROD-01: 650.00 * 10 = 6500.00
            // PROD-02: 15.00  *  4 =   60.00
            // PROD-03:  2.50  *  0 =    0.00
            // Total Esperado       = 6560.00
            double totalEsperado = 6560.00;
            double totalObtenido = servicio.calcularValorTotalInventario();

            assertEquals(totalEsperado, totalObtenido, 0.001, 
                    "El valor total consolidado debe ser igual a la sumatoria de precios por cantidad.");
        }

        @Test
        @DisplayName("Debe retornar cero cuando el inventario no tiene existencias o está vacío")
        void testCalcularValorTotalConStockCero() throws DatoInvalidoException, CodigoDuplicadoException {
            FakeProductoDao daoVacio = new FakeProductoDao();
            InventarioService servicioVacio = new InventarioService(daoVacio);

            assertEquals(0.0, servicioVacio.calcularValorTotalInventario(), 0.001);

            daoVacio.insertar(new Producto("A-1", "Lapicero", "Tinta azul", "Oficina", 1.25, 0));
            assertEquals(0.0, servicioVacio.calcularValorTotalInventario(), 0.001);
        }

        @Test
        @DisplayName("Debe detectar correctamente productos con existencias bajas (<= límite)")
        void testObtenerExistenciasBajas() throws DatoInvalidoException {
            List<Producto> bajas = servicio.obtenerExistenciasBajas(5);

            // Deben coincidir PROD-02 (stock: 4) y PROD-03 (stock: 0)
            assertEquals(2, bajas.size(), "Deben detectarse exactamente 2 productos con stock <= 5.");
        }
    }

    @Nested
    @DisplayName("Pruebas de Validación de Movimientos de Stock")
    class PruebasMovimientosStock {

        @Test
        @DisplayName("Debe registrar entradas e incrementar existencias físicas")
        void testRegistrarEntradaExitoso() throws ProductoNoEncontradoException, DatoInvalidoException {
            servicio.registrarEntrada("PROD-02", 6);
            Producto actualizado = servicio.buscarPorCodigo("PROD-02");

            assertEquals(10, actualizado.getCantidadDisponible(), 
                    "El stock debía subir de 4 a 10 con una entrada de 6 unidades.");
        }

        @Test
        @DisplayName("Debe lanzar DatoInvalidoException si la cantidad de entrada es <= 0")
        void testRegistrarEntradaInvalida() {
            assertThrows(DatoInvalidoException.class, () -> {
                servicio.registrarEntrada("PROD-01", 0);
            }, "Una entrada con cantidad 0 debe ser rechazada.");

            assertThrows(DatoInvalidoException.class, () -> {
                servicio.registrarEntrada("PROD-01", -5);
            }, "Una entrada con cantidad negativa debe ser rechazada.");
        }

        @Test
        @DisplayName("Debe registrar salidas y descontar existencias físicas")
        void testRegistrarSalidaExitoso() throws Exception {
            servicio.registrarSalida("PROD-01", 3);
            Producto actualizado = servicio.buscarPorCodigo("PROD-01");

            assertEquals(7, actualizado.getCantidadDisponible(), 
                    "El stock debía bajar de 10 a 7 tras una salida de 3 unidades.");
        }

        @Test
        @DisplayName("Debe lanzar StockInsuficienteException si la salida supera las existencias disponibles")
        void testRegistrarSalidaStockInsuficiente() {
            // PROD-02 tiene 4 en existencia; solicitar 5 debe lanzar la excepción
            assertThrows(StockInsuficienteException.class, () -> {
                servicio.registrarSalida("PROD-02", 5);
            }, "Debe bloquear la operación si la salida supera el stock físico.");
        }
    }

    @Nested
    @DisplayName("Pruebas de Excepciones en Búsqueda y Registro")
    class PruebasExcepcionesGenerales {

        @Test
        @DisplayName("Debe lanzar ProductoNoEncontradoException ante códigos inexistentes")
        void testBuscarProductoInexistente() {
            assertThrows(ProductoNoEncontradoException.class, () -> {
                servicio.buscarPorCodigo("COD-FANTASMA");
            });
        }

        @Test
        @DisplayName("Debe lanzar CodigoDuplicadoException si se registra un código existente")
        void testRegistrarCodigoDuplicado() throws DatoInvalidoException {
            Producto clon = new Producto("PROD-01", "Laptop clon", "Otra desc", "Electrónica", 500.0, 1);

            assertThrows(CodigoDuplicadoException.class, () -> {
                servicio.registrarProducto(clon);
            }, "No debe permitir insertar un producto con código ya existente.");
        }

        @Test
        @DisplayName("Debe rechazar la búsqueda si el código es nulo o vacío")
        void testBuscarCodigoInvalido() {
            assertThrows(DatoInvalidoException.class, () -> servicio.buscarPorCodigo(null));
            assertThrows(DatoInvalidoException.class, () -> servicio.buscarPorCodigo("   "));
        }
    }
}