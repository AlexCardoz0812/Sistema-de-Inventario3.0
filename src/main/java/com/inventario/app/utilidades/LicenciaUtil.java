package com.inventario.app.utilidades;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.prefs.Preferences;

public class LicenciaUtil {

    // Clave secreta maestra: DEBE ser exactamente igual en LicenciaUtil y GeneradorLicencias.
    // Cámbiala por una frase secreta propia antes de compilar tu instalador final.
    private static final String CLAVE_SECRETA_MAESTRA = "ALEX-INV-2026-CLAVE-PRIVADA-HMAC-#98765";
    private static final int DIAS_PRUEBA = 30;

    private static final Preferences PREFS = Preferences.userRoot().node("com/alexsoftware/inventario/licencia_v2");
    private static final String KEY_INICIO = "trial_start_epoch";
    private static final String KEY_ULTIMA_VISTA = "trial_last_seen_epoch";
    private static final String KEY_FIRMA_TRIAL = "trial_integrity_sig";
    private static final String KEY_LICENCIA_ACTIVA = "commercial_license_key";

    public enum EstadoLicencia {
        ACTIVADA,
        PRUEBA_VIGENTE,
        PRUEBA_EXPIRADA,
        RELOJ_MANIPULADO
    }

    public static class ResultadoVerificacion {
        private final EstadoLicencia estado;
        private final long diasRestantes;
        private final String hardwareId;

        public ResultadoVerificacion(EstadoLicencia estado, long diasRestantes, String hardwareId) {
            this.estado = estado;
            this.diasRestantes = diasRestantes;
            this.hardwareId = hardwareId;
        }

        public EstadoLicencia getEstado() { return estado; }
        public long getDiasRestantes() { return diasRestantes; }
        public String getHardwareId() { return hardwareId; }
        public boolean puedeUsarSistema() {
            return estado == EstadoLicencia.ACTIVADA || estado == EstadoLicencia.PRUEBA_VIGENTE;
        }
    }

