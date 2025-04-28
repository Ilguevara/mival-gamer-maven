import java.sql.ResultSet;
import java.sql.SQLException;

public class Genero {
    private final Long idGenero;
    private final String nombre;
    private final String descripcion;
    private final String iconoUrl;

    public Genero(Long idGenero, String nombre, String descripcion, String iconoUrl) {
        this.idGenero = idGenero;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.iconoUrl = iconoUrl;
    }

    /**
     * Constructor desde ResultSet para mapear resultados de BD
     */
    public static Genero fromResultSet(ResultSet rs) throws SQLException {
        return new Genero(
                rs.getLong("id_genero"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("icono_url")
        );
    }

    // Getters
    public Long getIdGenero() { return idGenero; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getIconoUrl() { return iconoUrl; }

    @Override
    public String toString() {
        return String.format(
                "Genero[id=%d, nombre='%s', descripcion='%s', icono='%s']",
                idGenero, nombre, descripcion, iconoUrl
        );
    }
}