package mivalgamer.app;
import java.sql.*;
import java.time.LocalDate;

public class TarjetaCredito extends MetodoPago {
    private final double limiteCredito;

    /**
     * Constructor para crear nueva tarjeta de crédito.
     * Valida el número y la fecha de expiración.
     */
    public TarjetaCredito(Connection connection, String titular, String numero,
                          Date fechaExpiracion, String cvv, double limiteCredito) {
        super(connection, titular, validarNumeroTarjeta(numero), validarFechaExpiracion(fechaExpiracion), cvv, TipoMetodoPago.CREDITO);
        this.limiteCredito = limiteCredito;
    }

    /**
     * Constructor para cargar desde la BD.
     */
    public TarjetaCredito(Connection connection, ResultSet rs) throws SQLException {
        super(connection,
                rs.getString("titular"),
                rs.getString("numero"),
                rs.getDate("fecha_expiracion"),
                rs.getString("cvv"),
                TipoMetodoPago.CREDITO
        );
        this.idMetodo = rs.getInt("id_metodo");
        this.limiteCredito = rs.getDouble("limite_credito");
    }

    @Override
    public boolean procesarPago(double monto) throws SQLException {
        if (monto > limiteCredito) {
            throw new SQLException("Límite de crédito excedido");
        }

        String sql = "INSERT INTO transaccion (id_transaccion, tipo, monto, fecha, id_metodo) " +
                "VALUES (?, 'PAGO', ?, CURDATE(), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, generarIdTransaccion());
            stmt.setDouble(2, monto);
            stmt.setInt(3, idMetodo);
            return stmt.executeUpdate() > 0;
        }
    }

    public double getLimiteCredito() { return limiteCredito; }

    /**
     * Valida que el número de la tarjeta sea exactamente de 10 dígitos numéricos.
     */
    private static String validarNumeroTarjeta(String numero) {
        if (numero == null || !numero.matches("\\d{10}")) {
            throw new IllegalArgumentException("Introduce una tarjeta valida (10 dígitos numéricos)");
        }
        return numero;
    }

    /**
     * Valida que la fecha sea válida y no haya caducado.
     */
    private static Date validarFechaExpiracion(Date fechaExpiracion) {
        if (fechaExpiracion == null) {
            throw new IllegalArgumentException("Por favor ingrese una tarjeta valida");
        }
        LocalDate fecha = fechaExpiracion.toLocalDate();
        LocalDate hoy = LocalDate.now();
        int mes = fecha.getMonthValue();
        int anio = fecha.getYear();

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mes de expiración inválido");
        }
        if (fecha.isBefore(hoy)) {
            throw new IllegalArgumentException("Tarjeta caducada");
        }
        return fechaExpiracion;
    }
}