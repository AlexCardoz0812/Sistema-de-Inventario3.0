# Sistema de Inventario - Proyecto de Programación Orientada a Objetos

Este proyecto consiste en un sistema de escritorio desarrollado en Java para la gestión de inventarios, aplicando los principios de la Programación Orientada a Objetos y persistencia de datos en MongoDB.

## Tecnologías Utilizadas
- **Lenguaje:** Java (JDK 21)
- **Interfaz Gráfica:** Java Swing
- **Base de Datos:** MongoDB
- **Gestor de Dependencias:** Maven
- **Control de Versiones:** Git y GitHub

## Instrucciones de Instalación y Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/ErickDelga/Sistema-de-Inventario2.0

2. **Ejecución de pruebas**

Para verificar que la lógica y las conexiones funcionen correctamente, ejecuta las pruebas unitarias con Maven:

* **Desde la terminal (en la raíz del proyecto):**

##  ```bash
  mvn test
##

Credenciales de Prueba
- **Usuario:** `admin`
- **Contraseña:** `123456`

*(El usuario se crea automáticamente al iniciar la app si la base de datos está vacía, o puedes ejecutar el script `init_db.js` manualmente en MongoDB)*.

**Ejecutar la aplicación:**
   - Abre el proyecto en NetBeans.
   - Haz clic derecho en `Sistema_de_inventario.java` y selecciona "Run File".
   - *Nota: La primera vez que se ejecute, la aplicación creará automáticamente el usuario administrador.*

## Autores

- Erick Edgardo Delgado Palacios
- Josseline Marisol Bonilla Estrada
- Alexandra Sorto
- Oscar Alexander Cardoza Recinos