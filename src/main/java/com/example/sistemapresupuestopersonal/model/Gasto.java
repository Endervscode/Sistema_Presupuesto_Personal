package com.example.sistemapresupuestopersonal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Gasto extends Transaccion {
    private Categoria categoria;

    public Gasto(long id, LocalDate fecha, BigDecimal monto, String descripcion, Categoria categoria, Account account) {
        super(id, fecha, monto, descripcion, account);
        this.categoria = categoria;
    }

    public Categoria getCategoria() { return categoria; }

    @Override
    public boolean esIngreso() { return false; }
}