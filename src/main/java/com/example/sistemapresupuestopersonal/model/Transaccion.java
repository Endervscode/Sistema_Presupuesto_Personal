package com.example.sistemapresupuestopersonal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class Transaccion {
    private long id;
    private LocalDate fecha;
    private BigDecimal monto;
    private String descripcion;
    private Account account;  // cuenta asociada

    public Transaccion(long id, LocalDate fecha, BigDecimal monto, String descripcion, Account account) {
        this.id = id;
        this.fecha = fecha != null ? fecha : LocalDate.now();
        this.monto = monto != null ? monto : BigDecimal.ZERO;
        this.descripcion = descripcion != null ? descripcion : "";
        this.account = account;
    }

    public long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public Account getAccount() { return account; }

    public abstract boolean esIngreso();
}