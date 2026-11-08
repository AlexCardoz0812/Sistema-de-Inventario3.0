
package com.inventario.app.dao;

import com.inventario.app.conexion.ConexionMongoDB;
import com.inventario.app.modelos.Usuario;
import com.inventario.app.utilidades.Encriptador;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

public class UsuarioDAO {
    // Retorna el Usuario si las credenciales son correctas, o null si fallan
    public Usuario validarLogin(String username, String passwordPlano) {
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            MongoCollection<Document> coleccion = db.getCollection("usuarios");

            // 1. Encriptamos la contraseña que escribió el usuario
            String passwordHasheado = Encriptador.hashear(passwordPlano);

            // 2. Buscamos en MongoDB un documento que coincida con usuario Y contraseña hasheada
            Document doc = coleccion.find(
                    Filters.and(
                            Filters.eq("username", username),
                            Filters.eq("passwordHash", passwordHasheado)
                    )
            ).first();

            // 3. Si lo encontró, convertimos el Document a objeto Usuario
            if (doc != null) {
                return new Usuario(
                        doc.getString("username"),
                        doc.getString("passwordHash"),
                        doc.getString("rol")
                );
            }
            return null; // No existe o contraseña incorrecta

        } catch (Exception e) {
            System.err.println("Error al validar login: " + e.getMessage());
            return null;
        }
    }
}
