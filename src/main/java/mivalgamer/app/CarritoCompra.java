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
        this.idCarrito = idCarrito;
        this.usuario = usuario;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.connection = connection;
    }

    public CarritoCompra(Usuario usuario, Connection connection) {
        validarParametros(usuario, connection);
        this.usuario = usuario;
        this.connection = connection;
        this.estado = EstadoCarrito.ACTIVO;
        this.crearNuevoCarritoEnBD();
    }

    private void validarParametros(Usuario usuario, Connection connection) {
        if (usuario == null || connection == null) {
            throw new IllegalArgumentException("Usuario y conexión no pueden ser nulos");
        }
    }

    private void crearNuevoCarritoEnBD() {
        String sql = "INSERT INTO carrito_compra (id_usuario, fecha_creacion, estado) VALUES (?, NOW(), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setString(2, estado.name());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                this.idCarrito = rs.getLong(1);
                this.fechaCreacion = LocalDateTime.now();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al crear un nuevo carrito en la base de datos", ex);
            throw new RuntimeException("Error al crear carrito", ex);
        }
    }

    public List<ItemCarrito> getItems() {
        List<ItemCarrito> items = new ArrayList<>();
        String sql = "SELECT ic.*, v.* FROM item_carrito ic " +
                "JOIN videojuego v ON ic.id_videojuego = v.id_videojuego " +
                "WHERE ic.id_carrito = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapearItem(rs));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener items del carrito", ex);
            throw new RuntimeException("Error al obtener items", ex);
        }
        return items;
    }


    private ItemCarrito mapearItem(ResultSet rs) throws SQLException {
        Videojuego videojuego = new Videojuego(
                rs.getLong("id_videojuego"),
                rs.getString("titulo"),
                rs.getString("estudio"),
                rs.getLong("id_genero"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getDouble("precio_original"),
                rs.getBoolean("descuento_aplicado"),
                EstadoVideojuego.fromString(rs.getString("estado")),
                rs.getString("icono"),
                rs.getString("portada"),
                rs.getString("contenido_visual"),
                rs.getInt("stock")
        );

        int cantidad = rs.getInt("cantidad");
        double subtotal = rs.getDouble("subtotal");
        double precioUnitario = cantidad > 0 ? subtotal / cantidad : 0.0;
        return new ItemCarrito(videojuego, cantidad, precioUnitario);
    }

    public void agregarItem(Videojuego juego, int cantidad) throws SQLException {
        if (juego == null || cantidad <= 0) {
            throw new IllegalArgumentException("Juego y cantidad válidos requeridos");
        }
        if (!puedeAgregarItem(juego, cantidad)) {
            throw new IllegalStateException("No se puede agregar el juego al carrito (sin stock o ya está)");
        }

        double precio = obtenerPrecioActual(juego.getIdVideojuego());
        if (itemExisteEnCarrito(juego.getIdVideojuego())) {
            actualizarItemExistente(juego.getIdVideojuego(), cantidad, precio);
        } else {
            insertarNuevoItem(juego.getIdVideojuego(), cantidad, precio);
        }
    }

    private boolean itemExisteEnCarrito(Long idVideojuego) throws SQLException {
        String sql = "SELECT COUNT(*) FROM item_carrito WHERE id_carrito = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            stmt.setLong(2, idVideojuego);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }


    private double obtenerPrecioActual(Long idVideojuego) throws SQLException {
        String sql = "SELECT precio FROM videojuego WHERE id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idVideojuego);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("precio");
            } else {
                throw new SQLException("Videojuego no encontrado");
            }
        }
    }

    private void actualizarItemExistente(Long idVideojuego, int cantidadAdicional, double precio) throws SQLException {
        String sql = "UPDATE item_carrito SET cantidad = cantidad + ?, subtotal = (cantidad + ?) * ? WHERE id_carrito = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, cantidadAdicional);
            stmt.setInt(2, cantidadAdicional); // Para el subtotal
            stmt.setDouble(3, precio); // Precio unitario
            stmt.setLong(4, idCarrito);
            stmt.setLong(5, idVideojuego);
            stmt.executeUpdate();
        }
    }


    private void insertarNuevoItem(Long idVideojuego, int cantidad, double precio) throws SQLException {
        String sql = "INSERT INTO item_carrito (id_carrito, id_videojuego, cantidad, subtotal) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            stmt.setLong(2, idVideojuego);
            stmt.setInt(3, cantidad);
            stmt.setDouble(4, precio * cantidad);
            stmt.executeUpdate();
        }
    }


    public boolean puedeAgregarItem(Videojuego juego, int cantidad) {
        // Se mantiene la lógica de validación, usando el metodo de Videojuego actualizado si es necesario
        return juego != null && cantidad > 0 && validarDisponibilidadJuego(juego);
    }

    private boolean validarDisponibilidadJuego(Videojuego juego) {
        // Ejemplo básico: verificar stock mayor a 0
        return juego.getStock() > 0;
    }

    public boolean existeCarritoEnBD() throws SQLException {
        String sql = "SELECT COUNT(*) FROM carrito_compra WHERE id_carrito = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public void eliminarItem(Long idVideojuego) {
        String sql = "DELETE FROM item_carrito WHERE id_carrito = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idCarrito);
            stmt.setLong(2, idVideojuego);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al eliminar item del carrito", ex);
        }
    }


    public double calcularTotal() {
        double total = 0.0;
        for (ItemCarrito item : getItems()) {
            total += item.getCantidad() * item.getPrecioUnitario();
        }
        return total;
    }

    public void cambiarEstado(EstadoCarrito nuevoEstado) {
        String sql = "UPDATE carrito_compra SET estado = ? WHERE id_carrito = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado.name());
            stmt.setLong(2, idCarrito);
            stmt.executeUpdate();
            this.estado = nuevoEstado;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al cambiar el estado del carrito", ex);
            throw new RuntimeException("Error al cambiar el estado del carrito", ex);
        }
    }

    // Getters
    public Long getIdCarrito() { return idCarrito; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public EstadoCarrito getEstado() { return estado; }
    public boolean estaVacio() { return getItems().isEmpty(); }
}