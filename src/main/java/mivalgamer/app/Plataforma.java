import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Plataforma {
    private final long idPlataforma;
    private final String nombreComercial;
    private final String fabricante;

    public Plataforma(long idPlataforma, String nombreComercial, String fabricante) {
        this.idPlataforma = idPlataforma;
        this.nombreComercial = nombreComercial;
        this.fabricante = fabricante;
    }

    public static List<Plataforma> obtenerTodas(Connection conn) throws SQLException {
        List<Plataforma> plataformas = new ArrayList<>();
        String sql = "SELECT * FROM plataforma";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                plataformas.add(new Plataforma(
                        rs.getLong("id_plataforma"),
                        rs.getString("nombre_comercial"),
                        rs.getString("fabricante")
                ));
            }
        }
        return plataformas;
    }

    public static Plataforma obtenerPorId(Connection conn, long id) throws SQLException {
        String sql = "SELECT * FROM plataforma WHERE id_plataforma = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Plataforma(
                            rs.getLong("id_plataforma"),
                            rs.getString("nombre_comercial"),
                            rs.getString("fabricante")
                    );
                }
            }
        }
        throw new SQLException("Plataforma no encontrada");
    }

    // Getters
    public long getIdPlataforma() { return idPlataforma; }
    public String getNombreComercial() { return nombreComercial; }
    public String getFabricante() { return fabricante; }
}