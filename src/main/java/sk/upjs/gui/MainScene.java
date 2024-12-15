package sk.upjs.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sk.upjs.common.AppConfig;

public class MainScene extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainScene.fxml"));
        MainSceneController controller = new MainSceneController();
        loader.setController(controller);

        Parent parent = loader.load();
        Scene scene = new Scene(parent);

        stage.setOnCloseRequest(event -> {
            controller.close();
        }); // close the connection when the window is closed

        stage.setScene(scene);
        stage.setTitle(AppConfig.APP_NAME);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/images.jpg")));

        stage.show();


    }

    public static void main(String[] args) {
        launch(args);
    }


}
