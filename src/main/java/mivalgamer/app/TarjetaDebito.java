package mivalgamer.app;
import java.sql.*;

/**
 * Representa una tarjeta de débito como método de pago.
 * Extiende la clase abstracta MetodoPago e implementa el procesamiento específico
 * para pagos con tarjeta de débito.
 */
public class TarjetaDebito extends MetodoPago {
    private final String numeroCuenta;

    /**
     * Constructor para crear una nueva tarjeta de débito (antes de guardar en BD).
     *
     * @param connection Conexión a la base de datos
     * @param titular Nombre del titular de la tarjeta
     * @param numero Número de la tarjeta de débito
     * @param fechaExpiracion Fecha de expiración (yyyy-MM-dd)
     * @param cvv Código de seguridad
     * @param numeroCuenta Número de cuenta asociado
     */
    public TarjetaDebito(Connection connection, String titular, String numero,
                         Date fechaExpiracion, String cvv, String numeroCuenta) {
        super(connection, titular, numero, fechaExpiracion, cvv, TipoMetodoPago.DEBITO);
        this.numeroCuenta = numeroCuenta;
    }

    /**
     * Constructor para cargar una tarjeta existente desde la base de datos.
     *
     * @param connection Conexión a la base de datos
     * @param rs ResultSet con los datos de la tarjeta
     * @throws SQLException Si hay un error al acceder a los datos
     */
    public TarjetaDebito(Connection connection, ResultSet rs) throws SQLException {
        super(connection,
                rs.getString("titular"),
                rs.getString("numero"),
                rs.getDate("fecha_expiracion"),
                rs.getString("cvv"),
                TipoMetodoPago.DEBITO
        );
        this.idMetodo = rs.getInt("id_metodo"); // Asignar el ID después de llamar al constructor
        this.numeroCuenta = rs.getString("numero_cuenta");
    }

    /**
     * Procesa un pago con la tarjeta de débito.
     *
     * @param monto Cantidad a pagar
     * @return true si el pago fue exitoso
     * @throws SQLException Si ocurre un error en la base de datos
     */
    @Override
    public boolean procesarPago(double monto) throws SQLException {
        String sql = "INSERT INTO transaccion (id_transaccion, tipo, monto, fecha, id_metodo) " +
                "VALUES (?, 'PAGO', ?, CURDATE(), ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, generarIdTransaccion());
            stmt.setDouble(2, monto);
            stmt.setInt(3, idMetodo);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Obtiene el número de cuenta asociado a la tarjeta.
     *
     * @return Número de cuenta bancaria
     */
    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    /**
     * Representación en String de la tarjeta de débito.
     *
     * @return String con información básica de la tarjeta
     */
    @Override
    public String toString() {
        return "TarjetaDebito{" +
                "id=" + idMetodo +
                ", titular='" + titular + '\'' +
                ", numero='" + numero.substring(0, 4) + "****" + numero.substring(numero.length() - 4) + '\'' +
                ", cuenta='" + numeroCuenta + '\'' +
                '}';
    }
}