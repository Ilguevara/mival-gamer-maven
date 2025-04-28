package mivalgamer.app;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class PedidoFactory {
    private static final Logger LOGGER = Logger.getLogger(PedidoFactory.class.getName());

    public static Pedido crearPedidoDesdeCarrito(Connection connection, List<ItemCarrito> itemsCarrito, int idMetodoPago,
                                                 String codigoDescuento, Usuario usuario) throws SQLException {
        // Desactivar auto-commit para usar transacciones
        connection.setAutoCommit(false);
        try {
            // Convertir items del carrito a items de pedido
            List<ItemPedido> itemsPedido = convertirItems(connection, itemsCarrito);

            // Calcular descuento
            double descuento = calcularDescuento(connection, codigoDescuento, itemsPedido);

            // Crear objeto Pedido
            Pedido pedido = new Pedido(
                    generarIdPedido(), // Generamos un ID único
                    usuario,
                    LocalDateTime.now(), // Fecha actual
                    idMetodoPago,
                    descuento,
                    calcularImpuestos(connection),
                    EstadoPedido.PAGADO,
                    connection
            );

            // Asignar items al pedido
            pedido.setItems(itemsPedido);

            // Guardar en BD
            pedido.guardarEnBD();

            // Marcar descuento como usado si aplica
            if (codigoDescuento != null && !codigoDescuento.isEmpty()) {
                marcarDescuentoUsado(connection, codigoDescuento);
            }

            // Hacer commit a la transacción
            connection.commit();

            return pedido;
        } catch (SQLException e) {
            // Solo hacer rollback si autocommit está desactivado
            if (!connection.getAutoCommit()) {
                connection.rollback();
            } else {
                LOGGER.warning("No se pudo hacer rollback porque autoCommit está en true");
            }
            LOGGER.severe("Error al crear pedido: " + e.getMessage());
            throw e;
        } finally {
            // Restaurar el auto-commit a true después de la transacción
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.severe("Mensaje de error: " + e.getMessage());
            }
        }
    }



    private static String generarIdPedido() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static List<ItemPedido> convertirItems(Connection conn, List<ItemCarrito> itemsCarrito) throws SQLException {
        List<ItemPedido> itemsPedido = new ArrayList<>();
        for (ItemCarrito item : itemsCarrito) {
            Videojuego juego = Videojuego.obtenerPorId(conn, item.getVideojuego().getIdVideojuego());
            itemsPedido.add(new ItemPedido(
                    null, // id_item se generará automáticamente
                    juego,
                    juego.getPrecio(),
                    item.getCantidad()
            ));
        }
        return itemsPedido;
    }

    private static double calcularDescuento(Connection conn, String codigoDescuento,
                                            List<ItemPedido> items) throws SQLException {
        if (codigoDescuento == null || codigoDescuento.isEmpty()) {
            return 0.0;
        }

        String sql = "SELECT valor, tipo FROM descuento " +
                "WHERE codigo = ? AND fecha_inicio <= CURRENT_DATE " +
                "AND fecha_expiracion >= CURRENT_DATE AND es_activo = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoDescuento);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double valor = rs.getDouble("valor"); // Cambié de 'porcentaje' a 'valor'
                    String tipo = rs.getString("tipo");
                    double subtotal = items.stream()
                            .mapToDouble(ItemPedido::getSubtotal)
                            .sum();

                    if (tipo.equals("PORCENTAJE")) {
                        return subtotal * (valor / 100);
                    } else if (tipo.equals("MONTO_FIJO")) {
                        return valor; // En este caso, se descuenta un monto fijo
                    }
                }
            }
        }
        throw new SQLException("Código de descuento no válido o expirado");
    }

    private static double calcularImpuestos(Connection conn) throws SQLException {
        // Usamos un valor fijo para el IVA (16%) si la tabla 'configuracion' no existe
        try {
            String sql = "SELECT valor FROM configuracion WHERE clave = 'IVA'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getDouble("valor") / 100 : 0.19; // Retorna el valor de la tabla o 0.16 por defecto
            }
        } catch (SQLException e) {
            // Si la tabla no existe, devolver un valor predeterminado
            LOGGER.warning("Tabla 'configuracion' no encontrada. Usando valor predeterminado de 16% para IVA.");
            return 0.16; // Valor por defecto (16%)
        }
    }


    private static void marcarDescuentoUsado(Connection conn, String codigo) throws SQLException {
        String sql = "UPDATE descuento SET es_acumulable = FALSE WHERE codigo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            stmt.executeUpdate();
        }
    }
}
