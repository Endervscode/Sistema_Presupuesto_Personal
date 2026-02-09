package com.example.sistemapresupuestopersonal;

import com.example.sistemapresupuestopersonal.model.*;
import com.example.sistemapresupuestopersonal.service.GestorPresupuesto;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.Cursor;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    public Button btnAddAccount;
    public Button btnAddTransaction;
    public VBox saldosContent;
    public Label lblBalancePeriodo;
    public PieChart pieIngresos;
    public PieChart pieGastos;
    @FXML private Label welcomeText;
    @FXML private FlowPane cuentasPane;
    private YearMonth mesSeleccionado; // Mes actual para Resumen y Calendario

    private GestorPresupuesto gestor;
    @FXML private TabPane mainTabPane;
    @FXML private Tab tabSaldos;
    @FXML private Tab tabResumen;
    @FXML private Tab tabMovimientos;
    @FXML private Tab tabCalendario;
    @FXML private Tab tabMas;
    @FXML private TableView<Transaccion> tablaMovimientos;
    @FXML private TableColumn<Transaccion, String> colFecha;
    @FXML private TableColumn<Transaccion, String> colTipo;
    @FXML private TableColumn<Transaccion, String> colCuenta;
    @FXML private TableColumn<Transaccion, String> colCategoria;
    @FXML private TableColumn<Transaccion, String> colDescripcion;
    @FXML private TableColumn<Transaccion, String> colMonto;
    @FXML private TableColumn<Transaccion, Void> colAcciones;
    @FXML private ComboBox<Account> cbFiltroCuenta;
    @FXML private DatePicker dpMovimientos; // Filtro específico para Movimientos
    @FXML private GridPane calendarGrid; // 5. Calendario
    @FXML private Label lblMesResumen;
    @FXML private Label lblMesCalendario;
    @FXML private VBox vboxPresupuestos;

    @FXML
    private void initialize() {
        // Ocultar las pestañas superiores del TabPane (solo usamos navegación inferior)
        mainTabPane.setTabMinHeight(0);
        mainTabPane.setTabMaxHeight(0);
        mainTabPane.setTabMinWidth(0);
        mainTabPane.setTabMaxWidth(0);

        // Seleccionar la pestaña "Saldos" por defecto
        mainTabPane.getSelectionModel().select(tabSaldos);

        // Efecto hover para botones flotantes (crecen un poco al pasar el mouse)
        if (btnAddTransaction != null) {
            btnAddTransaction.setOnMouseEntered(e -> {
                btnAddTransaction.setScaleX(1.12);
                btnAddTransaction.setScaleY(1.12);
            });
            btnAddTransaction.setOnMouseExited(e -> {
                btnAddTransaction.setScaleX(1.0);
                btnAddTransaction.setScaleY(1.0);
            });
        }

        if (btnAddAccount != null) {
            btnAddAccount.setOnMouseEntered(e -> {
                btnAddAccount.setScaleX(1.12);
                btnAddAccount.setScaleY(1.12);
            });
            btnAddAccount.setOnMouseExited(e -> {
                btnAddAccount.setScaleX(1.0);
                btnAddAccount.setScaleY(1.0);
            });
        }

        // Inicializar mes seleccionado
        mesSeleccionado = YearMonth.now();
        actualizarLabelsMes();

        dpMovimientos.valueProperty().addListener((obs, oldVal, newVal) -> {
            cargarMovimientos(cbFiltroCuenta.getValue());
        });

        // Mejora visual de la tabla
        tablaMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label placeholder = new Label("No hay movimientos para mostrar");
        placeholder.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 16px;");
        tablaMovimientos.setPlaceholder(placeholder);
    }

    public void setGestor(GestorPresupuesto gestor) {
        this.gestor = gestor;
        inicializarMovimientos(); // Inicializar tablas y filtros al cargar el gestor
        cargarDashboard();
    }

    private void actualizarLabelsMes() {
        String textoMes = mesSeleccionado.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        if (lblMesResumen != null) lblMesResumen.setText(textoMes);
        if (lblMesCalendario != null) lblMesCalendario.setText(textoMes);
    }

    @FXML private void onPrevMesResumen() {
        mesSeleccionado = mesSeleccionado.minusMonths(1);
        actualizarLabelsMes();
        refrescarTodo();
    }

    @FXML private void onNextMesResumen() {
        mesSeleccionado = mesSeleccionado.plusMonths(1);
        actualizarLabelsMes();
        refrescarTodo();
    }

    private void refrescarTodo() {
        cargarDashboard();
        if (tabResumen.isSelected()) actualizarResumen();
        // Movimientos se actualiza solo si cambia su propio DatePicker o filtro cuenta
        if (tabCalendario.isSelected()) cargarCalendario();
    }

    @FXML private void onTabSaldos() {
        mainTabPane.getSelectionModel().select(tabSaldos);
        cargarDashboard();
    }

    @FXML private void onTabResumen() {
        mainTabPane.getSelectionModel().select(tabResumen);
        actualizarResumen();
    }

    private void actualizarResumen() {
        // Limpiar gráficos previos
        pieIngresos.getData().clear();
        pieGastos.getData().clear();

        // Calcular ingresos y gastos totales
        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;

        Map<String, BigDecimal> ingresosPorFuente = new HashMap<>();
        Map<String, BigDecimal> gastosPorCategoria = new HashMap<>();

        for (Transaccion t : gestor.getTransacciones()) {
            if (!YearMonth.from(t.getFecha()).equals(this.mesSeleccionado)) continue; // Filtro
            if (t.esIngreso()) {
                Ingreso ingreso = (Ingreso) t;
                String cuentaNombre = ingreso.getAccount() != null ? ingreso.getAccount().toString() : "Sin fuente";
                BigDecimal monto = ingreso.getMonto();
                ingresosPorFuente.merge(cuentaNombre, monto, BigDecimal::add);
                totalIngresos = totalIngresos.add(monto);
            } else {
                Gasto gasto = (Gasto) t;
                String categoria = gasto.getCategoria() != null ? gasto.getCategoria().getNombre() : "Sin categoría";
                BigDecimal monto = gasto.getMonto();
                gastosPorCategoria.merge(categoria, monto, BigDecimal::add);
                totalGastos = totalGastos.add(monto);
            }
        }

        // Rellenar PieChart de Ingresos
        for (Map.Entry<String, BigDecimal> entry : ingresosPorFuente.entrySet()) {
            PieChart.Data slice = new PieChart.Data(entry.getKey(), entry.getValue().doubleValue());
            pieIngresos.getData().add(slice);
        }
        pieIngresos.setTitle("Ingresos totales: " + totalIngresos.toPlainString() + " Bs");

        // Rellenar PieChart de Gastos
        for (Map.Entry<String, BigDecimal> entry : gastosPorCategoria.entrySet()) {
            PieChart.Data slice = new PieChart.Data(entry.getKey(), entry.getValue().doubleValue());
            pieGastos.getData().add(slice);
        }
        pieGastos.setTitle("Gastos totales: " + totalGastos.toPlainString() + " Bs");

        // Balance
        BigDecimal balance = totalIngresos.subtract(totalGastos);
        lblBalancePeriodo.setText("Balance del período: " + balance.toPlainString() + " Bs");
        lblBalancePeriodo.setStyle("-fx-text-fill: " + (balance.compareTo(BigDecimal.ZERO) >= 0 ? "#4CAF50" : "#F44336") + ";");

        // Si no hay datos, mostrar mensaje
        if (ingresosPorFuente.isEmpty()) {
            pieIngresos.setTitle("No hay ingresos registrados");
        }
        if (gastosPorCategoria.isEmpty()) {
            pieGastos.setTitle("No hay gastos registrados");
        }

        // Actualizar barras de presupuesto en Resumen
        if (vboxPresupuestos != null) {
            vboxPresupuestos.getChildren().clear();

            // Filtrar gastos del mes seleccionado para el cálculo
            List<Gasto> gastosMes = gestor.getTransacciones().stream()
                    .filter(t -> !t.esIngreso())
                    .map(t -> (Gasto) t)
                    .filter(t -> YearMonth.from(t.getFecha()).equals(this.mesSeleccionado))
                    .collect(Collectors.toList());

            for (Categoria cat : gestor.getCategorias()) {
                // Solo mostrar si tiene límite definido
                if (cat.getLimitePresupuesto().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal porcentaje = cat.porcentajeUsado(gastosMes);
                    double progress = porcentaje.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    VBox itemBox = new VBox(5);
                    
                    HBox header = new HBox();
                    header.setAlignment(Pos.CENTER_LEFT);
                    
                    Label lblName = new Label(cat.getNombre());
                    lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    
                    Region r = new Region();
                    HBox.setHgrow(r, Priority.ALWAYS);
                    
                    Label lblPercent = new Label(porcentaje.toPlainString() + "% (" + cat.getLimitePresupuesto().toPlainString() + " Bs)");
                    lblPercent.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");
                    
                    header.getChildren().addAll(lblName, r, lblPercent);
                    
                    ProgressBar pb = new ProgressBar(progress > 1.0 ? 1.0 : progress);
                    pb.setMaxWidth(200);
                    pb.setPrefHeight(20);
                    pb.setStyle("-fx-accent: " + (progress > 1.0 ? "#F44336" : (progress > 0.8 ? "#FF9800" : "#4CAF50")) + ";");
                    
                    itemBox.getChildren().addAll(header, pb);
                    vboxPresupuestos.getChildren().add(itemBox);
                }
            }
        }
    }

    private void inicializarMovimientos() {
        // Configurar columnas de la tabla
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFecha().toString()));
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().esIngreso() ? "Ingreso" : "Gasto"));
        colCuenta.setCellValueFactory(cell -> {
            Account acc = cell.getValue().getAccount();
            return new SimpleStringProperty(acc != null ? acc.toString() : "Sin cuenta");
        });
        colCategoria.setCellValueFactory(cell -> {
            if (cell.getValue() instanceof Gasto g) {
                Categoria cat = g.getCategoria();
                return new SimpleStringProperty(cat != null ? cat.getNombre() : "Sin categoría");
            }
            return new SimpleStringProperty("—");
        });
        colDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcion()));
        colMonto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMonto().toPlainString() + " Bs"));

        // Fix: Asegurar que la columna de acciones se actualice correctamente
        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(null));

        // Columna de acciones (botones Editar/Eliminar)
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏");
            private final Button btnEliminar = new Button("🗑");

            {
                btnEditar.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 4;");
                btnEliminar.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 4;");

                btnEditar.setOnAction(e -> {
                    Transaccion t = getTableView().getItems().get(getIndex());
                    editarTransaccion(t);
                });

                btnEliminar.setOnAction(e -> {
                    Transaccion t = getTableView().getItems().get(getIndex());
                    eliminarTransaccion(t);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10, btnEditar, btnEliminar);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        // Filtro por cuenta
        cbFiltroCuenta.getItems().addAll(gestor.getAccounts());
        cbFiltroCuenta.getItems().add(0, null); // Opción "Todas"
        cbFiltroCuenta.getSelectionModel().selectFirst();

        cbFiltroCuenta.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            cargarMovimientos(newVal);
        });

        // Carga inicial
        cargarMovimientos(null);
    }

    private void editarTransaccion(Transaccion t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-transaction.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 500, 600));
            stage.setTitle("Editar Transacción");

            AddTransactionController controller = loader.getController();
            controller.setGestor(gestor);
            controller.setTransaccionEditar(t); // Pasamos la transacción a editar
            
            controller.setOnSaveCallback(() -> {
                cargarDashboard();
                cargarMovimientos(cbFiltroCuenta.getValue()); // Refrescar tabla también
            });

            stage.show();
        } catch (IOException e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void eliminarTransaccion(Transaccion t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar transacción");
        confirm.setHeaderText("¿Eliminar esta transacción?");
        confirm.setContentText(t.getDescripcion() + " - " + t.getMonto().toPlainString() + " Bs");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            gestor.eliminarTransaccion(t);
            cargarMovimientos(cbFiltroCuenta.getValue());
            cargarDashboard();
        }
    }

    @FXML private void onTabMovimientos() {
        mainTabPane.getSelectionModel().select(tabMovimientos);
        cargarMovimientos(cbFiltroCuenta.getValue());
    }

    private void cargarMovimientos(Account filtro) {
        List<Transaccion> lista = gestor.getTransacciones();

        LocalDate fechaFiltro = dpMovimientos.getValue();
        if (fechaFiltro != null) {
            // Filtrar por día exacto si hay fecha seleccionada
            lista = lista.stream().filter(t -> t.getFecha().equals(fechaFiltro)).collect(Collectors.toList());
        }
        // Si no hay fecha seleccionada, se muestran todos los movimientos (o podrías filtrar por mes actual si prefieres)
        // Para seguir el comportamiento estándar de "sin filtro", mostramos todo.
        // Si quisieras filtrar por mes actual por defecto: else { ... filter by YearMonth.now() ... }

        if (filtro != null) {
            lista = lista.stream()
                    .filter(t -> t.getAccount() != null && t.getAccount().equals(filtro))
                    .collect(Collectors.toList());
        }

        ObservableList<Transaccion> data = FXCollections.observableArrayList(lista);
        tablaMovimientos.setItems(data);
    }

    @FXML private void onTabMas() {
        mainTabPane.getSelectionModel().select(tabMas);
    }

    private void cargarDashboard() {
        cuentasPane.getChildren().clear();

        List<Account> accounts = gestor.getAccounts();
        if (accounts.isEmpty()) {
            Label lbl = new Label("Todavía no ha creado ninguna cuenta.");
            lbl.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 16px;");
            lbl.setAlignment(Pos.CENTER);
            cuentasPane.getChildren().add(lbl);
            return;
        }

        for (Account acc : accounts) {
            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: #1E1E1E; -fx-background-radius: 12; -fx-padding: 15;");
            card.setPrefWidth(280);
            card.setAlignment(Pos.CENTER);

            Label lblIcon = new Label(acc.getIcono());
            lblIcon.setStyle("-fx-font-size: 40px;");

            Label lblNombre = new Label(acc.getNombre());
            lblNombre.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

            BigDecimal saldo = gestor.calcularSaldo(acc);
            Label lblSaldo = new Label(saldo.toPlainString() + " Bs");
            lblSaldo.setStyle("-fx-text-fill: " + (saldo.compareTo(BigDecimal.ZERO) >= 0 ? "#4CAF50" : "#F44336") + "; -fx-font-size: 20px;");

            // 2. ALERTAS VISUALES
            if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                card.setStyle("-fx-background-color: #2C0B0E; -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: #F44336; -fx-border-width: 2;");
            } else if (saldo.compareTo(new BigDecimal("100")) < 0) {
                card.setStyle("-fx-background-color: #3E2723; -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: #FF9800; -fx-border-width: 1;");
            }
            Label lblTipo = new Label(acc.getTipo());
            lblTipo.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px;");

            card.getChildren().addAll(lblIcon, lblNombre, lblSaldo, lblTipo);

            cuentasPane.getChildren().add(card);
        }

        actualizarSaldoTotal();
    }

    private void actualizarSaldoTotal() {
        BigDecimal total = gestor.getAccounts().stream()
                .map(gestor::calcularSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUsd = gestor.calcularSaldoEnDolares();

        welcomeText.setText("Saldos - Total: " + total.toPlainString() + " Bs (" + totalUsd.toPlainString() + " $)");
    }

    @FXML
    private void onAddAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-account.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 450, 400));
            stage.setTitle("Añadir Cuenta");

            AddAccountController controller = loader.getController();
            controller.setGestor(gestor);
            controller.setOnSaveCallback(this::cargarDashboard);  // o el método que refresca cuentas

            stage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario.").showAndWait();
        }
    }

    @FXML
    private void onAddTransaction() {
        try {
            System.out.println("Intentando cargar add-transaction.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-transaction.fxml"));
            Parent root = loader.load();
            System.out.println("FXML cargado correctamente");

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 500, 600));
            stage.setTitle("Añadir Transacción");

            AddTransactionController controller = loader.getController();
            controller.setGestor(gestor);
            controller.setOnSaveCallback(this::cargarDashboard); // o el método que refresques

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();  // <-- Esto muestra el error real en consola
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    @FXML
    private void onEditarDolar() {
        TextInputDialog dialog = new TextInputDialog(gestor.getTasaDolar().toPlainString());
        dialog.setTitle("Configuración");
        dialog.setHeaderText("Tipo de Cambio");
        dialog.setContentText("Ingrese el valor del Dólar en Bolívares (1 $ = ? Bs):");

        dialog.showAndWait().ifPresent(result -> {
            try {
                BigDecimal tasa = new BigDecimal(result.replace(",", "."));
                if (tasa.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                gestor.setTasaDolar(tasa);
                actualizarSaldoTotal();
            } catch (NumberFormatException e) {
                mostrarError("Valor inválido. Debe ser un número positivo.");
            }
        });
    }

    @FXML
    private void onEliminarDatos() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Zona de Peligro");
        alert.setHeaderText("¿Estás seguro de eliminar TODOS los datos?");
        alert.setContentText("Se borrarán todas las cuentas, transacciones y categorías.\nEsta acción no se puede deshacer.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            gestor.reiniciarSistema();
            cargarDashboard();
            actualizarResumen();
            cargarMovimientos(null);
            mostrarInfo("El sistema se ha reiniciado correctamente.");
        }
    }

    @FXML
    private void onGestionarRecurrentes() {
        Stage stage = new Stage();
        stage.setTitle("Gestionar Pagos Recurrentes");

        ListView<PagoRecurrente> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(gestor.getRecurrentes()));

        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PagoRecurrente item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    
                    String tipoIcono = "INGRESO".equals(item.getTipo()) ? "🟢" : "🔴";
                    Label lbl = new Label(String.format("%s Día %d - %s (%s Bs)", tipoIcono, item.getDia(), item.getDescripcion(), item.getMonto()));
                    lbl.setStyle("-fx-text-fill: black;");
                    
                    Region r = new Region();
                    HBox.setHgrow(r, Priority.ALWAYS);

                    Button btnEdit = new Button("✏");
                    btnEdit.setOnAction(e -> abrirDialogoRecurrente(item.getDia(), item));

                    Button btnDel = new Button("🗑");
                    btnDel.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                    btnDel.setOnAction(e -> {
                        gestor.eliminarRecurrente(item);
                        listView.getItems().remove(item);
                        cargarCalendario();
                    });

                    hbox.getChildren().addAll(lbl, r, btnEdit, btnDel);
                    setGraphic(hbox);
                }
            }
        });

        stage.setScene(new Scene(new VBox(listView), 400, 500));
        stage.show();
    }

    @FXML
    private void onEditarCategorias() {
        Stage stage = new Stage();
        stage.setTitle("Editar Categorías");

        // Obtener gastos del mes seleccionado para calcular progreso
        List<Gasto> gastosMes = gestor.getTransacciones().stream()
                .filter(t -> !t.esIngreso())
                .map(t -> (Gasto) t)
                .filter(t -> YearMonth.from(t.getFecha()).equals(mesSeleccionado))
                .collect(Collectors.toList());

        ListView<Categoria> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(gestor.getCategorias()));

        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Categoria item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox vBoxInfo = new VBox(5);
                    vBoxInfo.setAlignment(Pos.CENTER_LEFT);

                    Label lbl = new Label(item.getNombre());
                    lbl.setStyle("-fx-text-fill: black; -fx-font-size: 14px; -fx-font-weight: bold;");

                    // Barra de progreso
                    HBox progressBox = new HBox(10);
                    progressBox.setAlignment(Pos.CENTER_LEFT);

                    if (item.getLimitePresupuesto().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal porcentaje = item.porcentajeUsado(gastosMes);
                        double progress = porcentaje.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

                        ProgressBar pb = new ProgressBar(progress > 1.0 ? 1.0 : progress);
                        pb.setPrefWidth(120);
                        pb.setPrefHeight(10);
                        pb.setStyle("-fx-accent: " + (progress > 1.0 ? "#F44336" : (progress > 0.8 ? "#FF9800" : "#4CAF50")) + ";");

                        Label lblProgreso = new Label(porcentaje.toPlainString() + "% (" + item.getLimitePresupuesto().toPlainString() + " Bs)");
                        lblProgreso.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

                        progressBox.getChildren().addAll(pb, lblProgreso);
                    } else {
                        Label lblNoLimit = new Label("Sin límite definido");
                        lblNoLimit.setStyle("-fx-text-fill: #999; -fx-font-size: 11px; -fx-font-style: italic;");
                        progressBox.getChildren().add(lblNoLimit);
                    }

                    vBoxInfo.getChildren().addAll(lbl, progressBox);

                    Region r = new Region();
                    HBox.setHgrow(r, Priority.ALWAYS);

                    Button btnEdit = new Button("✏");
                    btnEdit.setOnAction(e -> editarCategoria(item));

                    Button btnDel = new Button("🗑");
                    btnDel.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                    btnDel.setOnAction(e -> {
                        gestor.eliminarCategoria(item);
                        listView.getItems().remove(item);
                    });

                    HBox root = new HBox(10);
                    root.setAlignment(Pos.CENTER_LEFT);
                    root.getChildren().addAll(vBoxInfo, r, btnEdit, btnDel);

                    setGraphic(root);
                }
            }
        });
        stage.setScene(new Scene(new VBox(listView), 450, 500));
        stage.show();
    }

    private void editarCategoria(Categoria c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-categoria.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 450, 400));
            stage.setTitle("Editar Categoría");

            AddCategoriaController controller = loader.getController();
            controller.setGestor(gestor);
            controller.setCategoriaEditar(c);
            
            // Refrescar dashboard al cerrar por si cambiaron nombres en gráficos
            stage.setOnHidden(e -> refrescarTodo());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onGestionarCuentas() {
        Stage stage = new Stage();
        stage.setTitle("Gestionar Cuentas");

        ListView<Account> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(gestor.getAccounts()));

        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    Label lbl = new Label(item.getIcono() + " " + item.getNombre());
                    lbl.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");
                    Region r = new Region();
                    HBox.setHgrow(r, Priority.ALWAYS);

                    Button btnDel = new Button("🗑");
                    btnDel.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-cursor: hand;");
                    btnDel.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar cuenta '" + item.getNombre() + "'?", ButtonType.YES, ButtonType.NO);
                        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                            gestor.eliminarCuenta(item);
                            listView.getItems().remove(item);
                            cargarDashboard();
                        }
                    });

                    hbox.getChildren().addAll(lbl, r, btnDel);
                    setGraphic(hbox);
                }
            }
        });

        VBox root = new VBox(10, new Label("Cuentas registradas:"), listView);
        root.setPadding(new Insets(15));
        stage.setScene(new Scene(root, 350, 450));
        stage.show();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void mostrarInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    // 5. PANTALLA CALENDARIO
    @FXML private void onTabCalendario() {
        mainTabPane.getSelectionModel().select(tabCalendario);
        cargarCalendario();
    }

    private void cargarCalendario() {
        calendarGrid.getChildren().clear();
        YearMonth mes = this.mesSeleccionado;
        LocalDate primero = mes.atDay(1);
        int diaSemana = primero.getDayOfWeek().getValue(); // 1=Lunes, 7=Domingo
        int diasEnMes = mes.lengthOfMonth();

        List<PagoRecurrente> recurrentes = gestor.getRecurrentes();

        // Cabeceras
        String[] dias = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(dias[i]);
            lbl.setStyle("-fx-text-fill: #8B949E; -fx-font-weight: bold;");
            calendarGrid.add(lbl, i, 0);
        }

        int row = 1;
        int col = diaSemana - 1;

        for (int dia = 1; dia <= diasEnMes; dia++) {
            VBox cell = new VBox(2);
            cell.setPrefSize(100, 80);
            cell.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-padding: 5;");

            Label lblDia = new Label(String.valueOf(dia));
            lblDia.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            cell.getChildren().add(lblDia);

            // Puntos por transacción
            LocalDate fechaActual = mes.atDay(dia);
            long count = gestor.getTransacciones().stream().filter(t -> t.getFecha().equals(fechaActual)).count();
            if (count > 0) {
                Label lblDot = new Label("• " + count + " movs");
                lblDot.setStyle("-fx-text-fill: #58A6FF; -fx-font-size: 10px;");
                cell.getChildren().add(lblDot);
            }

            // Barras de colores para recurrentes
            int diaFinal = dia;
            List<PagoRecurrente> delDia = recurrentes.stream().filter(r -> r.getDia() == diaFinal).toList();
            if (!delDia.isEmpty()) {
                HBox bars = new HBox(2);
                bars.setAlignment(Pos.CENTER);
                for (PagoRecurrente rp : delDia) {
                    Rectangle rect = new Rectangle(8, 3, "INGRESO".equals(rp.getTipo()) ? javafx.scene.paint.Color.LIGHTGREEN : javafx.scene.paint.Color.TOMATO);
                    bars.getChildren().add(rect);
                }
                cell.getChildren().add(bars);
            }

            cell.setOnMouseClicked(e -> onDiaCalendarioClick(diaFinal, mes.atDay(diaFinal)));
            cell.setCursor(Cursor.HAND);

            calendarGrid.add(cell, col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }
    }

    private void onDiaCalendarioClick(int dia, LocalDate fecha) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Opciones del día " + dia);
        alert.setHeaderText("¿Qué deseas hacer con el día " + fecha + "?");
        
        // Buscar recurrentes existentes
        List<PagoRecurrente> recurrentesDia = gestor.getRecurrentes().stream()
                .filter(r -> r.getDia() == dia)
                .toList();

        StringBuilder msg = new StringBuilder("Selecciona una opción:");
        if (!recurrentesDia.isEmpty()) {
            msg.append("\n\nPagos recurrentes ya programados:");
            for (PagoRecurrente rp : recurrentesDia) {
                msg.append("\n- ").append(rp.getDescripcion()).append(" (").append(rp.getMonto()).append(" Bs)");
            }
        }
        alert.setContentText(msg.toString());

        ButtonType btnVerMovs = new ButtonType("Ver Movimientos");
        ButtonType btnAddRecurrente = new ButtonType("Agregar Recurrente");
        ButtonType btnCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnVerMovs, btnAddRecurrente, btnCancel);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnVerMovs) {
                dpMovimientos.setValue(fecha); // Sincronizar DatePicker
                mainTabPane.getSelectionModel().select(tabMovimientos);
            } else if (type == btnAddRecurrente) {
                abrirDialogoRecurrente(dia, null);
            }
        });
    }

    private void abrirDialogoRecurrente(int dia, PagoRecurrente editar) {
        Dialog<PagoRecurrente> dialog = new Dialog<>();
        dialog.setTitle((editar == null ? "Nuevo" : "Editar") + " Pago Recurrente (Día " + dia + ")");
        dialog.setHeaderText("Configurar transacción automática para el día " + dia + " de cada mes");

        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField desc = new TextField();
        desc.setPromptText("Descripción");
        TextField monto = new TextField();
        monto.setPromptText("Monto");

        ComboBox<String> tipo = new ComboBox<>();
        tipo.getItems().addAll("GASTO", "INGRESO");
        

        ComboBox<Categoria> categoria = new ComboBox<>();
        categoria.setItems(FXCollections.observableArrayList(gestor.getCategorias()));

        ComboBox<Account> cuenta = new ComboBox<>();
        cuenta.setItems(FXCollections.observableArrayList(gestor.getAccounts()));

        // Pre-llenar si es edición
        if (editar != null) {
            desc.setText(editar.getDescripcion());
            monto.setText(editar.getMonto().toPlainString());
            tipo.setValue(editar.getTipo());
            cuenta.getSelectionModel().select(gestor.getAccounts().stream().filter(a -> a.getId().equals(editar.getCuentaId())).findFirst().orElse(null));
            if ("GASTO".equals(editar.getTipo())) {
                categoria.getSelectionModel().select(gestor.getCategorias().stream().filter(c -> c.getId().equals(editar.getCategoriaId())).findFirst().orElse(null));
            }
        } else {
            tipo.setValue("GASTO");
        }

        grid.add(new Label("Descripción:"), 0, 0);
        grid.add(desc, 1, 0);
        grid.add(new Label("Monto:"), 0, 1);
        grid.add(monto, 1, 1);
        grid.add(new Label("Tipo:"), 0, 2);
        grid.add(tipo, 1, 2);
        grid.add(new Label("Categoría:"), 0, 3);
        grid.add(categoria, 1, 3);
        grid.add(new Label("Cuenta:"), 0, 4);
        grid.add(cuenta, 1, 4);

        // Deshabilitar categoría si es Ingreso
        tipo.valueProperty().addListener((obs, oldVal, newVal) -> {
            categoria.setDisable("INGRESO".equals(newVal));
        });

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarBtn) {
                try {
                    BigDecimal m = new BigDecimal(monto.getText());
                    String catId = (categoria.getValue() != null && "GASTO".equals(tipo.getValue())) ? categoria.getValue().getId() : "null";
                    String accId = cuenta.getValue() != null ? cuenta.getValue().getId() : "null";

                    return new PagoRecurrente(
                            editar != null ? editar.getId() : UUID.randomUUID().toString(),
                            desc.getText(),
                            m,
                            dia,
                            tipo.getValue(),
                            catId,
                            accId
                    );
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        Optional<PagoRecurrente> result = dialog.showAndWait();

        result.ifPresent(rp -> {
            if (editar != null) {
                gestor.actualizarRecurrente(rp);
            } else {
                gestor.agregarRecurrente(rp);
            }
            mostrarInfo("Pago recurrente agregado correctamente.");
            cargarCalendario(); // Refrescar puntos
            cargarDashboard(); // Refrescar saldos si se aplicó hoy
        });
    }
}