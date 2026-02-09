package com.example.sistemapresupuestopersonal;

import com.example.sistemapresupuestopersonal.model.Account;
import com.example.sistemapresupuestopersonal.service.GestorPresupuesto;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controlador para la ventana de añadir una nueva cuenta (inspirado en Homeasy).
 * Permite definir nombre, tipo de cuenta, saldo inicial y asigna un icono por defecto.
 */
public class AddAccountController {

    @FXML private TextField tfNombre;
    @FXML private ChoiceBox<String> cbTipo;
    @FXML private TextField tfSaldoInicial;
    @FXML private Label lblError;

    private GestorPresupuesto gestor;
    private Runnable onSaveCallback;  // Callback para actualizar el dashboard después de guardar

    /**
     * Inyecta el gestor y carga los tipos de cuenta disponibles.
     */
    public void setGestor(GestorPresupuesto gestor) {
        this.gestor = gestor;

        // Tipos de cuenta inspirados en Homeasy (bancaria, cartera/efectivo, etc.)
        cbTipo.getItems().addAll(
                "Bancaria",
                "Cartera / Efectivo",
                "Tarjeta de crédito/débito",
                "Ahorro",
                "Otro"
        );
        cbTipo.getSelectionModel().selectFirst();
    }

    /**
     * Establece el callback que se ejecutará al guardar (normalmente recargar dashboard).
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onGuardar() {
        String nombre = tfNombre.getText().trim();
        if (nombre.isEmpty()) {
            lblError.setText("El nombre de la cuenta es obligatorio.");
            return;
        }

        BigDecimal saldoInicial;
        try {
            String saldoStr = tfSaldoInicial.getText().trim();
            saldoInicial = saldoStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(saldoStr);
        } catch (NumberFormatException e) {
            lblError.setText("Saldo inicial inválido. Usa números con punto decimal.");
            return;
        }

        String tipo = cbTipo.getValue();
        if (tipo == null) {
            lblError.setText("Selecciona un tipo de cuenta.");
            return;
        }

        // Icono por defecto según tipo
        String icono = switch (tipo.toLowerCase()) {
            case "bancaria" -> "🏦";
            case "cartera / efectivo" -> "💵";
            case "tarjeta de crédito/débito" -> "💳";
            case "ahorro" -> "🐷";
            default -> "📁";
        };

        // Crear y agregar la cuenta
        String id = UUID.randomUUID().toString();
        Account nuevaCuenta = new Account(id,nombre, tipo, saldoInicial, icono);
        gestor.agregarAccount(nuevaCuenta);

        // Notificar al dashboard para refrescar
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        // Cerrar ventana
        ((Stage) tfNombre.getScene().getWindow()).close();
    }

    @FXML
    private void onCancelar() {
        ((Stage) tfNombre.getScene().getWindow()).close();
    }
}