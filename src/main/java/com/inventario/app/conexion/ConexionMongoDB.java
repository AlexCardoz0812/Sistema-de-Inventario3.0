package com.inventario.app.conexion;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.io.InputStream;
import java.util.Properties;

public class ConexionMongoDB {
    private static MongoClient mongoClient = null;
    private static MongoDatabase database = null;
    
    public static MongoDatabase getDatabase() {
        if (database == null) {
            try {
                Properties props = new Properties();
                InputStream input = ConexionMongoDB.class.getClassLoader().getResourceAsStream("config.properties");
                
                String uri = "mongodb://localhost:27017"; // Por defecto
                String dbName = "inventario_db";
                
                if (input != null) {
                    props.load(input);
                    uri = props.getProperty("mongo.uri", uri);
                    dbName = props.getProperty("mongo.db", dbName);
                } else {
                    System.out.println("⚠️ No se encontró config.properties. Usando configuración por defecto.");
                }
                
                mongoClient = MongoClients.create(uri);
                database = mongoClient.getDatabase(dbName);
                System.out.println("✅ Conexión exitosa a MongoDB");
                
                // Crear usuario por defecto si no existe
                crearUsuarioPorDefecto();
                
            } catch (Exception e) {
                System.err.println("❌ Error al conectar a MongoDB: " + e.getMessage());
            }
        }
        return database;
    }
    
    private static void crearUsuarioPorDefecto() {
        try {
            MongoCollection<Document> usuarios = database.getCollection("usuarios");
            // Si la colección está vacía, insertamos el admin
            if (usuarios.countDocuments() == 0) {
                Document admin = new Document("username", "admin")
                        .append("passwordHash", "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92")
                        .append("rol", "MASTER");
                usuarios.insertOne(admin);
                System.out.println("🌟 Usuario 'admin' creado automáticamente (Contraseña: 123456).");
            }
        } catch (Exception e) {
            System.err.println("Error al crear usuario por defecto: " + e.getMessage());
        }
    }
    
    public static void cerrarConexion() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}