package com.inventario.app.conexion;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.swing.JOptionPane;
import java.util.Properties;

public class ConexionMongoDB {
    private static MongoClient mongoClient = null;
    private static MongoDatabase database = null;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            try {
                // 1. Carga o crea el archivo externo en AppData\Local\SistemaInventario\config.properties
                Properties props = ConfiguracionBD.cargarConfiguracion();

                String uri = props.getProperty("mongo.uri", "mongodb://localhost:27017");
                String dbName = props.getProperty("mongo.db", "inventario_db");

                mongoClient = MongoClients.create(uri);
                database = mongoClient.getDatabase(dbName);
                System.out.println("Conexión exitosa a MongoDB (" + dbName + ")");

                // 2. Crea el usuario MASTER inicial si es la primera vez que se instala en la PC
                inicializarUsuarioMaster();

            } catch (Exception e) {
                System.err.println("Error al conectar a MongoDB: " + e.getMessage());
                JOptionPane.showMessageDialog(
                        null,
                        "No se pudo conectar con la base de datos local (MongoDB).\n"
                        + "Verifique que el servicio de base de datos esté iniciado o revise su configuración.\n\n"
                        + "Detalle: " + e.getMessage(),
                        "Error de Conexión - Sistema de Inventario",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        return database;
    }

    private static void inicializarUsuarioMaster() {
        try {
            MongoCollection<Document> usuarios = database.getCollection("usuarios");

            if (usuarios.countDocuments() == 0) {
                System.out.println("Base de datos nueva. Creando usuario administrador inicial...");

                // Hash SHA-256 de "123456"
                String hash123456 = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

                Document admin = new Document("username", "admin")
                        .append("passwordHash", hash123456)
                        .append("rol", "MASTER");

                usuarios.insertOne(admin);
                System.out.println("Usuario inicial creado -> admin / 123456");
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar el usuario MASTER: " + e.getMessage());
        }
    }

    public static void cerrarConexion() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}