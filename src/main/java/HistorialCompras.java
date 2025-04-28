import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistorialCompras {
    private static final Logger LOGGER = Logger.getLogger(HistorialCompras.class.getName());

    private final Usuario usuario;
    private final Connection connection;

    public HistorialCompras(Usuario usuario, Connection connection) {
        if (usuario == null || connection == null) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }
        this.usuario = usuario;
        this.connection = connection;
    }

    public void registrarPedido(Pedido pedido) {
        String sql = "INSERT INTO historial_compras (id_usuario, id_pedido, fecha_compra, total) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setString(2, pedido.getIdPedido());
            stmt.setDate(3, Date.valueOf(pedido.getFechaCreacion().toLocalDate()));
            stmt.setDouble(4, pedido.getTotal());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al registrar pedido", ex);
            throw new RuntimeException("Error en historial", ex);
        }
    }

    public List<Pedido> getPedidosRecientes() {
        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT p.* FROM pedido p WHERE p.id_usuario = ? ORDER BY p.fecha_creacion DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(mapPedido(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener pedidos", ex);
        }
        return pedidos;
    }

    private Pedido mapPedido(ResultSet rs) throws SQLException {
        return new Pedido(
                rs.getString("id_pedido"),
                null,// idPedido
                rs.getTimestamp("fecha_creacion").toLocalDateTime(), // fechaCreacion
                rs.getInt("metodo_pago"),      // metodoPagoId
                rs.getDouble("descuento_total"),  // descuentoTotal
                rs.getDouble("impuestos"),        // impuestos
                EstadoPedido.desdeString(rs.getString("estado")), // estado
                connection                        // connection
        );
    }

    public double getGastoTotal() {
        String sql = "SELECT SUM(total) AS total FROM historial_compras WHERE id_usuario = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al calcular gasto", ex);
            return 0.0;
        }
    }
}