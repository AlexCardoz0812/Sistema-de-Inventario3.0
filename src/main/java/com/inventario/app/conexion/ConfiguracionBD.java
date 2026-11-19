package com.inventario.app.conexion;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfiguracionBD {

    private static final String CARPETA_APP = System.getenv("LOCALAPPDATA") + File.separator + "SistemaInventario";
    private static final String RUTA_CONFIG = CARPETA_APP + File.separator + "config.properties";

    public static Properties cargarConfiguracion() {
        Properties props = new Properties();
        Path rutaArchivo = Paths.get(RUTA_CONFIG);

        try {
            if (!Files.exists(rutaArchivo)) {
                Files.createDirectories(Paths.get(CARPETA_APP));
                props.setProperty("mongo.uri", "mongodb://localhost:27017");
                props.setProperty("mongo.db", "inventario_db");

                try (OutputStream out = new FileOutputStream(RUTA_CONFIG)) {
                    props.store(out, "Configuracion de conexion a MongoDB - Sistema de Inventario");
                }
            } else {
                try (InputStream in = new FileInputStream(RUTA_CONFIG)) {
                    props.load(in);
                }
            }
        } catch (IOException e) {
            props.setProperty("mongo.uri", "mongodb://localhost:27017");
            props.setProperty("mongo.db", "inventario_db");
        }
        return props;
    }
}