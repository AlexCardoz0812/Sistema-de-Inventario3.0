
package com.inventario.app.dao;

public class UsuarioDAO {
    //Retorna el Usuario si las credenciales son correctas, o null si fallan
    public Usuario validarLogin(String username, String passwordPlano) {
        MongoDatabase db = ConexionMongoDB.getDatabase();
    }
}
