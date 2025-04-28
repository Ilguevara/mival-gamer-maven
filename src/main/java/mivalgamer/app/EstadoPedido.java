package mivalgamer.app;
public enum EstadoPedido {
    PENDIENTE,
    PAGADO,
    ENVIADO,
    CANCELADO;

    // Método para convertir desde valor de BD
    public static EstadoPedido desdeString(String valor) {
        return valueOf(valor.toUpperCase());
    }

    // Lógica de transición de estados acorde a la BD
    public boolean puedeCambiarA(EstadoPedido nuevoEstado) {
        return switch (this) {
            case PENDIENTE ->
                    nuevoEstado == PAGADO ||
                            nuevoEstado == CANCELADO;
            case PAGADO ->
                    nuevoEstado == ENVIADO ||
                            nuevoEstado == CANCELADO;
            case ENVIADO, CANCELADO -> false;
        };
    }

    public boolean esEstadoFinal() {
        return this == ENVIADO || this == CANCELADO;
    }
}