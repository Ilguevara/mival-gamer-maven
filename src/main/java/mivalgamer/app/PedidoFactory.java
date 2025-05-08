package mivalgamer.app;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PedidoFactory {
    private static final Logger LOGGER = Logger.getLogger(PedidoFactory.class.getName());

    public static Pedido crearPedidoDesdeCarrito(Connection connection, List<ItemCarrito> itemsCarrito,
                                                 int idMetodoPago, String codigoDescuento,
                                                 Usuario usuario) throws SQLException {
        connection.setAutoCommit(false);
        try {
            List<ItemPedido> itemsPedido = convertirItems(connection, itemsCarrito);
            double descuento = calcularDescuento(connection, codigoDescuento, itemsPedido);

            Pedido pedido = new Pedido(
                    generarIdPedido(),
                    usuario,
                    LocalDateTime.now(),
                    idMetodoPago,
                    descuento,
                    calcularImpuestos(),
                    EstadoPedido.PAGADO,
                    connection
            );

            pedido.setItems(itemsPedido);
            pedido.guardarEnBD();

            // Procesar pago y agregar a biblioteca
            if (pedido.procesarPago()) {
                if (codigoDescuento != null && !codigoDescuento.isEmpty()) {
                    marcarDescuentoUsado(connection, codigoDescuento);
                }
                connection.commit();

                // Eliminada la línea que causaba el error:
                // usuario.setBiblioteca(new Biblioteca(usuario, connection));

                return pedido;
            } else {
                connection.rollback();
                throw new SQLException("El pago no pudo ser procesado");
            }
        } catch (SQLException e) {
            connection.rollback();
            LOGGER.log(Level.SEVERE, "Error al crear pedido", e);
            throw e;
        } finally {
            connection.setAutoCommit(true);
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
                    double valor = rs.getDouble("valor");
                    String tipo = rs.getString("tipo");
                    double subtotal = items.stream()
                            .mapToDouble(ItemPedido::getSubtotal)
                            .sum();

                    if (tipo.equals("PORCENTAJE")) {
                        return subtotal * (valor / 100);
                    } else if (tipo.equals("MONTO_FIJO")) {
                        return valor;
                    }
                }
            }
        }
        throw new SQLException("Código de descuento no válido o expirado");
    }

    private static double calcularImpuestos() {
        // IVA fijo del 19%
        return 0.19;
    }

    private static void marcarDescuentoUsado(Connection conn, String codigo) throws SQLException {
        String sql = "UPDATE descuento SET es_acumulable = FALSE WHERE codigo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            stmt.executeUpdate();
        }
    }
}