package com.inventario.app.servicio;

import com.inventario.app.dao.MovimientoDAO;
import com.inventario.app.modelos.Movimiento;
import java.util.List;

public class MovimientoServicio {
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();

    public List<Movimiento> obtenerHistorialCompleto() {
        return movimientoDAO.listarTodos();
    }
}