package mivalgamer.app;

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

        public Videojuego getJuego() {
            return juego;
        }

        public LocalDateTime getFechaCompra() {
            return fechaCompra;
        }

        public String getKeyActivacion() {
            return keyActivacion;
        }
    }

    public Biblioteca(Usuario usuario, Connection connection) {
        if (usuario == null || connection == null) {
            throw new IllegalArgumentException("Usuario y conexión no pueden ser nulos");
        }
        this.usuario = usuario;
        this.connection = connection;
    }

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
            List<ItemBiblioteca> items = getJuegos();

            System.out.println("\n=== TU BIBLIOTECA DE JUEGOS ===");
            System.out.printf("Total de juegos: %d%n%n", items.size());

            if (items.isEmpty()) {
                System.out.println("No tienes juegos en tu biblioteca");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    ItemBiblioteca item = items.get(i);
                    // Aquí mostramos los nombres de TODAS las plataformas del videojuego
                    String plataformas = obtenerNombresPlataformas(item.getJuego());
                    System.out.printf("%d. %s - %s | Key: %s | Comprado: %s%n",
                            i + 1,
                            item.getJuego().getTitulo(),
                            plataformas,
                            item.getKeyActivacion(),
                            item.getFechaCompra().toLocalDate());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al mostrar biblioteca", e);
            System.out.println("\nError al cargar la biblioteca: " + e.getMessage());
        }
    }

    public void agregarJuego(Videojuego juego, String keyActivacion) {
        if (juego == null || keyActivacion == null || keyActivacion.isEmpty()) {
            throw new IllegalArgumentException("Juego y key de activación no pueden ser nulos");
        }

        if (contieneJuego(juego)) {
            LOGGER.warning("El juego " + juego.getTitulo() + " ya está en la biblioteca");
            return;
        }

        String sql = "INSERT INTO biblioteca (id_usuario, id_videojuego, fecha_compra, key_activacion) " +
                "VALUES (?, ?, NOW(), ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setLong(2, juego.getIdVideojuego());
            stmt.setString(3, keyActivacion);
            stmt.executeUpdate();

            LOGGER.info("Juego " + juego.getTitulo() + " agregado a la biblioteca");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al agregar juego a la biblioteca", ex);
            throw new RuntimeException("Error al agregar juego a la biblioteca", ex);
        }
    }

    public List<ItemBiblioteca> getJuegosRecientes(int limite) {
        List<ItemBiblioteca> items = new ArrayList<>();
        String sql = "SELECT v.*, b.fecha_compra, b.key_activacion FROM biblioteca b " +
                "JOIN videojuego v ON b.id_videojuego = v.id_videojuego " +
                "WHERE b.id_usuario = ? ORDER BY b.fecha_compra DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, usuario.getIdUsuario());
            stmt.setInt(2, limite);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Videojuego juego = mapVideojuegoFromResultSet(rs);
                LocalDateTime fechaCompra = rs.getTimestamp("fecha_compra").toLocalDateTime();
                String keyActivacion = rs.getString("key_activacion");
                items.add(new ItemBiblioteca(juego, fechaCompra, keyActivacion));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener juegos recientes", ex);
        }
        return items;
    }

<<<<<<< HEAD
    // Devuelve los nombres de todas las plataformas asociadas a un videojuego separados por coma
    private String obtenerNombresPlataformas(Videojuego juego) {
        try {
            List<Plataforma> plataformas = juego.obtenerPlataformas(connection);
            if (plataformas.isEmpty()) {
                return "Plataformas desconocidas";
            }
            List<String> nombres = new ArrayList<>();
            for (Plataforma plataforma : plataformas) {
                nombres.add(plataforma.getNombreComercial());
            }
            return String.join(", ", nombres);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener plataformas del videojuego", ex);
=======
    private String obtenerNombrePlataforma(long idPlataforma) {
        String sql = "SELECT nombre_comercial FROM plataforma WHERE id_plataforma = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, idPlataforma);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("nombre_comercial") : "Desconocida";
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al obtener nombre de plataforma", ex);
>>>>>>> 832a7629a1439125ab9c5211116ca639b8163bb8
            return "Desconocida";
        }
    }

    private Videojuego mapVideojuegoFromResultSet(ResultSet rs) throws SQLException {
<<<<<<< HEAD
        // Se adapta a la nueva firma del constructor de Videojuego (sin idPlataforma y con los nuevos campos)
=======
>>>>>>> 832a7629a1439125ab9c5211116ca639b8163bb8
        return new Videojuego(
                rs.getLong("id_videojuego"),
                rs.getString("titulo"),
                rs.getString("estudio"),
                rs.getLong("id_genero"),
<<<<<<< HEAD
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getDouble("precio_original"),
                rs.getBoolean("descuento_aplicado"),
                EstadoVideojuego.fromString(rs.getString("estado")),
                rs.getString("icono"),
                rs.getString("portada"),
                rs.getString("contenido_visual"),
                rs.getInt("stock")
=======
                rs.getLong("id_plataforma"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                EstadoVideojuego.fromString(rs.getString("estado"))
>>>>>>> 832a7629a1439125ab9c5211116ca639b8163bb8
        );
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