import java.sql.*;
import java.util.UUID;

/**
 * Clase abstracta que representa un método de pago genérico.
 * Proporciona la base para implementaciones específicas de métodos de pago como
 * tarjetas de crédito y débito.
 */
public abstract class MetodoPago {
    /** Conexión a la base de datos */
    protected final Connection connection;

    /** Identificador único del método de pago (generado por la BD) */
    protected Integer idMetodo = null;

    /** Nombre del titular del método de pago */
    protected final String titular;

    /** Número de la tarjeta o cuenta */
    protected final String numero;

    /** Fecha de expiración (para tarjetas) */
    protected final Date fechaExpiracion;

    /** Código de seguridad (CVV) */
    protected final String cvv;

    /** Tipo de método de pago (CREDITO/DEBITO) */
    protected final TipoMetodoPago tipo;

    /**
     * Constructor protegido para métodos de pago.
     *
     * @param connection Conexión a la base de datos
     * @param titular Nombre del titular
     * @param numero Número de tarjeta/cuenta
     * @param fechaExpiracion Fecha de expiración
     * @param cvv Código de seguridad
     * @param tipo Tipo de método de pago
     */
    protected MetodoPago(Connection connection, String titular, String numero,
                         Date fechaExpiracion, String cvv, TipoMetodoPago tipo) {
        this.connection = connection;
        this.titular = titular;
        this.numero = numero;
        this.fechaExpiracion = fechaExpiracion;
        this.cvv = cvv;
        this.tipo = tipo;
    }

    /**
     * Guarda el método de pago en la base de datos.
     * Asigna automáticamente el ID generado por la BD.
     *
     * @throws SQLException Si ocurre un error al acceder a la base de datos
     */
    public void guardarEnBD() throws SQLException {
        // Insertar en tabla metodo_pago
        String sql = "INSERT INTO metodo_pago (titular, numero, fecha_expiracion, cvv, tipo) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, titular);
            stmt.setString(2, numero);
            stmt.setDate(3, fechaExpiracion);
            stmt.setString(4, cvv);
            stmt.setString(5, tipo.toString());

            stmt.executeUpdate();

            // Obtener el ID generado
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    this.idMetodo = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("No se pudo obtener el ID generado");
                }
            }
        }

        // Insertar en tabla específica (credito/debito)
        if (this instanceof TarjetaCredito) {
            String sqlCredito = "INSERT INTO tarjeta_credito (id_metodo, limite_credito) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sqlCredito)) {
                stmt.setInt(1, idMetodo);
                stmt.setDouble(2, ((TarjetaCredito) this).getLimiteCredito());
                stmt.executeUpdate();
            }
        } else if (this instanceof TarjetaDebito) {
            String sqlDebito = "INSERT INTO tarjeta_debito (id_metodo, numero_cuenta) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sqlDebito)) {
                stmt.setInt(1, idMetodo);
                stmt.setString(2, ((TarjetaDebito) this).getNumeroCuenta());
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Carga un método de pago desde la base de datos.
     *
     * @param idMetodo ID del método a cargar
     * @param conn Conexión a la base de datos
     * @return Instancia del método de pago
     * @throws SQLException Si ocurre un error al acceder a la BD o no se encuentra el método
     */
    public static MetodoPago cargarDesdeBD(int idMetodo, Connection conn) throws SQLException {
        String sql = "SELECT mp.*, " +
                "COALESCE(tc.limite_credito, 0) as limite_credito, " +
                "COALESCE(td.numero_cuenta, '') as numero_cuenta " +
                "FROM metodo_pago mp " +
                "LEFT JOIN tarjeta_credito tc ON mp.id_metodo = tc.id_metodo " +
                "LEFT JOIN tarjeta_debito td ON mp.id_metodo = td.id_metodo " +
                "WHERE mp.id_metodo = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMetodo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TipoMetodoPago tipo = TipoMetodoPago.valueOf(rs.getString("tipo"));
                    return switch (tipo) {
                        case CREDITO -> new TarjetaCredito(conn, rs);
                        case DEBITO -> new TarjetaDebito(conn, rs);
                    };
                }
            }
        }
        throw new SQLException("Método de pago no encontrado");
    }

    /**
     * Procesa un pago con este método.
     * Método abstracto que debe ser implementado por las subclases.
     *
     * @param monto Cantidad a pagar
     * @return true si el pago fue exitoso
     * @throws SQLException Si ocurre un error al procesar el pago
     */
    public abstract boolean procesarPago(double monto) throws SQLException;

    /**
     * Genera un ID único para transacciones.
     *
     * @return ID de transacción generado con formato TXN-XXXXXX
     */
    protected String generarIdTransaccion() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Enumeración de tipos de métodos de pago.
     */
    public enum TipoMetodoPago {
        /** Tarjeta de crédito */
        CREDITO,
        /** Tarjeta de débito */
        DEBITO
    }

    // Getters con documentación

    /**
     * Obtiene el ID del método de pago.
     * @return ID del método de pago (null si no se ha guardado en BD)
     */
    public Integer getIdMetodo() { return idMetodo; }

    /**
     * Obtiene el nombre del titular.
     * @return Nombre del titular
     */
    public String getTitular() { return titular; }

    /**
     * Obtiene el número de tarjeta/cuenta.
     * @return Número de tarjeta/cuenta
     */
    public String getNumero() { return numero; }

    /**
     * Obtiene la fecha de expiración.
     * @return Fecha de expiración
     */
    public Date getFechaExpiracion() { return fechaExpiracion; }

    /**
     * Obtiene el tipo de método de pago.
     * @return Tipo de método de pago
     */
    public TipoMetodoPago getTipo() { return tipo; }
}