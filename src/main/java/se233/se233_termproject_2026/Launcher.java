package se233.se233_termproject_2026;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {
    public static Stage primaryStage;
    public static HostServices hs;
    @Override
    public void start(Stage stage) throws Exception{
        primaryStage=stage;
        hs=getHostServices();
        //hostService to open files on OS
        FXMLLoader fxmlLoader=new FXMLLoader(Launcher.class.getResource("drop-view.fxml"));
        Scene scene =new Scene(fxmlLoader.load());
        primaryStage.setTitle("Vector Magic Clone");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    static void main(String[] args) {
        launch( args);
    }
}
