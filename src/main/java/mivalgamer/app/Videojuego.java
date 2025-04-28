package mivalgamer.app;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Videojuego {
    private final long idVideojuego;
    private final String titulo;
    private final String estudio;
    private final long idGenero;
    private final long idPlataforma;
    private final String descripcion;
    private final double precio;
    private final EstadoVideojuego estado;

    public Videojuego(long idVideojuego, String titulo, String estudio, long idGenero,
                      long idPlataforma, String descripcion, double precio, EstadoVideojuego estado) {
        this.idVideojuego = idVideojuego;
        this.titulo = titulo;
        this.estudio = estudio;
        this.idGenero = idGenero;
        this.idPlataforma = idPlataforma;
        this.descripcion = descripcion;
        this.precio = precio;
        this.estado = estado;
    }

    public static Videojuego fromResultSet(Connection conn, ResultSet rs) throws SQLException {
        return new Videojuego(
                rs.getLong("id_videojuego"),
                rs.getString("titulo"),
                rs.getString("estudio"),
                rs.getLong("id_genero"),
                rs.getLong("id_plataforma"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                EstadoVideojuego.fromString(rs.getString("estado")) // Conversión correcta
        );
    }


    public static List<Videojuego> obtenerTodos(Connection conn) throws SQLException {
        List<Videojuego> videojuegos = new ArrayList<>();
        String sql = "SELECT * FROM videojuego WHERE estado = 'DISPONIBLE'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                videojuegos.add(fromResultSet(conn, rs));
            }
        }
        return videojuegos;
    }

    public static Videojuego obtenerPorId(Connection conn, long id) throws SQLException {
        String sql = "SELECT * FROM videojuego WHERE id_videojuego = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(conn, rs);
                }
            }
        }
        throw new SQLException("Videojuego no encontrado");
    }

    public static List<Videojuego> obtenerPorPlataforma(Connection conn, long idPlataforma) throws SQLException {
        List<Videojuego> videojuegos = new ArrayList<>();
        String sql = "SELECT * FROM videojuego WHERE id_plataforma = ? AND estado = 'DISPONIBLE'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idPlataforma);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    videojuegos.add(fromResultSet(conn, rs));
                }
            }
        }
        return videojuegos;
    }

    public static List<Videojuego> obtenerEnDescuento(Connection conn) throws SQLException {
        List<Videojuego> videojuegos = new ArrayList<>();
        String sql = "SELECT v.* FROM videojuego v " +
                "JOIN videojuego_descuento vd ON v.id_videojuego = vd.videojuego_id " +
                "JOIN descuento d ON vd.descuento_id = d.id_descuento " +
                "WHERE d.fecha_inicio <= CURRENT_DATE AND d.fecha_expiracion >= CURRENT_DATE";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                videojuegos.add(fromResultSet(conn, rs));
            }
        }
        return videojuegos;
    }

    // Getters
    public long getIdVideojuego() { return idVideojuego; }
    public String getTitulo() { return titulo; }
    public String getEstudio() { return estudio; }
    public long getIdGenero() { return idGenero; }
    public long getIdPlataforma() { return idPlataforma; }
    public String getDescripcion() { return descripcion; }
    public double getPrecio() { return precio; }
    public EstadoVideojuego getEstado() { return estado; }
}