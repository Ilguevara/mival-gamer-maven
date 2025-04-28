public enum EstadoCarrito {
    ACTIVO,
    FINALIZADO,
    ABANDONADO,
    PENDIENTE_PAGO;

    // Método para convertir desde valor de BD
    public static EstadoCarrito desdeString(String valor) {
        return valueOf(valor.toUpperCase());
    }

    // Método para validar transiciones según reglas de negocio
    public boolean puedeTransicionarA(EstadoCarrito nuevoEstado) {
        return switch (this) {
            case ACTIVO ->
                    nuevoEstado == FINALIZADO ||
                            nuevoEstado == ABANDONADO ||
                            nuevoEstado == PENDIENTE_PAGO;
            case PENDIENTE_PAGO ->
                    nuevoEstado == FINALIZADO ||
                            nuevoEstado == ABANDONADO;
            default -> false;
        };
    }
}