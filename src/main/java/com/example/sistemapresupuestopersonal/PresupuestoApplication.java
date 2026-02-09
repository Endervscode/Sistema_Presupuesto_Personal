package com.example.sistemapresupuestopersonal;

import com.example.sistemapresupuestopersonal.model.Usuario;
import com.example.sistemapresupuestopersonal.persistence.AlmacenDatosTxt;
import com.example.sistemapresupuestopersonal.service.GestorPresupuesto;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PresupuestoApplication extends Application {

    private GestorPresupuesto gestor;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Inicializar gestor (lo compartiremos con login y main)
        Usuario userDefault = new Usuario(1L, "admin", "admin@ejemplo.com");
        AlmacenDatosTxt almacen = new AlmacenDatosTxt();
        gestor = new GestorPresupuesto(userDefault, almacen);

        // Abrir ventana de login
        FXMLLoader loginLoader = new FXMLLoader(PresupuestoApplication.class.getResource("/login.fxml"));
        Scene loginScene = new Scene(loginLoader.load(), 1160, 760);
        Stage loginStage = new Stage();
        loginStage.setTitle("Login - Gestor de Presupuesto");
        loginStage.setScene(loginScene);

        // Obtener el controller de login y pasarle el gestor y stage principal
        LoginController loginController = loginLoader.getController();
        loginController.setGestor(gestor);
        loginController.setMainStage(primaryStage);  // Para abrir main después

        loginStage.show();
    }
}