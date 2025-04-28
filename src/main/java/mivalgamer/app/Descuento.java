package mivalgamer.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Descuento {
    private final String codigo;
    private final String nombre;
    private final double porcentaje;
    private final LocalDateTime fechaInicio;
    private final LocalDateTime fechaExpiracion;
    private final boolean esAcumulable;
    private final String tituloJuego;
    private final String estudioJuego;
    private final double precioOriginal;

    // Constructor desde BD
    public Descuento(String codigo, String nombre, double porcentaje,
                     LocalDateTime fechaInicio, LocalDateTime fechaExpiracion,
                     boolean esAcumulable, String tituloJuego, String estudioJuego,
                     double precioOriginal) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.porcentaje = porcentaje;
        this.fechaInicio = fechaInicio;
        this.fechaExpiracion = fechaExpiracion;
        this.esAcumulable = esAcumulable;
        this.tituloJuego = tituloJuego;
        this.estudioJuego = estudioJuego;
        this.precioOriginal = precioOriginal;
    }

    public String obtenerInfoDescuento() {
        return String.format(
                "%s - %s\n" +
                        "   Precio original: $%.2f\n" +
                        "   Precio con %.0f%% OFF: $%.2f\n" +
                        "   Válido hasta: %s\n" +
                        "-----------------------------------",
                tituloJuego, estudioJuego, precioOriginal, porcentaje,
                calcularPrecioConDescuento(),
                fechaExpiracion.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    public double calcularPrecioConDescuento() {
        return esVigente() ? precioOriginal * (1 - porcentaje / 100) : precioOriginal;
    }

    public boolean esVigente() {
        LocalDateTime ahora = LocalDateTime.now();
        return !ahora.isBefore(fechaInicio) && !ahora.isAfter(fechaExpiracion);
    }

    // Getters
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public double getPorcentaje() { return porcentaje; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public boolean isEsAcumulable() { return esAcumulable; }
}