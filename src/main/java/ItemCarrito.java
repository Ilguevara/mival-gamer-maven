public class ItemCarrito {
    private final Long idItem;
    private final Videojuego videojuego;
    private int cantidad;
    private final CarritoCompra carrito;

    public ItemCarrito(Long idItem, Videojuego videojuego, int cantidad, CarritoCompra carrito) {
        if (idItem == null) {
            throw new IllegalArgumentException("ID de ítem no puede ser nulo");
        }
        if (videojuego == null) {
            throw new IllegalArgumentException("Videojuego no puede ser nulo");
        }
        if (carrito == null) {
            throw new IllegalArgumentException("Carrito no puede ser nulo");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser positiva");
        }

        this.idItem = idItem;
        this.videojuego = videojuego;
        this.cantidad = cantidad;
        this.carrito = carrito;
    }

    public double calcularSubtotal() {
        return videojuego.getPrecio() * cantidad;
    }

    public void actualizarCantidad(int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        this.cantidad = nuevaCantidad;
    }

    // Getters
    public Long getIdItem() { return idItem; }
    public Videojuego getVideojuego() { return videojuego; }
    public int getCantidad() { return cantidad; }
    public CarritoCompra getCarrito() { return carrito; }
    public double getSubtotal() { return calcularSubtotal(); }

    @Override
    public String toString() {
        // Opción 1: Mostrando solo el ID de plataforma
        return String.format("%s (Plataforma ID: %d) - %d x $%.2f = $%.2f",
                videojuego.getTitulo(),
                videojuego.getIdPlataforma(),
                cantidad,
                videojuego.getPrecio(),
                getSubtotal());


    }
}