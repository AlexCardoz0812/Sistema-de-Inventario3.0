package com.inventario.app.dao;

import com.inventario.app.conexion.ConexionMongoDB;
import com.inventario.app.excepciones.CodigoDuplicadoException;
import com.inventario.app.excepciones.DatoInvalidoException;
import com.inventario.app.excepciones.ProductoNoEncontradoException;
import com.inventario.app.modelos.Producto;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author DELL
 * Capa de Acceso a Datos (DAO) para la colección 'productos' en MongoDB.
 * Implementa las operaciones CRUD y consultas específicas requeridas.
 */

public class ProductoDao {

    private MongoCollection<Document> getColeccion() {
        MongoDatabase db = ConexionMongoDB.getDatabase();
        return db.getCollection("productos");
    }

    // Proceso 1.1: Registrar nuevo producto
    public void insertar(Producto producto) throws CodigoDuplicadoException {
        if (buscarPorCodigo(producto.getCodigo()) != null) {
            throw new CodigoDuplicadoException("Ya existe un producto con el código: " + producto.getCodigo());
        }

        Document doc = new Document("codigo", producto.getCodigo())
                .append("nombre", producto.getNombre())
                .append("descripcion", producto.getDescripcion())
                .append("categoria", producto.getCategoria())
                .append("precio", producto.getPrecio())
                .append("cantidadDisponible", producto.getCantidadDisponible());

        getColeccion().insertOne(doc);
    }

    // Proceso 1.2 y 3.1: Buscar producto por código
    public Producto buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }

        Document doc = getColeccion().find(Filters.eq("codigo", codigo.trim().toUpperCase())).first();
        return doc != null ? documentAProducto(doc) : null;
    }

    // Proceso 1.3 y 3.4: Consultar catálogo completo
    public List<Producto> obtenerTodos() {
        List<Producto> lista = new ArrayList<>();
        for (Document doc : getColeccion().find()) {
            Producto p = documentAProducto(doc);
            if (p != null) {
                lista.add(p);
            }
        }
        return lista;
    }

    // Proceso 1.4: Modificar datos de un producto
    public void actualizar(Producto producto) throws ProductoNoEncontradoException {
        Bson filtro = Filters.eq("codigo", producto.getCodigo());
        
        Bson actualizacion = Updates.combine(
                Updates.set("nombre", producto.getNombre()),
                Updates.set("descripcion", producto.getDescripcion()),
                Updates.set("categoria", producto.getCategoria()),
                Updates.set("precio", producto.getPrecio()),
                Updates.set("cantidadDisponible", producto.getCantidadDisponible())
        );

        long modificados = getColeccion().updateOne(filtro, actualizacion).getModifiedCount();
        if (modificados == 0 && buscarPorCodigo(producto.getCodigo()) == null) {
            throw new ProductoNoEncontradoException("No se encontró el producto con código: " + producto.getCodigo());
        }
    }

    // Proceso 1.5: Eliminar producto
    public void eliminar(String codigo) throws ProductoNoEncontradoException {
        Bson filtro = Filters.eq("codigo", codigo.trim().toUpperCase());
        long eliminados = getColeccion().deleteOne(filtro).getDeletedCount();

        if (eliminados == 0) {
            throw new ProductoNoEncontradoException("No se encontró ningún producto con el código: " + codigo);
        }
    }

    // Proceso 2.5: Actualizar stock específico (para entradas y salidas)
    public void actualizarStock(String codigo, int nuevoStock) throws ProductoNoEncontradoException {
        Bson filtro = Filters.eq("codigo", codigo.trim().toUpperCase());
        Bson actualizacion = Updates.set("cantidadDisponible", nuevoStock);

        long modificados = getColeccion().updateOne(filtro, actualizacion).getModifiedCount();
        if (modificados == 0 && buscarPorCodigo(codigo) == null) {
            throw new ProductoNoEncontradoException("No se encontró el producto con código: " + codigo);
        }
    }

    // Proceso 3.3: Filtrar productos por categoría
    public List<Producto> buscarPorCategoria(String categoria) {
        List<Producto> lista = new ArrayList<>();
        Bson filtro = Filters.eq("categoria", categoria);
        
        for (Document doc : getColeccion().find(filtro)) {
            Producto p = documentAProducto(doc);
            if (p != null) {
                lista.add(p);
            }
        }
        return lista;
    }

    // Conversor auxiliar: BSON Document -> Entidad Producto
    private Producto documentAProducto(Document doc) {
        try {
            return new Producto(
                    doc.getString("codigo"),
                    doc.getString("nombre"),
                    doc.getString("descripcion"),
                    doc.getString("categoria"),
                    doc.getDouble("precio"),
                    doc.getInteger("cantidadDisponible")
            );
        } catch (DatoInvalidoException e) {
            System.err.println("Documento corrupto en MongoDB (" + doc.getString("codigo") + "): " + e.getMessage());
            return null;
        }
    }
    // Consulta optimizada: MongoDB filtra y devuelve solo lo necesario
    public List<Producto> obtenerBajosStock(int limiteMinimo, int maxResultados) {
        List<Producto> lista = new ArrayList<>();
        Bson filtro = Filters.lte("cantidadDisponible", limiteMinimo);
        
        for (Document doc : getColeccion().find(filtro).limit(maxResultados)) {
            Producto p = documentAProducto(doc);
            if (p != null) {
                lista.add(p);
            }
        }
        return lista;
    }

    // Cuenta total rápida en MongoDB sin descargar datos
    public long contarBajosStock(int limiteMinimo) {
        Bson filtro = Filters.lte("cantidadDisponible", limiteMinimo);
        return getColeccion().countDocuments(filtro);
    }
    
    // Búsqueda por coincidencia parcial o prefijo en el código
    public List<Producto> buscarPorCodigoSimilitud(String patronCodigo) {
        List<Producto> lista = new ArrayList<>();
        if (patronCodigo == null || patronCodigo.trim().isEmpty()) {
            return lista;
        }

        // Bson filter usando regex insensible a mayúsculas/minúsculas ("i")
        // Utilizar "^" + patron si deseas que estrictamente EMPIECE con esos caracteres,
        // o simplemente patron para que coincida en cualquier parte del código.
        Bson filtro = Filters.regex("codigo", "(?i)" + Pattern.quote(patronCodigo.trim()));

        for (Document doc : getColeccion().find(filtro)) {
            Producto p = documentAProducto(doc);
            if (p != null) {
                lista.add(p);
            }
        }
        return lista;
    }
}    