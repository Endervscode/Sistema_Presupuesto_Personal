package com.example.sistemapresupuestopersonal;

import com.example.sistemapresupuestopersonal.model.Categoria;
import com.example.sistemapresupuestopersonal.service.GestorPresupuesto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.UUID;

public class AddCategoriaController {

    @FXML private TextField tfNombre;
    @FXML private TextField tfLimite;
    @FXML private TextArea taDescripcion;
    @FXML private Label lblError;
    private ComboBox<String> cbIcono; // 4. ICONOS
    private Categoria categoriaEditar;

    private GestorPresupuesto gestor;
    private Runnable onSaveCallback;  // Para refrescar el ChoiceBox o lista después

    @FXML
    public void initialize() {
        // Inyectar selector de iconos dinámicamente ya que no podemos editar el FXML
        cbIcono = new ComboBox<>();
        cbIcono.getItems().addAll("🍔", "🏠", "🚗", "💊", "🎉", "💡", "🛒", "✈️", "🎓", "💻");
        cbIcono.getSelectionModel().selectFirst();
        
        if (tfNombre != null && tfNombre.getParent() instanceof VBox) {
            VBox parent = (VBox) tfNombre.getParent();
            parent.getChildren().add(1, cbIcono); // Insertar debajo del título o campo
        }
    }

    public void setGestor(GestorPresupuesto gestor) {
        this.gestor = gestor;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setCategoriaEditar(Categoria c) {
        this.categoriaEditar = c;
        String nombre = c.getNombre();
        
        // Intentar separar el icono del nombre si existe
        if (cbIcono != null) {
            for (String icon : cbIcono.getItems()) {
                if (nombre.startsWith(icon)) {
                    cbIcono.setValue(icon);
                    nombre = nombre.substring(icon.length()).trim();
                    break;
                }
            }
        }
        tfNombre.setText(nombre);
        tfLimite.setText(c.getLimitePresupuesto().toPlainString());
        taDescripcion.setText(c.getDescripcion());
    }

    @FXML
    private void onGuardar() {
        // Guardar icono junto con el nombre
        String nombre = (cbIcono != null ? cbIcono.getValue() + " " : "") + tfNombre.getText().trim();
        if (nombre.isEmpty()) {
            lblError.setText("El nombre de la categoría es obligatorio.");
            return;
        }

        BigDecimal limite;
        try {
            String limiteStr = tfLimite.getText().trim();
            limite = limiteStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(limiteStr);
        } catch (NumberFormatException e) {
            lblError.setText("Límite inválido. Usa números con punto decimal.");
            return;
        }

        String descripcion = taDescripcion.getText().trim();

        // Crear y agregar la categoría
        String id = (categoriaEditar != null) ? categoriaEditar.getId() : UUID.randomUUID().toString();
        Categoria nueva = new Categoria(id, nombre, limite, descripcion);
        
        if (categoriaEditar != null) {
            gestor.actualizarCategoria(nueva);
        } else {
            gestor.agregarCategoria(nueva);
        }

        // Refrescar (si hay callback)
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