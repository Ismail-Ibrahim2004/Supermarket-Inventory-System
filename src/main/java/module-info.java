module com.example.demo1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.sql;
    requires java.desktop;



    opens com.example.demo1 to javafx.fxml;
    exports com.example.demo1;
    exports com.example.demo1.database;
    exports com.example.demo1.models;
    opens com.example.demo1.models to javafx.fxml;
}