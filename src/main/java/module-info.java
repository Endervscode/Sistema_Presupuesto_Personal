module com.example.sistemapresupuestopersonal {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens com.example.sistemapresupuestopersonal to javafx.fxml;
    exports com.example.sistemapresupuestopersonal;
    exports com.example.sistemapresupuestopersonal.service;
    exports com.example.sistemapresupuestopersonal.model;
    exports com.example.sistemapresupuestopersonal.persistence;
}