package com.example.demo1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // إضافة أيقونة البرنامج (تأكد من وجود صورة باسم app_icon.png في مجلد resources)
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/demo1/images/app_icon.png")));
        } catch (Exception e) {
            System.out.println("Icon not found.");
        }

        stage.setTitle("Supermarket Manager");
        stage.setScene(scene);

        // جعل الشاشة كاملة عند التشغيل


        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}