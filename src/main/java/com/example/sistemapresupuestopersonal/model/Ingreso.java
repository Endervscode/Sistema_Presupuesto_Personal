package com.example.sistemapresupuestopersonal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Ingreso extends Transaccion {

    public Ingreso(long id, LocalDate fecha, BigDecimal monto, String descripcion, Account account) {
        super(id, fecha, monto, descripcion, account);
    }

    @Override
    public boolean esIngreso() { return true; }
}