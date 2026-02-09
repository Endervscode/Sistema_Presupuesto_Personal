package com.example.sistemapresupuestopersonal;

import com.example.sistemapresupuestopersonal.model.*;
import com.example.sistemapresupuestopersonal.service.GestorPresupuesto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AddTransactionController {

    @FXML private DatePicker dpFecha;
    @FXML private FlowPane cuentasFlow;
    @FXML private Button btnIngreso;
    @FXML private Button btnGasto;
    @FXML private HBox categoriaBox;
    @FXML private ChoiceBox<Categoria> cbCategoria;
    @FXML private TextField tfImporte;
    @FXML private TextArea taComentarios;
    @FXML private CheckBox chkAddMore;
    @FXML private Label lblError;

    private GestorPresupuesto gestor;
    private Runnable onSaveCallback;
    private Account cuentaSeleccionada;
    private String tipoSeleccionado = "Ingreso";  // Por defecto
    private Transaccion transaccionEditar; // Si es null, es modo crear

    /**
     * Inicialización automática al cargar el FXML.
     * Aquí se ponen valores por defecto y estilos iniciales.
     */
    @FXML
    private void initialize() {
        // Fecha actual por defecto
        dpFecha.setValue(LocalDate.now());

        // Estilos iniciales de los botones de tipo
        btnIngreso.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 8;");
        btnGasto.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 8;");

        // Ocultar categorías al inicio (solo para gasto)
        categoriaBox.setVisible(false);
        categoriaBox.setManaged(false);  // Evita que reserve espacio cuando oculto
    }

    public void setGestor(GestorPresupuesto gestor) {
        this.gestor = gestor;

        // Cargar cuentas como chips
        cuentasFlow.getChildren().clear();
        for (Account acc : gestor.getAccounts()) {
            Button chip = new Button(acc.getIcono() + " " + acc.getNombre());
            chip.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
            chip.setOnAction(e -> {
                cuentaSeleccionada = acc;
                cuentasFlow.getChildren().forEach(node -> node.setStyle("-fx-background-color: #333;"));
                chip.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
            });
            cuentasFlow.getChildren().add(chip);
        }

        // Cargar categorías
        cbCategoria.setItems(FXCollections.observableArrayList(gestor.getCategorias()));
        cbCategoria.getSelectionModel().selectFirst();
    }

    public void setTransaccionEditar(Transaccion t) {
        this.transaccionEditar = t;
        chkAddMore.setVisible(false); // No permitir "añadir más" en edición

        // Rellenar datos
        dpFecha.setValue(t.getFecha());
        tfImporte.setText(t.getMonto().toPlainString());
        taComentarios.setText(t.getDescripcion());

        // Seleccionar cuenta visualmente
        this.cuentaSeleccionada = t.getAccount();
        for (javafx.scene.Node node : cuentasFlow.getChildren()) {
            if (node instanceof Button btn) {
                btn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
                if (cuentaSeleccionada != null && btn.getText().contains(cuentaSeleccionada.getNombre())) {
                    btn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16;");
                }
            }
        }

        // Configurar tipo y categoría
        if (t.esIngreso()) {
            onSelectIngreso();
        } else if (t instanceof Gasto g) {
            onSelectGasto();
            cbCategoria.getSelectionModel().select(g.getCategoria());
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onSelectIngreso() {
        tipoSeleccionado = "Ingreso";
        btnIngreso.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 8;");
        btnGasto.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 8;");
        categoriaBox.setVisible(false);
        categoriaBox.setManaged(false);
    }

    @FXML
    private void onSelectGasto() {
        tipoSeleccionado = "Gasto";
        btnGasto.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 8;");
        btnIngreso.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 8;");
        categoriaBox.setVisible(true);
        categoriaBox.setManaged(true);
    }

    @FXML
    private void onGuardar() {
        lblError.setText("");

        if (cuentaSeleccionada == null) {
            lblError.setText("Selecciona una cuenta.");
            return;
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(tfImporte.getText().trim());
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                lblError.setText("El importe debe ser positivo.");
                return;
            }
        } catch (NumberFormatException e) {
            lblError.setText("Importe inválido (usa punto decimal).");
            return;
        }

        Categoria categoria = null;
        if ("Gasto".equals(tipoSeleccionado)) {
            categoria = cbCategoria.getValue();
            if (categoria == null) {
                lblError.setText("Selecciona una categoría para gasto.");
                return;
            }
        }

        long id = (transaccionEditar != null) ? transaccionEditar.getId() : gestor.getTransacciones().size() + 1;
        Transaccion transaccion;

        if ("Ingreso".equals(tipoSeleccionado)) {
            transaccion = new Ingreso(id, dpFecha.getValue(), monto, taComentarios.getText().trim(), cuentaSeleccionada);
            gestor.agregarIngreso((Ingreso) transaccion);
        } else {
            transaccion = new Gasto(id, dpFecha.getValue(), monto, taComentarios.getText().trim(), categoria, cuentaSeleccionada);
            // Validar solo si es nuevo o si el monto ha aumentado (lógica simplificada)
            if (transaccionEditar == null) {
                String error = gestor.validarAgregarGasto((Gasto) transaccion);
                if (error != null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Atención");
                    alert.setHeaderText(null);
                    alert.setContentText(error + "\n\n¿Registrar el gasto de todos modos?");
                    alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                        return;
                    }
                }
            }
        }

        if (transaccionEditar != null) {
            // Validar edición si es un gasto
            if (transaccion instanceof Gasto) {
                String error = gestor.validarModificacionGasto((Gasto) transaccion, transaccionEditar);
                if (error != null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Atención");
                    alert.setHeaderText(null);
                    alert.setContentText(error + "\n\n¿Actualizar el gasto de todos modos?");
                    alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) return;
                }
            }
            gestor.actualizarTransaccion(transaccionEditar, transaccion);
        } else if (transaccion instanceof Gasto) {
            gestor.confirmarAgregarGasto((Gasto) transaccion);
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        if (!chkAddMore.isSelected()) {
            ((Stage) tfImporte.getScene().getWindow()).close();
        } else {
            limpiarFormulario();
        }
    }

    private void limpiarFormulario() {
        tfImporte.clear();
        taComentarios.clear();
        dpFecha.setValue(LocalDate.now());
        onSelectIngreso();  // Vuelve a seleccionar Ingreso por defecto
        cbCategoria.getSelectionModel().selectFirst();
    }

    @FXML
    private void onCancelar() {
        ((Stage) tfImporte.getScene().getWindow()).close();
    }

    @FXML
    private void onAddAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-account.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tfImporte.getScene().getWindow()); // Vincula al owner para que sea modal
            stage.setScene(new Scene(root, 450, 400));
            stage.setTitle("Añadir Cuenta");

            AddAccountController controller = loader.getController();
            controller.setGestor(gestor);

            // Callback para refrescar las cuentas en esta ventana después de añadir
            controller.setOnSaveCallback(() -> {
                // Recargar la lista de cuentas (chips)
                cuentasFlow.getChildren().clear();
                for (Account acc : gestor.getAccounts()) {
                    Button chip = new Button(acc.getIcono() + " " + acc.getNombre());
                    chip.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 20;");
                    chip.setOnAction(e -> {
                        cuentaSeleccionada = acc;
                        cuentasFlow.getChildren().forEach(node -> node.setStyle("-fx-background-color: #333;"));
                        chip.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 20;");
                    });
                    cuentasFlow.getChildren().add(chip);
                }
                // Seleccionar automáticamente la última cuenta añadida (opcional)
                if (!gestor.getAccounts().isEmpty()) {
                    cuentaSeleccionada = gestor.getAccounts().get(gestor.getAccounts().size() - 1);
                }
            });

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de cuenta: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onAddCategoria() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-categoria.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(tfImporte.getScene().getWindow());
            stage.setScene(new Scene(loader.load(), 450, 400));
            stage.setTitle("Añadir Categoría");

            AddCategoriaController controller = loader.getController();
            controller.setGestor(gestor);

            // Callback para refrescar el ChoiceBox después de añadir
            controller.setOnSaveCallback(() -> {
                cbCategoria.setItems(FXCollections.observableArrayList(gestor.getCategorias()));
                // Seleccionar la última añadida (opcional)
                if (!gestor.getCategorias().isEmpty()) {
                    cbCategoria.getSelectionModel().select(gestor.getCategorias().size() - 1);
                }
            });

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de categoría: " + e.getMessage()).showAndWait();
        }
    }
}