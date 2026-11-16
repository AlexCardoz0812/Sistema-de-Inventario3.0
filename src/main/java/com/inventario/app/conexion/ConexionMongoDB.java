package com.inventario.app.conexion;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.io.InputStream;
import java.util.Arrays;
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
                System.out.println("Conexión exitosa a MongoDB");
                
                // Inicializar datos de prueba si la base de datos está vacía
                inicializarDatosDePrueba();
                
            } catch (Exception e) {
                System.err.println("Error al conectar a MongoDB: " + e.getMessage());
            }
        }
        return database;
    }
    
    private static void inicializarDatosDePrueba() {
        try {
            MongoCollection<Document> usuarios = database.getCollection("usuarios");
            MongoCollection<Document> productos = database.getCollection("productos");

            // Verificamos si ya hay datos. Si la colección de usuarios está vacía, asumimos que es la primera vez.
            if (usuarios.countDocuments() == 0) {
                System.out.println("Base de datos vacía. Creando datos de prueba automáticamente...");

                // 1. Crear Usuarios
                // Hash SHA-256 de "123456"
                String hash123456 = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
                
                Document admin = new Document("username", "admin")
                        .append("passwordHash", hash123456)
                        .append("rol", "MASTER");
                
                Document operador = new Document("username", "operador1")
                        .append("passwordHash", hash123456)
                        .append("rol", "OPERADOR");

                usuarios.insertMany(Arrays.asList(admin, operador));

                // 2. Crear Productos
                Document p1 = new Document("codigo", "PROD-001").append("nombre", "Laptop HP ProBook").append("descripcion", "Core i5, 16GB RAM").append("categoria", "Electrónica").append("precio", 750.50).append("cantidadDisponible", 10);
                Document p2 = new Document("codigo", "PROD-002").append("nombre", "Mouse Inalámbrico").append("descripcion", "Mouse ergonómico").append("categoria", "Electrónica").append("precio", 18.99).append("cantidadDisponible", 25);
                Document p3 = new Document("codigo", "PROD-003").append("nombre", "Resma de Papel").append("descripcion", "Papel tamaño carta").append("categoria", "Oficina").append("precio", 5.25).append("cantidadDisponible", 50);
                Document p4 = new Document("codigo", "PROD-004").append("nombre", "Teclado Mecánico").append("descripcion", "Teclado RGB").append("categoria", "Electrónica").append("precio", 45.00).append("cantidadDisponible", 3); // Stock bajo
                Document p5 = new Document("codigo", "PROD-005").append("nombre", "Cloro Multiusos").append("descripcion", "Limpiador 1L").append("categoria", "Limpieza").append("precio", 2.75).append("cantidadDisponible", 0); // Agotado
                Document p6 = new Document("codigo", "PROD-006").append("nombre", "Martillo de Acero").append("descripcion", "Mango de madera").append("categoria", "Herramientas").append("precio", 12.50).append("cantidadDisponible", 15);

                productos.insertMany(Arrays.asList(p1, p2, p3, p4, p5, p6));

                System.out.println("Usuarios y 6 productos de prueba creados con éxito.");
                System.out.println("Credenciales -> admin/123456 y operador1/123456");
            } else {
                System.out.println("ℹ️ La base de datos ya contiene información. Omitiendo la creación de datos de prueba.");
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar datos de prueba: " + e.getMessage());
        }
    }
    
    public static void cerrarConexion() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}