# Sistema de Inventario - Proyecto de Programación Orientada a Objetos

Este proyecto consiste en un sistema de escritorio desarrollado en Java para la gestión de
inventarios, aplicando los principios de la Programación Orientada a Objetos y persistencia de
datos en MongoDB. Incluye un sistema de control de acceso basado en roles y soporte para
múltiples sesiones.

## Tecnologías Utilizadas
 - Lenguaje: Java (JDK 21)

 - Interfaz Gráfica: Java Swing

 - Base de Datos: MongoDB (Driver 5.1.0)

 - Gestor de Dependencias: Maven

 - Pruebas Unitarias: JUnit 5

 - Control de Versiones: Git y GitHub

## Instrucciones de Instalación y Ejecución
Clonar el repositorio:

bash
git clone https://github.com/ErickDelga/Sistema-de-Inventario2.0

**Ejecutar la aplicación:**

En NetBeans, haz clic derecho sobre el archivo Sistema_de_inventario.java (ubicado en com.inventario.app) y selecciona Run File 
(o presiona Shift + F6).

Nota: "La primera vez que se ejecute la aplicación, se inicializará la base de datos con los usuarios de 
prueba y un catálogo de 6 productos de ejemplo."

## Credenciales de Prueba

El sistema cuenta con un mecanismo de seguridad basado en roles. Para la primera prueba, utiliza el usuario maestro:

| Rol | Usuario | Contraseña | Privilegios |
| :--- | :--- | :--- | :--- |
| **MASTER** | `admin` | `123456` | Acceso total (Inventario y Gestión de Usuarios) |
| **OPERADOR** | `operador1` | `123456` | Puede registrar productos y mover stock, pero no eliminarlos. |
| **CONSULTA** | *(Crea uno desde el Login)* | *(La que elijas)* | Solo lectura. No puede modificar nada. |

*Nota: Para probar el rol de CONSULTA, simplemente abre la aplicación y en la ventana de Login haz clic en el 
botón "Registrarse (Consulta)". Llena el formulario con el nombre de usuario y contraseña que tú prefieras. 
Al terminar, vuelve al Login e ingresa con esas credenciales; verás que el sistema te permite ver los productos, 
pero bloquea los botones para modificarlos*

## Funcionalidades Implementadas

- **Autenticación Segura:** Login con contraseñas encriptadas (SHA-256).
- **Multisesión:** La ventana de Login permanece abierta, permitiendo que múltiples usuarios inicien sesión concurrentemente en la misma máquina.
- **Roles y Permisos:** Restricciones de botones y acciones según el rol del usuario logueado.
- **CRUD de Productos:** Registrar, consultar, modificar y eliminar productos (sujeto a roles).
- **Gestión de Existencias:** Registrar entradas y salidas de stock con validación de stock insuficiente.
- **Reportes y Alertas:** Alerta visual de productos con stock bajo (<= 5) y cálculo automático del valor total del inventario.
- **Validaciones Robustas:** Verificación de precios válidos (mayores a 0, sin infinitos/NaN), cantidades no negativas, campos obligatorios y códigos duplicados.



## Nota sobre el registro: 
El botón "Registrarse" en el Login está diseñado únicamente para crear cuentas con el rol de CONSULTA (solo lectura). 
Si un usuario necesita privilegios de OPERADOR o ADMIN/MASTER para modificar el inventario, 
deberá ponerse en contacto con un administrador del sistema, quien podrá asignarle el rol correspondiente desde 
el panel de gestión de usuarios


## Autores

Erick Edgardo Delgado Palacios

Alexandra Sorto

Oscar Alexander Cardoza Recinos