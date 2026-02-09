package com.example.sistemapresupuestopersonal.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Categoria {
    private String id;
    private String nombre;
    private BigDecimal limitePresupuesto;
    private String descripcion;

    // Campos de presupuesto
    private String periodoPresupuesto;
    private LocalDate fechaInicioPresupuesto;
    private LocalDate fechaFinPresupuesto;
    private String descripcionPresupuesto;

    public Categoria(String id,String nombre, BigDecimal limitePresupuesto, String descripcion) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.nombre = nombre != null ? nombre.trim() : "";
        this.limitePresupuesto = limitePresupuesto != null ? limitePresupuesto : BigDecimal.ZERO;
        this.descripcion = descripcion != null ? descripcion.trim() : "";
        this.periodoPresupuesto = null;
        this.fechaInicioPresupuesto = null;
        this.fechaFinPresupuesto = null;
        this.descripcionPresupuesto = null;
    }

    // Getters y setters existentes...
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre != null ? nombre.trim() : ""; }
    public BigDecimal getLimitePresupuesto() { return limitePresupuesto; }
    public void setLimitePresupuesto(BigDecimal limite) { this.limitePresupuesto = limite != null ? limite : BigDecimal.ZERO; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String desc) { this.descripcion = desc != null ? desc.trim() : ""; }

    // Nuevos getters/setters para presupuesto
    public boolean tienePresupuesto() {
        return periodoPresupuesto != null && fechaInicioPresupuesto != null && fechaFinPresupuesto != null;
    }

    public String getId() { return id; }
    public String getPeriodoPresupuesto() { return periodoPresupuesto; }
    public void setPeriodoPresupuesto(String periodo) { this.periodoPresupuesto = periodo; }
    public LocalDate getFechaInicioPresupuesto() { return fechaInicioPresupuesto; }
    public void setFechaInicioPresupuesto(LocalDate fecha) { this.fechaInicioPresupuesto = fecha; }
    public LocalDate getFechaFinPresupuesto() { return fechaFinPresupuesto; }
    public void setFechaFinPresupuesto(LocalDate fecha) { this.fechaFinPresupuesto = fecha; }
    public String getDescripcionPresupuesto() { return descripcionPresupuesto; }
    public void setDescripcionPresupuesto(String desc) { this.descripcionPresupuesto = desc; }

    public BigDecimal calcularGastoTotal(List<Gasto> gastos) {
        return gastos.stream()
                .filter(g -> g.getCategoria() != null && g.getCategoria().equals(this))
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal porcentajeUsado(List<Gasto> gastos) {
        if (limitePresupuesto.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal total = calcularGastoTotal(gastos);
        return total.divide(limitePresupuesto, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria categoria = (Categoria) o;
        return nombre.trim().equalsIgnoreCase(categoria.nombre.trim());
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre.trim().toLowerCase());
    }

    @Override
    public String toString() {
        return nombre + " (límite: " + limitePresupuesto + ")";
    }
}