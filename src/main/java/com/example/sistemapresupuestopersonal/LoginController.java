package com.example.sistemapresupuestopersonal;

import com.example.sistemapresupuestopersonal.service.GestorPresupuesto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML private TextField tfUsuario;
    @FXML private PasswordField pfContrasena;
    @FXML private Label lblError;

    private GestorPresupuesto gestor;
    private Stage mainStage;

    public void setGestor(GestorPresupuesto gestor) {
        this.gestor = gestor;
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    @FXML
    private void onLogin() {
        String usuario = tfUsuario.getText().trim();
        String contrasena = pfContrasena.getText().trim();

        if (!"admin".equals(usuario) || !"1234".equals(contrasena)) {
            lblError.setText("Usuario o contraseña incorrectos.");
            return;
        }

        // Abrir ventana principal
        try {
            URL url = PresupuestoApplication.class.getResource("/main-view.fxml");
            if (url == null) {
                throw new IOException("FXML no encontrado: /main-view.fxml");
            }

            FXMLLoader mainLoader = new FXMLLoader(url);
            Parent root = mainLoader.load();

            MainController mainController = mainLoader.getController();
            mainController.setGestor(gestor);

            Scene mainScene = new Scene(root, 1160, 759);
            mainStage.setScene(mainScene);
            mainStage.setTitle("Sistema de Gestión de Presupuesto Personal");
            mainStage.show();

            ((Stage) tfUsuario.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
            lblError.setText("Error al cargar ventana principal: " + e.getMessage());
        }
    }
}