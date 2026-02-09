package com.example.sistemapresupuestopersonal.service;

import com.example.sistemapresupuestopersonal.model.*;
import com.example.sistemapresupuestopersonal.persistence.AlmacenDatosTxt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.YearMonth;

/**
 * Clase central de negocio que gestiona transacciones, categorías (con sus presupuestos integrados)
 * y cálculos financieros. Coordina la persistencia a través de AlmacenDatosTxt.
 */
public class GestorPresupuesto {
    private final List<Transaccion> transacciones = new ArrayList<>();
    private final List<Categoria> categorias = new ArrayList<>();
    private final List<Account> accounts = new ArrayList<>();
    private final AlmacenDatosTxt almacen;
    private final Usuario usuario;

    public GestorPresupuesto(Usuario usuario, AlmacenDatosTxt almacen) {
        this.almacen = almacen;
        this.usuario = usuario;
        transacciones.addAll(almacen.cargarTransacciones());
        categorias.addAll(almacen.cargarCategorias());
        accounts.addAll(almacen.cargarAccounts());
        this.tasaDolar = almacen.cargarConfiguracion();
        verificarRecurrentes();
    }

    public void agregarAccount(Account account) {
        accounts.add(account);
        almacen.guardarAccounts(accounts);
    }

    public void eliminarCuenta(Account account) {
        accounts.remove(account);
        almacen.guardarAccounts(accounts);
    }

    public void actualizarCuenta(Account account) {
        // Asumimos que el objeto account ya fue modificado en memoria, solo guardamos
        almacen.guardarAccounts(accounts);
    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Transacciones
    // ────────────────────────────────────────────────────────────────────────────────

    public void agregarIngreso(Ingreso ingreso) {
        transacciones.add(ingreso);
        almacen.guardarTransacciones(transacciones);
    }

    public void eliminarTransaccion(Transaccion t) {
        transacciones.remove(t);
        almacen.guardarTransacciones(transacciones);
    }

    public void actualizarTransaccion(Transaccion original, Transaccion modificada) {
        int index = transacciones.indexOf(original);
        if (index != -1) {
            transacciones.set(index, modificada);
            almacen.guardarTransacciones(transacciones);
        }
    }

    /**
     * Valida si se puede agregar el gasto sin violar reglas de saldo o presupuesto.
     * @return null si OK, o mensaje de error si hay problema
     */
    public String validarAgregarGasto(Gasto gasto) {
        // 1. Validar saldo de la cuenta específica
        if (gasto.getAccount() != null) {
            BigDecimal saldoCuenta = calcularSaldo(gasto.getAccount());
            BigDecimal nuevoSaldo = saldoCuenta.subtract(gasto.getMonto());

            if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                return "Saldo insuficiente en la cuenta '" + gasto.getAccount().getNombre() + "'.\n" +
                        "Saldo actual: " + saldoCuenta.toPlainString() + " Bs\n" +
                        "Gasto: " + gasto.getMonto().toPlainString() + " Bs\n" +
                        "Saldo restante: " + nuevoSaldo.toPlainString() + " Bs";
            }
        }

        // 2. Validar presupuesto categoría
        return validarPresupuestoCategoria(gasto, null);
    }

    public String validarModificacionGasto(Gasto nuevo, Transaccion original) {
        // 1. Validar Saldo (considerando reversión del original)
        if (nuevo.getAccount() != null) {
            BigDecimal saldoActual = calcularSaldo(nuevo.getAccount());
            BigDecimal saldoDisponible = saldoActual;

            // Si la original afectaba a esta misma cuenta, revertimos su efecto para ver el saldo real disponible
            if (original != null && original.getAccount() != null && original.getAccount().equals(nuevo.getAccount())) {
                if (original.esIngreso()) {
                    saldoDisponible = saldoDisponible.subtract(original.getMonto()); // Si era ingreso, lo quitamos
                } else {
                    saldoDisponible = saldoDisponible.add(original.getMonto()); // Si era gasto, lo devolvemos
                }
            }
            // Si la original era de otra cuenta, el saldoActual de esta cuenta ya es el correcto.

            BigDecimal nuevoSaldo = saldoDisponible.subtract(nuevo.getMonto());
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                return "Saldo insuficiente en la cuenta '" + nuevo.getAccount().getNombre() + "'.\n" +
                        "Saldo disponible (ajustado): " + saldoDisponible.toPlainString() + " Bs\n" +
                        "Nuevo gasto: " + nuevo.getMonto().toPlainString() + " Bs";
            }
        }

