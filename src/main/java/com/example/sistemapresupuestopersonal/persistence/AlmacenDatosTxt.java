package com.example.sistemapresupuestopersonal.persistence;

import com.example.sistemapresupuestopersonal.model.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlmacenDatosTxt {
    private static final String TRANSACCIONES_FILE = "transacciones.txt";
    private static final String CATEGORIAS_FILE = "categorias.txt";
    private static final String ACCOUNTS_FILE = "cuentas.txt";
    private static final String CONFIG_FILE = "config.txt";
    private static final String RECURRENTES_FILE = "recurrentes.txt";

    private String sanitize(String s) {
        return s == null ? "" : s.replace("|", " ").trim();
    }

    public List<Account> cargarAccounts() {
        List<Account> list = new ArrayList<>();
        Path path = Path.of(ACCOUNTS_FILE);
        if (!Files.exists(path)) return list;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                String id = parts[0];
                String nombre = sanitize(parts[1]);
                String tipo = sanitize(parts[2]);
                BigDecimal saldo = new BigDecimal(parts[3].trim());
                String icono = parts.length > 4 ? sanitize(parts[4]) : "🏦";

                list.add(new Account(id, nombre, tipo, saldo, icono));
            }
        } catch (Exception e) {
            // silent
        }
        return list;
    }

    public void guardarAccounts(List<Account> accounts) {
        Path path = Path.of(ACCOUNTS_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Account a : accounts) {
                bw.write(String.join("|",
                        sanitize(a.getId()),
                        sanitize(a.getNombre()),
                        sanitize(a.getTipo()),
                        a.getSaldoInicial().toPlainString(),
                        sanitize(a.getIcono())));
                bw.newLine();
            }
        } catch (Exception ignored) {}
    }

    public List<Transaccion> cargarTransacciones() {
        List<Transaccion> list = new ArrayList<>();
        Path path = Path.of(TRANSACCIONES_FILE);
        if (!Files.exists(path)) return list;

        // Cargar cuentas y categorías
        List<Account> accounts = cargarAccounts();
        Map<String, Account> accountById = accounts.stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        List<Categoria> categorias = cargarCategorias();
        Map<String, Categoria> catById = categorias.stream()
                .collect(Collectors.toMap(Categoria::getId, c -> c));


        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;

                try {
                    long id = Long.parseLong(parts[0].trim());
                    LocalDate fecha = LocalDate.parse(parts[1].trim());
                    BigDecimal monto = new BigDecimal(parts[2].trim());
                    String desc = sanitize(parts[3]);

                    String tipo = parts[4].trim();

                    if ("INGRESO".equalsIgnoreCase(tipo)) {
                        String cuentaId = parts.length > 5 ? parts[5].trim() : null;
                        Account account = cuentaId != null && !cuentaId.equals("null") ? accountById.get(cuentaId) : null;
                        list.add(new Ingreso(id, fecha, monto, desc, account));
                    } else {
                        String catId = parts.length > 5 ? parts[5].trim() : null;
                        String cuentaId = parts.length > 6 ? parts[6].trim() : null;
                        Categoria categoria = catId != null && !catId.equals("null") ? catById.get(catId) : null;
                        Account account = cuentaId != null && !cuentaId.equals("null") ? accountById.get(cuentaId) : null;
                        list.add(new Gasto(id, fecha, monto, desc, categoria, account));
                    }
                } catch (Exception e) {
                    System.err.println("Línea inválida ignorada: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void guardarTransacciones(List<Transaccion> trans) {
        Path path = Path.of(TRANSACCIONES_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Transaccion t : trans) {
                StringBuilder sb = new StringBuilder();
                sb.append(t.getId()).append("|")
                        .append(t.getFecha()).append("|")
                        .append(t.getMonto()).append("|")
                        .append(sanitize(t.getDescripcion()));

                String cuentaNombre = t.getAccount() != null ? sanitize(t.getAccount().getNombre()) : "Sin cuenta";

                if (t.esIngreso()) {
                    String cuentaId = t.getAccount() != null ? t.getAccount().getId() : "null";
                    sb.append("|INGRESO|").append(cuentaId);
                } else {
                    Gasto g = (Gasto) t;
                    String cuentaId = t.getAccount() != null ? t.getAccount().getId() : "null";
                    String catId = g.getCategoria() != null ? g.getCategoria().getId() : "null";
                    sb.append("|GASTO|").append(catId).append("|").append(cuentaId);
                }

                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ────────────────────────────────────────────────
    // Categorías (ya estaba bien)
    // ────────────────────────────────────────────────


    // Cargar categorías + su presupuesto asociado
    public List<Categoria> cargarCategorias() {
        List<Categoria> list = new ArrayList<>();
        Path path = Path.of(CATEGORIAS_FILE);
        if (!Files.exists(path)) return list;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) continue;

                String id = parts[0];
                String nombre = sanitize(parts[1]);
                BigDecimal limite = parts[2].isEmpty() ? BigDecimal.ZERO : new BigDecimal(parts[2].trim());
                String desc = sanitize(parts[3]);

                Categoria cat = new Categoria(id, nombre, limite, desc);

                if (parts.length >= 7) {
                    cat.setPeriodoPresupuesto(sanitize(parts[4]));
                    cat.setFechaInicioPresupuesto(parts[5].isEmpty() ? null : LocalDate.parse(parts[5].trim()));
                    cat.setFechaFinPresupuesto(parts[6].isEmpty() ? null : LocalDate.parse(parts[6].trim()));
                    cat.setDescripcionPresupuesto(sanitize(parts.length > 7 ? parts[7] : ""));
                }

                list.add(cat);
            }
        } catch (IOException e) {
            System.err.println("Error cargando categorías: " + e.getMessage());
        }
        return list;
    }


    public void guardarCategorias(List<Categoria> cats) {
        Path path = Path.of(CATEGORIAS_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Categoria c : cats) {
                StringBuilder sb = new StringBuilder();
                sb.append(c.getId()).append("|")
                        .append(sanitize(c.getNombre())).append("|")
                        .append(c.getLimitePresupuesto()).append("|")
                        .append(sanitize(c.getDescripcion()));

                if (c.tienePresupuesto()) {
                    sb.append("|").append(sanitize(c.getPeriodoPresupuesto()))
                            .append("|").append(c.getFechaInicioPresupuesto() != null ? c.getFechaInicioPresupuesto() : "")
                            .append("|").append(c.getFechaFinPresupuesto() != null ? c.getFechaFinPresupuesto() : "")
                            .append("|").append(sanitize(c.getDescripcionPresupuesto()));
                }

                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error guardando categorías: " + e.getMessage());
        }
    }

    public BigDecimal cargarConfiguracion() {
        Path path = Path.of(CONFIG_FILE);
        if (!Files.exists(path)) return BigDecimal.ONE;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = br.readLine();
            if (line != null && !line.isEmpty()) {
                return new BigDecimal(line.trim());
            }
        } catch (Exception e) {
            // silent
        }
        return BigDecimal.ONE;
    }

    public void guardarConfiguracion(BigDecimal tasa) {
        try (BufferedWriter bw = Files.newBufferedWriter(Path.of(CONFIG_FILE), StandardCharsets.UTF_8)) {
            bw.write(tasa.toPlainString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<PagoRecurrente> cargarRecurrentes() {
        List<PagoRecurrente> list = new ArrayList<>();
        Path path = Path.of(RECURRENTES_FILE);
        if (!Files.exists(path)) return list;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;
                list.add(new PagoRecurrente(
                        parts[0], parts[1], new BigDecimal(parts[2]),
                        Integer.parseInt(parts[3]), parts[4], parts[5], parts[6]
                ));
            }
        } catch (Exception e) {
            // silent
        }
        return list;
    }

    public void guardarRecurrentes(List<PagoRecurrente> list) {
        Path path = Path.of(RECURRENTES_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (PagoRecurrente rp : list) {
                bw.write(String.join("|",
                        rp.getId(),
                        sanitize(rp.getDescripcion()),
                        rp.getMonto().toPlainString(),
                        String.valueOf(rp.getDia()),
                        sanitize(rp.getTipo()),
                        sanitize(rp.getCategoriaId()),
                        sanitize(rp.getCuentaId())
                ));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void borrarTodosLosDatos() {
        try {
            Files.deleteIfExists(Path.of(TRANSACCIONES_FILE));
            Files.deleteIfExists(Path.of(CATEGORIAS_FILE));
            Files.deleteIfExists(Path.of(ACCOUNTS_FILE));
            Files.deleteIfExists(Path.of(CONFIG_FILE));
            Files.deleteIfExists(Path.of(RECURRENTES_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}