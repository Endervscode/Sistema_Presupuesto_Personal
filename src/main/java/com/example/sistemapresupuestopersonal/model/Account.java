package com.example.sistemapresupuestopersonal.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class Account {
        private String id;
        private String nombre;
        private String tipo;
        private BigDecimal saldoInicial;
        private String icono;

        // Constructor nuevo con ID
        public Account(String id, String nombre, String tipo, BigDecimal saldoInicial, String icono) {
            this.id = id != null ? id : UUID.randomUUID().toString();
            this.nombre = nombre;
            this.tipo = tipo;
            this.saldoInicial = saldoInicial != null ? saldoInicial : BigDecimal.ZERO;
            this.icono = icono != null ? icono : "🏦";
        }

    // Getters y setters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public String getIcono() { return icono; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return Objects.equals(nombre, account.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
    @Override
    public String toString() {
        return icono + " " + nombre;
    }
}