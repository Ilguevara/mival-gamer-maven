package mivalgamer.app;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pedido {
    private Connection connection;
    private final String idPedido;
    private final Usuario usuario;
    private final LocalDateTime fechaCreacion;
    private final int metodoPagoId;
    private final double descuentoTotal;
    private final double impuestos;
    private EstadoPedido estado;
    private List<ItemPedido> items;

    public void setItems(List<ItemPedido> items) {
        this.items = items;
    }
    // Constructor principal
    public Pedido(String idPedido, Usuario usuario, LocalDateTime fechaCreacion, int metodoPagoId,
                  double descuentoTotal, double impuestos,
                  EstadoPedido estado, Connection connection) {
        this.idPedido = idPedido;
        this.usuario = usuario;
        this.fechaCreacion = fechaCreacion;
        this.metodoPagoId = metodoPagoId;
        this.descuentoTotal = descuentoTotal;
        this.impuestos = impuestos;
        this.estado = estado;
        this.connection = connection;
    }

    // Constructor alternativo para nuevos pedidos
    public Pedido(Usuario usuario, int metodoPagoId,
                  double descuentoTotal, double impuestos, Connection connection) {
        this(
                "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                usuario,
                LocalDateTime.now(),
                metodoPagoId,
                descuentoTotal,
                impuestos,
                EstadoPedido.PENDIENTE,
                connection
        );
    }

    private static final Logger LOGGER = Logger.getLogger(Pedido.class.getName());


    private static String generarIdPedido() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void guardarEnBD() throws SQLException {
        // Verificar si la conexión tiene el auto-commit activado antes de manipularlo
        boolean autoCommitOriginal = connection.getAutoCommit();

        try {
            // Desactivar auto-commit para usar transacciones manuales
            connection.setAutoCommit(false);

            // Asegurarse de que el auto-commit se mantenga desactivado durante la transacción
            System.out.println("Estado de autoCommit antes de commit: " + connection.getAutoCommit());

            // Guardar el pedido y los items en la base de datos
            guardarPedidoEnBD();
            guardarItemsPedido();

            // Si todo ha ido bien, hacer commit a la transacción
            connection.commit();
            LOGGER.log(Level.INFO, "Pedido guardado correctamente en la base de datos.");
        } catch (SQLException e) {
            // Si ocurre un error, hacer rollback de la transacción
            connection.rollback();
            LOGGER.log(Level.SEVERE, "Error al guardar el pedido, se ha hecho rollback.", e);
            throw e; // Propagar la excepción para que sea manejada en el controlador
        } finally {
            // Restaurar el auto-commit al valor original después de la transacción
            connection.setAutoCommit(autoCommitOriginal);
            System.out.println("Estado de autoCommit después de finalizar la transacción: " + connection.getAutoCommit());
        }
    }

    private void guardarPedidoEnBD() throws SQLException {
        String sql = "INSERT INTO pedido (id_pedido, id_usuario, fecha_creacion, " +
                "metodo_pago, descuento_total, impuestos, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, idPedido);
            stmt.setString(2, usuario.getIdUsuario());
            stmt.setTimestamp(3, Timestamp.valueOf(fechaCreacion));
            stmt.setInt(4, metodoPagoId);
            stmt.setDouble(5, descuentoTotal);
            stmt.setDouble(6, impuestos);
            stmt.setString(7, estado.name());
            stmt.executeUpdate();
        }
    }

    private void guardarItemsPedido() throws SQLException {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO item_pedido (id_pedido, id_videojuego, precio_unitario, cantidad, subtotal) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (ItemPedido item : items) {
                stmt.setString(1, idPedido);
                stmt.setLong(2, item.getJuego().getIdVideojuego());
                stmt.setDouble(3, item.getPrecioUnitario());
                stmt.setInt(4, item.getCantidad());
                stmt.setDouble(5, item.getSubtotal());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public boolean procesarPago() throws SQLException {
        MetodoPago metodo = MetodoPago.cargarDesdeBD(metodoPagoId, connection);
        double total = calcularTotal();

        if (metodo.procesarPago(total)) {
            actualizarEstado(EstadoPedido.PAGADO);
            registrarEnHistorial(total);
            agregarABiblioteca();
            return true;
        }
        return false;
    }

    private void registrarEnHistorial(double total) throws SQLException {
        String sql = "INSERT INTO historial_compras (id_usuario, id_pedido, fecha_compra, total) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setString(2, idPedido);
            stmt.setDate(3, Date.valueOf(fechaCreacion.toLocalDate()));
            stmt.setDouble(4, total);
            stmt.executeUpdate();
        }
    }

    private void agregarABiblioteca() throws SQLException {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO biblioteca (id_usuario, id_videojuego, fecha_compra) " +
                "VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (ItemPedido item : items) {
                stmt.setString(1, usuario.getIdUsuario());
                stmt.setLong(2, item.getJuego().getIdVideojuego());
                stmt.setDate(3, Date.valueOf(fechaCreacion.toLocalDate()));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<ItemPedido> getItems() throws SQLException {
        if (items == null) {
            items = ItemPedido.obtenerPorPedido(idPedido, connection);
        }
        return items;
    }

    public double calcularTotal() throws SQLException {
        double subtotal = getItems().stream()
                .mapToDouble(ItemPedido::getSubtotal)
                .sum();
        return (subtotal - descuentoTotal) * (1 + impuestos);
    }

    private void actualizarEstado(EstadoPedido nuevoEstado) throws SQLException {
        String sql = "UPDATE pedido SET estado = ? WHERE id_pedido = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado.name());
            stmt.setString(2, idPedido);
            stmt.executeUpdate();
            this.estado = nuevoEstado;
        }
    }

    // Getters
    public String getIdPedido() { return idPedido; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public int getMetodoPagoId() { return metodoPagoId; }
    public double getDescuentoTotal() { return descuentoTotal; }
    public double getImpuestos() { return impuestos; }
    public EstadoPedido getEstado() { return estado; }
    public double getTotal() {
        try {
            return calcularTotal();
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular total del pedido", e);
        }
    }
}