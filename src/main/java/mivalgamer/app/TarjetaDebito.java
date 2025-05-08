package mivalgamer.app;
import java.sql.*;
import java.time.LocalDate;

/**
 * Representa una tarjeta de débito como método de pago.
 */
public class TarjetaDebito extends MetodoPago {
    private final String numeroCuenta;

    /**
     * Constructor para crear una nueva tarjeta de débito (antes de guardar en BD).
     * El número de cuenta es el mismo que el número de tarjeta y se almacena como tal.
     * Valida el número y fecha igual que crédito.
     */
    public TarjetaDebito(Connection connection, String titular, String numero,
                         Date fechaExpiracion, String cvv) {
        super(connection, titular, validarNumeroTarjeta(numero), validarFechaExpiracion(fechaExpiracion), cvv, TipoMetodoPago.DEBITO);
        this.numeroCuenta = numero; // Almacena en la BD como numero_cuenta
    }

    /**
     * Constructor para cargar una tarjeta existente desde la base de datos.
     */
    public TarjetaDebito(Connection connection, ResultSet rs) throws SQLException {
        super(connection,
                rs.getString("titular"),
                rs.getString("numero"),
                rs.getDate("fecha_expiracion"),
                rs.getString("cvv"),
                TipoMetodoPago.DEBITO
        );
        this.idMetodo = rs.getInt("id_metodo");
        this.numeroCuenta = rs.getString("numero_cuenta");
    }

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
     * Obtiene el número de cuenta (igual al número de tarjeta).
     */
    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    @Override
    public String toString() {
        return "TarjetaDebito{" +
                "id=" + idMetodo +
                ", titular='" + titular + '\'' +
                ", numero='" + numero.substring(0, 4) + "****" + numero.substring(numero.length() - 4) + '\'' +
                ", cuenta='" + numeroCuenta + '\'' +
                '}';
    }

    /**
     * Valida el número de tarjeta (debe ser de 10 dígitos).
     */
    private static String validarNumeroTarjeta(String numero) {
        if (numero == null || !numero.matches("\\d{10}")) {
            throw new IllegalArgumentException("Introduce una tarjeta valida (10 dígitos numéricos)");
        }
        return numero;
    }

    /**
     * Valida la fecha igual que en crédito.
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