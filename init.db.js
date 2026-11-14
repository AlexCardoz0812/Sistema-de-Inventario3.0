// Script de inicialización para MongoDB
// Ejecutar con: mongosh < init_db.js

// Crear y usar la base de datos
use inventario_db;

// Limpiar la colección de usuarios para evitar duplicados en pruebas
db.usuarios.deleteMany({});

// Insertar el usuario administrador por defecto
// Contraseña: 123456 (Encriptada con SHA-256)
db.usuarios.insertOne({
    "username": "admin",
    "passwordHash": "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
    "rol": "ADMIN"
});

print("Base de datos 'inventario_db' inicializada.");
print("Usuario 'admin' creado (Contraseña: 123456).");
