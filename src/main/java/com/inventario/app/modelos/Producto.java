
package com.inventario.app.modelos;

import com.inventario.app.excepciones.DatoInvalidoException;

public class Producto {
    private String codigo;
    private String nombre;
    private String descripcion; //evaluar quitar
    private String categoria;
    private double precio;
    private int cantidadDisponible;

    public Producto() {
    }

    public Producto(String codigo, String nombre, String descripcion, String categoria, double precio, int cantidadDisponible)
            throws DatoInvalidoException {
        validar(codigo, nombre, descripcion, categoria, precio, cantidadDisponible);
        this.codigo = codigo.trim().toUpperCase();
        this.nombre = nombre.trim();
        this.descripcion = descripcion.trim();
        this.categoria = categoria.trim();
        this.precio = precio;
        this.cantidadDisponible = cantidadDisponible;
    }

    private void validar(String codigo, String nombre, String descripcion, String categoria, double precio, int cantidad) 
            throws DatoInvalidoException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DatoInvalidoException("El código no puede estar vacío.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new DatoInvalidoException("El nombre del producto es obligatorio.");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new DatoInvalidoException("La descripción del producto es obligatoria.");
        }
        if (categoria == null || categoria.trim().isEmpty() || categoria.equalsIgnoreCase("Seleccione una categoría")) {
            throw new DatoInvalidoException("Debe seleccionar una categoría válida.");
        }
        if (!Double.isFinite(precio) || precio <= 0) {
    throw new DatoInvalidoException(
        "El precio debe ser un número válido mayor a 0.00.");
        }
        if (cantidad < 0) {
            throw new DatoInvalidoException("Las existencias no pueden ser negativas.");
        }
    }
    
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = (codigo != null) ? codigo.trim().toUpperCase(): null;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = (nombre != null) ? nombre.trim() : null;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = (descripcion != null) ? descripcion.trim() : "";
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = (categoria != null) ? categoria.trim() : null;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
    if (!Double.isFinite(precio) || precio <= 0) {
        throw new IllegalArgumentException("El precio debe ser un número válido mayor a cero.");
    }

    this.precio = precio;
}

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
    if (cantidadDisponible < 0) {
        throw new IllegalArgumentException("Las existencias no pueden ser negativas.");
    }

    this.cantidadDisponible = cantidadDisponible;
}
    
@Override
    public String toString() {
        return String.format("[%s] %s (%s) - $%.2f | Stock: %d", 
                codigo, nombre, categoria, precio, cantidadDisponible);
    }    
    
    //Metodo para reportes
    public double calcularValorTotal() {
        return this.precio * this.cantidadDisponible;
    }
}
