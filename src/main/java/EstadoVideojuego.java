import java.util.List;

public enum EstadoVideojuego {
    DISPONIBLE,
    AGOTADO,
    PREVENTA;

    public static EstadoVideojuego fromString(String estado) {
        if (estado == null) return null;

        try {
            return EstadoVideojuego.valueOf(estado.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de videojuego no válido: " + estado);
        }
    }

    public boolean permiteTransaccion() {
        return this == DISPONIBLE || this == PREVENTA;
    }

    public static List<EstadoVideojuego> getEstadosComprables() {
        return List.of(DISPONIBLE, PREVENTA);
    }
}