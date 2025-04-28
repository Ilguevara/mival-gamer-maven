import java.sql.*;

public class TarjetaCredito extends MetodoPago {
    private final double limiteCredito;

    // Constructor para crear nueva tarjeta
    public TarjetaCredito(Connection connection, String titular, String numero,
                          Date fechaExpiracion, String cvv, double limiteCredito) {
        super(connection, titular, numero, fechaExpiracion, cvv, TipoMetodoPago.CREDITO);
        this.limiteCredito = limiteCredito;
    }

    // Constructor para cargar desde BD
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


}