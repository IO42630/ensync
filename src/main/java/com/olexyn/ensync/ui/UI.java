package com.olexyn.ensync.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class UI extends Application implements Runnable {





    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        Scene scene = new Scene(root, 500, 500);





        primaryStage.setTitle("EnSync");
        primaryStage.setScene(scene);
        primaryStage.show();
    }




    @Override
    public void run() {
        UI.launch();
    }
}
