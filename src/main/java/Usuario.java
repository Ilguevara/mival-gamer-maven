import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Usuario {
    private final Connection connection;
    private final String idUsuario;
    private final String nombre;
    private final String email;

    // NUEVO: Atributo para mantener el carrito en memoria
    private CarritoCompra carrito;

    public Usuario(Connection connection, String idUsuario, String nombre, String email) {
        this.connection = connection;
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
    }

    // Métodos relacionados con métodos de pago
    public List<MetodoPago> getMetodosPago() throws SQLException {
        List<MetodoPago> metodos = new ArrayList<>();
        String sql = "SELECT * FROM metodo_pago WHERE id_metodo IN " +
                "(SELECT id_metodo FROM usuario_metodo_pago WHERE id_usuario = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                metodos.add(MetodoPago.cargarDesdeBD(rs.getInt("id_metodo"), connection));
            }
        }
        return metodos;
    }

    public void agregarMetodoPago(MetodoPago metodo) throws SQLException {
        String sql = "INSERT INTO usuario_metodo_pago (id_usuario, id_metodo) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, idUsuario);
            stmt.setInt(2, metodo.getIdMetodo());
            stmt.executeUpdate();
        }
    }

    // Creación de pedidos
    public Pedido crearPedido(List<ItemCarrito> items,
                              int idMetodoPago, String codigoDescuento) throws SQLException {
        return PedidoFactory.crearPedidoDesdeCarrito(connection, items,
                idMetodoPago, codigoDescuento, this);
    }

    // Biblioteca y compras
    public Biblioteca getBiblioteca() {
        return new Biblioteca(this, connection);
    }

    public HistorialCompras getHistorialCompras() {
        return new HistorialCompras(this, connection);
    }

    // NUEVO: Carrito persistente en el objeto Usuario
    public CarritoCompra getCarrito() {
        return carrito;
    }

    public void setCarrito(CarritoCompra carrito) {
        this.carrito = carrito;
    }

    // Cambio de contraseña
    public void cambiarContrasena(Autentificacion auth, String nuevaPassword) throws SQLException {
        auth.cambiarContrasena(this.idUsuario, nuevaPassword);
    }

    // Getters
    public String getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
}
