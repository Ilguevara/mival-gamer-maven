package mivalgamer.app;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Videojuego {
    private final long idVideojuego;
    private final String titulo;
    private final String estudio;
    private final long idGenero;
    private final String descripcion;
    private final double precio;
    private final double precioOriginal;
    private final boolean descuentoAplicado;
    private final EstadoVideojuego estado;
    private final String icono;
    private final String portada;
    private final String contenidoVisual;
    private final int stock;

    public Videojuego(long idVideojuego, String titulo, String estudio, long idGenero,
                      String descripcion, double precio, double precioOriginal,
                      boolean descuentoAplicado, EstadoVideojuego estado,
                      String icono, String portada, String contenidoVisual, int stock) {
        this.idVideojuego = idVideojuego;
        this.titulo = titulo;
        this.estudio = estudio;
        this.idGenero = idGenero;
        this.descripcion = descripcion;
        this.precio = precio;
        this.precioOriginal = precioOriginal;
        this.descuentoAplicado = descuentoAplicado;
        this.estado = estado;
        this.icono = icono;
        this.portada = portada;
        this.contenidoVisual = contenidoVisual;
        this.stock = stock;
    }

    public static Videojuego fromResultSet(Connection conn, ResultSet rs) throws SQLException {
        return new Videojuego(
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
    }

    public static List<Videojuego> obtenerTodos(Connection conn) throws SQLException {
        List<Videojuego> videojuegos = new ArrayList<>();
        String sql = "SELECT * FROM videojuego";

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

    /**
     * Obtiene todos los videojuegos que están en una plataforma específica.
     */
    public static List<Videojuego> obtenerPorPlataforma(Connection conn, long idPlataforma) throws SQLException {
        List<Videojuego> videojuegos = new ArrayList<>();
        String sql = """
            SELECT v.*
            FROM videojuego v
            JOIN videojuego_plataforma vp ON v.id_videojuego = vp.id_videojuego
            WHERE vp.id_plataforma = ?
        """;

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
    public String getDescripcion() { return descripcion; }
    public double getPrecio() { return precio; }
    public double getPrecioOriginal() { return precioOriginal; }
    public boolean isDescuentoAplicado() { return descuentoAplicado; }
    public EstadoVideojuego getEstado() { return estado; }
    public String getIcono() { return icono; }
    public String getPortada() { return portada; }
    public String getContenidoVisual() { return contenidoVisual; }
    public int getStock() { return stock; }

    /**
     * Obtiene todas las plataformas asociadas a este videojuego (muchos a muchos).
     */
    public List<Plataforma> obtenerPlataformas(Connection conn) throws SQLException {
        return Plataforma.obtenerPorVideojuego(conn, this.idVideojuego);
    }
}