package com.inventario.app.dao;

import com.inventario.app.conexion.ConexionMongoDB;
import com.inventario.app.modelos.Usuario;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

public class UsuarioDAO {

    public Usuario validarLogin(String username, String passwordHash) {
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            MongoCollection<Document> coleccion = db.getCollection("usuarios");

            Document doc = coleccion.find(
                    Filters.and(
                            Filters.eq("username", username),
                            Filters.eq("passwordHash", passwordHash)
                    )
            ).first();

            if (doc != null) {
                return new Usuario(
                        doc.getString("username"),
                        doc.getString("passwordHash"),
                        doc.getString("rol")
                );
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error al validar login: " + e.getMessage());
            return null;
        }
    }

    public boolean crearUsuario(Usuario nuevoUsuario) {
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            if (db == null) {
                System.err.println(">>> ERROR CRÍTICO: ConexionMongoDB.getDatabase() retornó null.");
                return false;
            }

            MongoCollection<Document> coleccion = db.getCollection("usuarios");

            Document existente = coleccion.find(Filters.eq("username", nuevoUsuario.getUsername())).first();
            if (existente != null) {
                System.out.println(">>> AVISO: El usuario [" + nuevoUsuario.getUsername() + "] ya existe en MongoDB.");
                return false;
            }

            Document doc = new Document("username", nuevoUsuario.getUsername())
                    .append("passwordHash", nuevoUsuario.getPasswordHash())
                    .append("rol", nuevoUsuario.getRol());

            coleccion.insertOne(doc);
            System.out.println(">>> ÉXITO: Usuario [" + nuevoUsuario.getUsername() + "] insertado correctamente con ID: " + doc.getObjectId("_id"));
            return true;

        } catch (Exception e) {
            System.err.println(">>> EXCEPCIÓN AL INSERTAR EN MONGO:");
            e.printStackTrace();
            return false;
        }
    }

    public List<Usuario> listarUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            MongoCollection<Document> coleccion = db.getCollection("usuarios");

            for (Document doc : coleccion.find()) {
                lista.add(new Usuario(
                        doc.getString("username"),
                        doc.getString("passwordHash"),
                        doc.getString("rol")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizarUsuario(String username, String nuevoPasswordHash, String nuevoRol) {
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            MongoCollection<Document> coleccion = db.getCollection("usuarios");

            if (nuevoPasswordHash != null && !nuevoPasswordHash.trim().isEmpty()) {
                coleccion.updateOne(
                        Filters.eq("username", username),
                        Updates.combine(
                                Updates.set("rol", nuevoRol),
                                Updates.set("passwordHash", nuevoPasswordHash)
                        )
                );
            } else {
                coleccion.updateOne(
                        Filters.eq("username", username),
                        Updates.set("rol", nuevoRol)
                );
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean existeUsuario(String username) {
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            if (db == null) {
                return false;
            }
            MongoCollection<Document> coleccion = db.getCollection("usuarios");
            return coleccion.find(Filters.eq("username", username)).first() != null;
        } catch (Exception e) {
            System.err.println("Error al verificar existencia de usuario: " + e.getMessage());
            return false;
        }
    }
    
    public boolean eliminarUsuario(String username) {
        if (username == null || username.trim().isEmpty() 
                || com.inventario.app.utilidades.SeguridadUtil.MASTER_DEFAULT_USER.equalsIgnoreCase(username.trim())) {
            System.err.println(">>> AVISO DE SEGURIDAD: No se puede eliminar el usuario maestro por defecto.");
            return false;
        }
        try {
            MongoDatabase db = ConexionMongoDB.getDatabase();
            if (db == null) {
                return false;
            }
            MongoCollection<Document> coleccion = db.getCollection("usuarios");
            return coleccion.deleteOne(Filters.eq("username", username.trim())).getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }
}