    /**
     * Genera un ID único de 16 caracteres (formato XXXX-XXXX-XXXX-XXXX) vinculado al hardware del equipo.
     */
    public static String obtenerHardwareId() {
        try {
            String uuidPlaca = obtenerUUIDWindows();
            String nombreEquipo = InetAddress.getLocalHost().getHostName();
            String usuarioSO = System.getProperty("user.name", "USER");
            String base = uuidPlaca + "|" + nombreEquipo + "|" + usuarioSO;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02X", hash[i]));
            }
            String raw = hex.toString();
            return raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-"
                    + raw.substring(8, 12) + "-" + raw.substring(12, 16);
        } catch (Exception e) {
            return "FALLBACK-ID-2026";
        }
    }

    private static String obtenerUUIDWindows() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-Command",
                    "(Get-CimInstance -ClassName Win32_ComputerSystemProduct).UUID"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    linea = linea.trim();
                    if (!linea.isEmpty() && !linea.equalsIgnoreCase("UUID")) {
                        return linea;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return System.getenv("COMPUTERNAME") != null ? System.getenv("COMPUTERNAME") : "GENERIC-HW";
    }

    /**
     * Calcula la llave de activación esperada para un Hardware ID dado.
     */
    public static String generarLlaveParaHardware(String hardwareId) {
        try {
            String hwNormalizado = hardwareId.trim().toUpperCase();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    CLAVE_SECRETA_MAESTRA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(("LICENCIA-PERMANENTE:" + hwNormalizado).getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02X", hmacBytes[i]));
            }
            String cod = hex.toString(); // 12 caracteres hex
            return "ALEX-" + cod.substring(0, 4) + "-" + cod.substring(4, 8) + "-" + cod.substring(8, 12);
        } catch (Exception e) {
            throw new RuntimeException("Error criptográfico al generar licencia", e);
        }
    }

    /**
     * Valida y registra permanentemente una llave comercial en este equipo.
     */
    public static boolean activarLicencia(String llaveIngresada) {
        if (llaveIngresada == null || llaveIngresada.trim().isEmpty()) {
            return false;
        }
        String hwId = obtenerHardwareId();
        String llaveEsperada = generarLlaveParaHardware(hwId);
        if (llaveEsperada.equalsIgnoreCase(llaveIngresada.trim())) {
            PREFS.put(KEY_LICENCIA_ACTIVA, llaveEsperada);
            try { PREFS.flush(); } catch (Exception ignored) {}
            return true;
        }
        return false;
    }

    /**
     * Evalúa el estado actual del software (Activada, Trial de 30 días, Expirada o Manipulada).
     */
    public static ResultadoVerificacion verificarEstado() {
        String hwId = obtenerHardwareId();

        // 1. Verificar si ya tiene licencia permanente válida
        String llaveGuardada = PREFS.get(KEY_LICENCIA_ACTIVA, "");
        if (!llaveGuardada.isEmpty() && llaveGuardada.equalsIgnoreCase(generarLlaveParaHardware(hwId))) {
            return new ResultadoVerificacion(EstadoLicencia.ACTIVADA, -1, hwId);
        }

        // 2. Evaluar período de prueba de 30 días
        long hoyEpoch = LocalDate.now().toEpochDay();
        long inicioEpoch = PREFS.getLong(KEY_INICIO, -1L);
        long ultimaVistaEpoch = PREFS.getLong(KEY_ULTIMA_VISTA, -1L);
        String firmaGuardada = PREFS.get(KEY_FIRMA_TRIAL, "");

        if (inicioEpoch == -1L) {
            // Primera ejecución: iniciar prueba de 30 días
            guardarEstadoTrial(hwId, hoyEpoch, hoyEpoch);
            return new ResultadoVerificacion(EstadoLicencia.PRUEBA_VIGENTE, DIAS_PRUEBA, hwId);
        }

        // Verificar integridad (evitar que editen el registro manualmente)
        String firmaEsperada = firmarTrial(hwId, inicioEpoch, ultimaVistaEpoch);
        if (!firmaEsperada.equals(firmaGuardada)) {
            return new ResultadoVerificacion(EstadoLicencia.PRUEBA_EXPIRADA, 0, hwId);
        }

        // Detectar si atrasaron el reloj de Windows
        if (hoyEpoch < ultimaVistaEpoch) {
            return new ResultadoVerificacion(EstadoLicencia.RELOJ_MANIPULADO, 0, hwId);
        }

        // Actualizar última fecha vista
        if (hoyEpoch > ultimaVistaEpoch) {
            guardarEstadoTrial(hwId, inicioEpoch, hoyEpoch);
        }

        long diasTranscurridos = ChronoUnit.DAYS.between(
                LocalDate.ofEpochDay(inicioEpoch),
                LocalDate.ofEpochDay(hoyEpoch)
        );
        long restantes = DIAS_PRUEBA - diasTranscurridos;

        if (restantes > 0) {
            return new ResultadoVerificacion(EstadoLicencia.PRUEBA_VIGENTE, restantes, hwId);
        } else {
            return new ResultadoVerificacion(EstadoLicencia.PRUEBA_EXPIRADA, 0, hwId);
        }
    }

    private static void guardarEstadoTrial(String hwId, long inicio, long ultimaVista) {
        PREFS.putLong(KEY_INICIO, inicio);
        PREFS.putLong(KEY_ULTIMA_VISTA, ultimaVista);
        PREFS.put(KEY_FIRMA_TRIAL, firmarTrial(hwId, inicio, ultimaVista));
        try { PREFS.flush(); } catch (Exception ignored) {}
    }

    private static String firmarTrial(String hwId, long inicio, long ultimaVista) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CLAVE_SECRETA_MAESTRA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((hwId + ":" + inicio + ":" + ultimaVista).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}