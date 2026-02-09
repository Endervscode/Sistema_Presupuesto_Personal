package com.example.sistemapresupuestopersonal.model;

import java.math.BigDecimal;

public class PagoRecurrente {
    private String id;
    private String descripcion;
    private BigDecimal monto;
    private int dia;
    private String tipo; // "INGRESO" or "GASTO"
    private String categoriaId;
    private String cuentaId;

    public PagoRecurrente(String id, String descripcion, BigDecimal monto, int dia, String tipo, String categoriaId, String cuentaId) {
        this.id = id;
        this.descripcion = descripcion;
        this.monto = monto;
        this.dia = dia;
        this.tipo = tipo;
        this.categoriaId = categoriaId;
        this.cuentaId = cuentaId;
    }

    public String getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getMonto() { return monto; }
    public int getDia() { return dia; }
    public String getTipo() { return tipo; }
    public String getCategoriaId() { return categoriaId; }
    public String getCuentaId() { return cuentaId; }
}