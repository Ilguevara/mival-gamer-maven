package mivalgamer.app;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemPedido {
    private final String idPedido;
    private final Videojuego juego;
    private final double precioUnitario;
    private final int cantidad;
    private final double subtotal;

    public ItemPedido(String idPedido, Videojuego juego, double precioUnitario, int cantidad) {
        this.idPedido = idPedido;
        this.juego = juego;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = precioUnitario * cantidad;
    }

    public static List<ItemPedido> obtenerPorPedido(String idPedido, Connection conn) throws SQLException {
        List<ItemPedido> items = new ArrayList<>();
        String sql = "SELECT ip.*, v.* FROM item_pedido ip " +
                "JOIN videojuego v ON ip.id_videojuego = v.id_videojuego " +
                "WHERE ip.id_pedido = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Videojuego juego = Videojuego.fromResultSet(conn, rs);
                    items.add(new ItemPedido(
                            rs.getString("id_pedido"),
                            juego,
                            rs.getDouble("precio_unitario"),
                            rs.getInt("cantidad")
                    ));
                }
            }
        }
        return items;
    }

    // Getters
    public String getIdPedido() { return idPedido; }
    public Videojuego getJuego() { return juego; }
    public double getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
    public double getSubtotal() { return subtotal; }
}