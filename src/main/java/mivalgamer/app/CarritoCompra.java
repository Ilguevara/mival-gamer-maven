package mivalgamer.app;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CarritoCompra {
    private static final Logger LOGGER = Logger.getLogger(CarritoCompra.class.getName());

    private final Connection connection;
    private Long idCarrito;
    private final Usuario usuario;
    private LocalDateTime fechaCreacion;
    private EstadoCarrito estado;

    public CarritoCompra(Long idCarrito, Usuario usuario, LocalDateTime fechaCreacion,
                         EstadoCarrito estado, Connection connection) {
        validarParametros(usuario, connection);
        this.connection = connection;
        this.idCarrito = idCarrito;
        this.usuario = usuario;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        LOGGER.log(Level.INFO, "Carrito creado con ID: {0}", idCarrito);
    }

    public CarritoCompra(Usuario usuario, Connection connection) {
        validarParametros(usuario, connection);
        this.connection = connection;
        this.usuario = usuario;
        this.estado = EstadoCarrito.ACTIVO;
        crearNuevoCarritoEnBD();
    }

    private void validarParametros(Usuario usuario, Connection connection) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no puede ser nulo");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Conexión no puede ser nula");
        }
        try {
            if (connection.isClosed()) {
                throw new IllegalArgumentException("Conexión está cerrada");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar conexión", e);
        }
    }

    private void crearNuevoCarritoEnBD() {
        String sql = "INSERT INTO carrito_compra (id_usuario, fecha_creacion, estado) VALUES (?, NOW(), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setString(2, estado.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No se pudo crear el carrito, ninguna fila afectada");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    this.idCarrito = rs.getLong(1);
                    this.fechaCreacion = LocalDateTime.now();
                    LOGGER.log(Level.INFO, "Nuevo carrito creado con ID: {0}", idCarrito);
                } else {
                    throw new SQLException("No se pudo obtener el ID del carrito creado");
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al crear carrito en BD", ex);
            throw new RuntimeException("Error al crear carrito", ex);
        }
    }

    public List<ItemCarrito> getItems() {
        if (idCarrito == null) {
            LOGGER.warning("Intento de obtener items de carrito no creado");
            return new ArrayList<>();
        }

        List<ItemCarrito> items = new ArrayList<>();
        String sql = "SELECT i.id_item, i.id_carrito, i.id_videojuego, i.cantidad, i.subtotal, " +
                "v.titulo, v.estudio, v.id_genero, v.id_plataforma, v.descripcion, " +
                "v.precio, v.estado " +
                "FROM item_carrito i " +
                "JOIN videojuego v ON i.id_videojuego = v.id_videojuego " +
                "WHERE i.id_carrito = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapearItem(rs));
                }
            }
            LOGGER.log(Level.INFO, "Obtenidos {0} items del carrito {1}", new Object[]{items.size(), idCarrito});
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener ítems del carrito " + idCarrito, ex);
            throw new RuntimeException("Error al cargar ítems del carrito", ex);
        }
        return items;
    }

    private ItemCarrito mapearItem(ResultSet rs) throws SQLException {
        Videojuego juego = new Videojuego(
                rs.getLong("id_videojuego"),
                rs.getString("titulo"),
                rs.getString("estudio"),
                rs.getLong("id_genero"),
                rs.getLong("id_plataforma"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                EstadoVideojuego.fromString(rs.getString("estado"))
        );

        return new ItemCarrito(
                rs.getLong("id_item"),
                juego,
                rs.getInt("cantidad"),
                this
        );
    }

    public void agregarItem(Videojuego juego, int cantidad) throws SQLException {
        if (juego == null) {
            throw new IllegalArgumentException("El videojuego no puede ser nulo");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        if (!existeCarritoEnBD()) {
            crearNuevoCarritoEnBD();
        }

        if (puedeAgregarItem(juego, cantidad)) {
            double precio = obtenerPrecioActual(juego.getIdVideojuego());

            if (itemExisteEnCarrito(juego.getIdVideojuego())) {
                actualizarItemExistente(juego.getIdVideojuego(), cantidad, precio);
            } else {
                insertarNuevoItem(juego.getIdVideojuego(), cantidad, precio);
            }

            LOGGER.log(Level.INFO, "Item agregado correctamente al carrito {0}", idCarrito);
        } else {
            throw new IllegalStateException("No se pudo agregar el ítem al carrito");
        }
    }



    private boolean itemExisteEnCarrito(Long idVideojuego) throws SQLException {
        String sql = "SELECT 1 FROM item_carrito WHERE id_carrito = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            stmt.setLong(2, idVideojuego);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private double obtenerPrecioActual(Long idVideojuego) throws SQLException {
        String sql = "SELECT precio FROM videojuego WHERE id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idVideojuego);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("precio");
                }
                throw new SQLException("Videojuego no encontrado");
            }
        }
    }

    private void actualizarItemExistente(Long idVideojuego, int cantidadAdicional, double precio) throws SQLException {
        String sql = "UPDATE item_carrito SET cantidad = cantidad + ?, subtotal = (cantidad + ?) * ? " +
                "WHERE id_carrito = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, cantidadAdicional);
            stmt.setInt(2, cantidadAdicional);
            stmt.setDouble(3, precio);
            stmt.setLong(4, idCarrito);
            stmt.setLong(5, idVideojuego);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No se pudo actualizar el ítem");
            }
        }
    }

    private void insertarNuevoItem(Long idVideojuego, int cantidad, double precio) throws SQLException {
        String sql = "INSERT INTO item_carrito (id_carrito, id_videojuego, cantidad, subtotal) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            stmt.setLong(2, idVideojuego);
            stmt.setInt(3, cantidad);
            stmt.setDouble(4, cantidad * precio);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No se pudo insertar el ítem");
            }
        }
    }

    public boolean puedeAgregarItem(Videojuego juego, int cantidad) {
        if (estado != EstadoCarrito.ACTIVO) {
            LOGGER.log(Level.WARNING, "Intento de agregar item a carrito no ACTIVO. Estado actual: {0}", estado);
            return false;
        }
        if (!validarDisponibilidadJuego(juego)) {
            LOGGER.log(Level.WARNING, "Juego no disponible: {0}", juego.getIdVideojuego());
            return false;
        }
        return true;
    }

    private boolean validarDisponibilidadJuego(Videojuego juego) {
        String sql = "SELECT estado FROM videojuego WHERE id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, juego.getIdVideojuego());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && EstadoVideojuego.fromString(rs.getString("estado")).permiteTransaccion();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al validar disponibilidad del juego", ex);
            return false;
        }
    }

    public boolean existeCarritoEnBD() throws SQLException {
        if (idCarrito == null) {
            return false;
        }
        String sql = "SELECT 1 FROM carrito_compra WHERE id_carrito = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void eliminarItem(Long idVideojuego) {
        String sql = "DELETE FROM item_carrito WHERE id_carrito = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            stmt.setLong(2, idVideojuego);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                LOGGER.log(Level.WARNING, "No se encontró el ítem para eliminar");
            } else {
                LOGGER.log(Level.INFO, "Ítem eliminado del carrito {0}", idCarrito);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al eliminar ítem", ex);
            throw new RuntimeException("Error al eliminar ítem del carrito", ex);
        }
    }

    public double calcularTotal() {
        String sql = "SELECT SUM(subtotal) AS total FROM item_carrito WHERE id_carrito = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al calcular total del carrito", ex);
            throw new RuntimeException("Error al calcular total del carrito", ex);
        }
    }

    public void cambiarEstado(EstadoCarrito nuevoEstado) {
        if (!estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalStateException(String.format(
                    "Transición inválida: de %s a %s", estado, nuevoEstado));
        }

        String sql = "UPDATE carrito_compra SET estado = ? WHERE id_carrito = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado.name());
            stmt.setLong(2, idCarrito);
            stmt.executeUpdate();
            this.estado = nuevoEstado;
            LOGGER.log(Level.INFO, "Estado del carrito {0} cambiado a {1}", new Object[]{idCarrito, nuevoEstado});
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al cambiar estado del carrito", ex);
            throw new RuntimeException("Error al actualizar estado del carrito", ex);
        }
    }

    // Getters
    public Long getIdCarrito() { return idCarrito; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public EstadoCarrito getEstado() { return estado; }
    public boolean estaVacio() { return getItems().isEmpty(); }
}
