package mivalgamer.app;

import java.sql.SQLException;
import java.util.List;

public class ItemCarrito {
    private final Videojuego videojuego;
    private int cantidad;
    private final double precioUnitario;

    // Constructor principal
    public ItemCarrito(Videojuego videojuego, int cantidad, double precioUnitario) {
        if (videojuego == null) {
            throw new IllegalArgumentException("Videojuego no puede ser nulo");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser positiva");
        }
        this.videojuego = videojuego;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public double calcularSubtotal() {
        return precioUnitario * cantidad;
    }

    public double getSubtotal() {
        return calcularSubtotal();
    }

    public void actualizarCantidad(int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        this.cantidad = nuevaCantidad;
    }

    // Nuevo método para obtener el precio unitario (necesario para CarritoCompra)
    public double getPrecioUnitario() {
        return precioUnitario;
    }

    // Getter del videojuego
    public Videojuego getVideojuego() {
        return videojuego;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String obtenerNombresPlataformas(java.sql.Connection connection) {
        try {
            List<Plataforma> plataformas = videojuego.obtenerPlataformas(connection);
            if (plataformas == null || plataformas.isEmpty()) {
                return "Plataformas desconocidas";
            }
            return plataformas.stream()
                    .map(Plataforma::getNombreComercial)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Plataformas desconocidas");
        } catch (Exception ex) {
            return "Plataformas desconocidas";
        }
    }



    @Override
    public String toString() {
        return String.format("%s - %d x $%.2f = $%.2f",
                videojuego.getTitulo(),
                cantidad,
                precioUnitario,
                calcularSubtotal());
    }
}