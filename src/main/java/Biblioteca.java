import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Biblioteca {
    private static final Logger LOGGER = Logger.getLogger(Biblioteca.class.getName());
    private final Usuario usuario;
    private final Connection connection;

    public static class ItemBiblioteca {
        private final Videojuego juego;
        private final LocalDateTime fechaCompra;
        private final String keyActivacion;

        public ItemBiblioteca(Videojuego juego, LocalDateTime fechaCompra, String keyActivacion) {
            this.juego = juego;
            this.fechaCompra = fechaCompra;
            this.keyActivacion = keyActivacion;
        }

        public Videojuego getJuego() { return juego; }
        public LocalDateTime getFechaCompra() { return fechaCompra; }
        public String getKeyActivacion() { return keyActivacion; }
    }

    public Biblioteca(Usuario usuario, Connection connection) {
        if (usuario == null || connection == null) {
            throw new IllegalArgumentException("Usuario y conexión no pueden ser nulos");
        }
        this.usuario = usuario;
        this.connection = connection;
    }

    // Método renombrado de getItemsBiblioteca() a getJuegos() para resolver el primer error
    public List<ItemBiblioteca> getJuegos() {
        List<ItemBiblioteca> items = new ArrayList<>();
        String sql = "SELECT v.*, b.fecha_compra, b.key_activacion FROM biblioteca b " +
                "JOIN videojuego v ON b.id_videojuego = v.id_videojuego " +
                "WHERE b.id_usuario = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Videojuego juego = mapVideojuegoFromResultSet(rs);
                LocalDateTime fechaCompra = rs.getTimestamp("fecha_compra").toLocalDateTime();
                String keyActivacion = rs.getString("key_activacion");
                items.add(new ItemBiblioteca(juego, fechaCompra, keyActivacion));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener juegos de la biblioteca", ex);
            throw new RuntimeException("Error al obtener juegos de la biblioteca", ex);
        }
        return items;
    }

    public void mostrarBiblioteca() {
        try {
            List<ItemBiblioteca> items = getJuegos(); // Usamos el método renombrado

            System.out.println("\n=== TU BIBLIOTECA DE JUEGOS ===");
            System.out.printf("Total de juegos: %d%n%n", items.size());

            if (items.isEmpty()) {
                System.out.println("No tienes juegos en tu biblioteca");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    ItemBiblioteca item = items.get(i);
                    System.out.printf("%d. %s - %s | Key: %s | Comprado: %s%n",
                            i + 1,
                            item.getJuego().getTitulo(),
                            obtenerNombrePlataforma(item.getJuego().getIdPlataforma()),
                            item.getKeyActivacion(),
                            item.getFechaCompra().toLocalDate());
                }
            }
        } catch (Exception e) { // Cambiado a Exception para resolver el segundo error
            LOGGER.log(Level.SEVERE, "Error al mostrar biblioteca", e);
            System.out.println("\nError al cargar la biblioteca: " + e.getMessage());
        }
    }

    public static void mostrarMenuBiblioteca() {
        if (Proyecto.usuarioActual == null) {
            System.out.println("No hay usuario autenticado");
            return;
        }

        Biblioteca biblioteca = Proyecto.usuarioActual.getBiblioteca();
        biblioteca.mostrarBiblioteca();
    }

    private String obtenerNombrePlataforma(long idPlataforma) {
        String sql = "SELECT nombre_comercial FROM plataforma WHERE id_plataforma = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idPlataforma);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("nombre_comercial") : "Desconocida";
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener nombre de plataforma", ex);
            return "Desconocida";
        }
    }

    private Videojuego mapVideojuegoFromResultSet(ResultSet rs) throws SQLException {
        return new Videojuego(
                rs.getLong("id_videojuego"),
                rs.getString("titulo"),
                rs.getString("estudio"),
                rs.getLong("id_genero"),
                rs.getLong("id_plataforma"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                EstadoVideojuego.fromString(rs.getString("estado"))
        );
    }

    public void agregarJuego(Videojuego juego, String keyActivacion) {
        if (juego == null || keyActivacion == null || keyActivacion.isEmpty()) {
            throw new IllegalArgumentException("Juego y key de activación no pueden ser nulos");
        }

        String sql = "INSERT INTO biblioteca (id_usuario, id_videojuego, fecha_compra, key_activacion) " +
                "VALUES (?, ?, CURDATE(), ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setLong(2, juego.getIdVideojuego());
            stmt.setString(3, keyActivacion);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al agregar juego a la biblioteca", ex);
            throw new RuntimeException("Error al agregar juego a la biblioteca", ex);
        }
    }

    public boolean contieneJuego(Videojuego juego) {
        if (juego == null) return false;

        String sql = "SELECT COUNT(*) FROM biblioteca WHERE id_usuario = ? AND id_videojuego = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setLong(2, juego.getIdVideojuego());
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al verificar juego en biblioteca", ex);
            return false;
        }
    }
}
