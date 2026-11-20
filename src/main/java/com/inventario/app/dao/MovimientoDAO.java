package com.inventario.app.dao;

import com.inventario.app.conexion.ConexionMongoDB;
import com.inventario.app.modelos.Movimiento;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

public class MovimientoDAO {

    private MongoCollection<Document> obtenerColeccion() {
        MongoDatabase db = ConexionMongoDB.getDatabase();
        return db.getCollection("movimientos");
    }

    public void registrar(Movimiento mov) {
        Document doc = new Document("codigoProducto", mov.getCodigoProducto())
                .append("nombreProducto", mov.getNombreProducto())
                .append("tipo", mov.getTipo())
                .append("cantidad", mov.getCantidad())
                .append("stockAnterior", mov.getStockAnterior())
                .append("stockNuevo", mov.getStockNuevo())
                .append("usuario", mov.getUsuario())
                .append("motivo", mov.getMotivo())
                .append("fechaHora", mov.getFechaHora());
        obtenerColeccion().insertOne(doc);
    }

    public List<Movimiento> listarTodos() {
        List<Movimiento> lista = new ArrayList<>();
        for (Document doc : obtenerColeccion().find().sort(Sorts.descending("_id"))) {
            Movimiento m = new Movimiento();
            m.setId(doc.getObjectId("_id").toHexString());
            m.setCodigoProducto(doc.getString("codigoProducto"));
            m.setNombreProducto(doc.getString("nombreProducto"));
            m.setTipo(doc.getString("tipo"));
            m.setCantidad(doc.getInteger("cantidad", 0));
            m.setStockAnterior(doc.getInteger("stockAnterior", 0));
            m.setStockNuevo(doc.getInteger("stockNuevo", 0));
            m.setUsuario(doc.getString("usuario"));
            m.setMotivo(doc.getString("motivo"));
            m.setFechaHora(doc.getString("fechaHora"));
            lista.add(m);
        }
        return lista;
    }
}