        // 2. Validar Presupuesto
        return validarPresupuestoCategoria(nuevo, original);
    }

    private String validarPresupuestoCategoria(Gasto nuevo, Transaccion original) {
        Categoria cat = nuevo.getCategoria();
        if (cat != null && cat.getLimitePresupuesto() != null && cat.getLimitePresupuesto().compareTo(BigDecimal.ZERO) > 0) {
            YearMonth mes = YearMonth.from(nuevo.getFecha());

            // Calcular total gastado en el mes para esta categoría
            BigDecimal totalGastado = transacciones.stream()
                    .filter(t -> !t.esIngreso())
                    .map(t -> (Gasto) t)
                    .filter(g -> g.getCategoria() != null && g.getCategoria().getId().equals(cat.getId()))
                    .filter(t -> YearMonth.from(t.getFecha()).equals(mes))
                    .map(Gasto::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Si estamos editando, restar el original del total (si era gasto y del mismo mes/cat)
            if (original instanceof Gasto && !original.esIngreso()) {
                Gasto gOriginal = (Gasto) original;
                if (gOriginal.getCategoria() != null && gOriginal.getCategoria().getId().equals(cat.getId()) && YearMonth.from(gOriginal.getFecha()).equals(mes)) {
                    totalGastado = totalGastado.subtract(gOriginal.getMonto());
                }
            }

            BigDecimal nuevoTotal = totalGastado.add(nuevo.getMonto());

            if (nuevoTotal.compareTo(cat.getLimitePresupuesto()) > 0) {
                BigDecimal porcentaje = nuevoTotal
                        .divide(cat.getLimitePresupuesto(), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, BigDecimal.ROUND_HALF_UP);

                return "Presupuesto excedido en categoría '" + cat.getNombre() + "'\n" +
                        "Límite: " + cat.getLimitePresupuesto().toPlainString() + " Bs\n" +
                        "Gasto acumulado + nuevo: " + nuevoTotal.toPlainString() + " Bs\n" +
                        "(" + porcentaje.toPlainString() + "% del límite)";
            }
        }

        return null; // Todo OK
    }

    /**
     * Registra el gasto (solo llamar después de validación y confirmación del usuario)
     */
    public void confirmarAgregarGasto(Gasto gasto) {
        transacciones.add(gasto);
        almacen.guardarTransacciones(transacciones);
    }

    public BigDecimal calcularSaldo() {
        return accounts.stream()
                .map(this::calcularSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Saldo de una cuenta específica
    public BigDecimal calcularSaldo(Account account) {
        if (account == null) return calcularSaldo();

        BigDecimal ingresos = transacciones.stream()
                .filter(t -> t.esIngreso() && t.getAccount() != null && t.getAccount().equals(account))
                .map(Transaccion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gastos = transacciones.stream()
                .filter(t -> !t.esIngreso() && t.getAccount() != null && t.getAccount().equals(account))
                .map(Transaccion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ingresos.subtract(gastos).add(account.getSaldoInicial());
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Categorías y Presupuestos integrados
    // ────────────────────────────────────────────────────────────────────────────────

    public void agregarCategoria(Categoria categoria) {
        categorias.add(categoria);
        almacen.guardarCategorias(categorias);
    }

    public void eliminarCategoria(Categoria c) {
        categorias.remove(c);
        almacen.guardarCategorias(categorias);
    }

    public void actualizarCategoria(Categoria c) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getId().equals(c.getId())) {
                categorias.set(i, c);
                break;
            }
        }
        almacen.guardarCategorias(categorias);
    }

    public List<Categoria> getCategorias() {
        return new ArrayList<>(categorias);
    }

    public List<Transaccion> getTransacciones() {
        return new ArrayList<>(transacciones);
    }

    private BigDecimal tasaDolar = BigDecimal.ONE;  // Default 1 (para evitar división por 0)

    // Métodos nuevos
    public void setTasaDolar(BigDecimal tasa) {
        this.tasaDolar = tasa != null && tasa.compareTo(BigDecimal.ZERO) > 0 ? tasa : BigDecimal.ONE;
        almacen.guardarConfiguracion(this.tasaDolar);
    }

    public BigDecimal getTasaDolar() {
        return tasaDolar;
    }

    public BigDecimal calcularSaldoEnDolares() {
        return calcularSaldo().divide(tasaDolar, 2, BigDecimal.ROUND_HALF_UP);
    }

    public void reiniciarSistema() {
        transacciones.clear();
        categorias.clear();
        accounts.clear();
        almacen.borrarTodosLosDatos();
    }

    // 6. PAGOS RECURRENTES
    private void verificarRecurrentes() {
        List<PagoRecurrente> recurrentes = almacen.cargarRecurrentes();
        java.time.LocalDate hoy = java.time.LocalDate.now();
        YearMonth mesActual = YearMonth.from(hoy);

        boolean cambios = false;
        for (PagoRecurrente rp : recurrentes) {
            // Verificar si ya existe la transacción este mes
            boolean existe = transacciones.stream()
                    .anyMatch(t -> t.getDescripcion().equals(rp.getDescripcion()) &&
                            YearMonth.from(t.getFecha()).equals(mesActual));

            if (!existe && hoy.getDayOfMonth() >= rp.getDia()) {
                // Crear transacción
                Account acc = accounts.stream().filter(a -> a.getId().equals(rp.getCuentaId())).findFirst().orElse(null);
                if (acc == null) continue;

                long id = transacciones.size() + 1;
                java.time.LocalDate fecha = mesActual.atDay(rp.getDia());

                if ("INGRESO".equalsIgnoreCase(rp.getTipo())) {
                    transacciones.add(new Ingreso(id, fecha, rp.getMonto(), rp.getDescripcion(), acc));
                } else {
                    Categoria cat = categorias.stream().filter(c -> c.getId().equals(rp.getCategoriaId())).findFirst().orElse(null);
                    transacciones.add(new Gasto(id, fecha, rp.getMonto(), rp.getDescripcion(), cat, acc));
                }
                cambios = true;
            }
        }
        if (cambios) almacen.guardarTransacciones(transacciones);
    }

    public void agregarRecurrente(PagoRecurrente rp) {
        List<PagoRecurrente> list = almacen.cargarRecurrentes();
        list.add(rp);
        almacen.guardarRecurrentes(list);
        verificarRecurrentes(); // Verificar inmediatamente si debe aplicarse hoy
    }

    public List<PagoRecurrente> getRecurrentes() {
        return almacen.cargarRecurrentes();
    }

    public void eliminarRecurrente(PagoRecurrente rp) {
        List<PagoRecurrente> list = getRecurrentes();
        list.removeIf(r -> r.getId().equals(rp.getId()));
        almacen.guardarRecurrentes(list);
    }

    public void actualizarRecurrente(PagoRecurrente rp) {
        List<PagoRecurrente> list = getRecurrentes();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(rp.getId())) {
                list.set(i, rp);
                break;
            }
        }
        almacen.guardarRecurrentes(list);
        verificarRecurrentes();
    }
}