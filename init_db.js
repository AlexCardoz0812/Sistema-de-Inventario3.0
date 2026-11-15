// ==========================================
// SCRIPT DE INICIALIZACIÓN - SISTEMA DE INVENTARIO
// Proyecto POO - Universidad de El Salvador
// ==========================================

use inventario_db;

print("🔄 Iniciando configuración de la base de datos...");

// 1. Limpiar las colecciones para evitar duplicados si se corre varias veces
db.usuarios.drop();
db.productos.drop();
print("🧹 Colecciones anteriores eliminadas.");

// ==========================================
// 2. CREAR USUARIOS (Con roles para probar permisos)
// ==========================================
print("👥 Creando usuarios de prueba...");

db.usuarios.insertMany([
    {
        "username": "admin",
        // Contraseña: 123456 (Encriptada con SHA-256)
        "passwordHash": "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
        "rol": "MASTER"
    },
    {
        "username": "operador1",
        // Contraseña: 123456 (Encriptada con SHA-256)
        "passwordHash": "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
        "rol": "OPERADOR"
    }
]);

// ==========================================
// 3. CREAR PRODUCTOS (Mínimo 6 para la defensa)
// ==========================================
print("📦 Insertando productos de prueba...");

db.productos.insertMany([
    {
        "codigo": "PROD-001",
        "nombre": "Laptop HP ProBook",
        "descripcion": "Core i5, 16GB RAM, 512GB SSD",
        "categoria": "Electrónica",
        "precio": 750.50,
        "cantidadDisponible": 10
    },
    {
        "codigo": "PROD-002",
        "nombre": "Mouse Inalámbrico Logitech",
        "descripcion": "Mouse ergonómico color negro",
        "categoria": "Electrónica",
        "precio": 18.99,
        "cantidadDisponible": 25
    },
    {
        "codigo": "PROD-003",
        "nombre": "Resma de Papel Bond",
        "descripcion": "Papel tamaño carta, 500 hojas",
        "categoria": "Oficina",
        "precio": 5.25,
        "cantidadDisponible": 50
    },
    {
        "codigo": "PROD-004",
        "nombre": "Teclado Mecánico Redragon",
        "descripcion": "Teclado RGB switch azul",
        "categoria": "Electrónica",
        "precio": 45.00,
        "cantidadDisponible": 3 // Stock bajo (Para probar las alertas)
    },
    {
        "codigo": "PROD-005",
        "nombre": "Cloro Multiusos 1L",
        "descripcion": "Limpiador desinfectante",
        "categoria": "Limpieza",
        "precio": 2.75,
        "cantidadDisponible": 0 // Agotado (Para probar el reporte de 0 existencias)
    },
    {
        "codigo": "PROD-006",
        "nombre": "Martillo de Acero",
        "descripcion": "Mango de madera, 16 oz",
        "categoria": "Herramientas",
        "precio": 12.50,
        "cantidadDisponible": 15
    }
]);

print("¡Base de datos 'inventario_db' inicializada correctamente!");
print("CREDENCIALES DE PRUEBA:");
print(" -> MASTER (Acceso total): admin / 123456");
print(" -> OPERADOR (Solo stock): operador1 / 123456");