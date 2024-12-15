package sk.upjs.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import sk.upjs.common.AppConfig;

import sk.upjs.client.ClientService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;


public class MainSceneController {

    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private TextField textFieldTcp;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label labelOutOf;
    @FXML
    private Label labelTotal;
    @FXML
    private Label labelCurrent;

    private ClientService clientService;
    private int TCPConnections;


    @FXML
    void initialize() throws IOException {
        textFieldTcp.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textFieldTcp.setText(newValue.replaceAll("[^\\d]", ""));// Odstráni všetky nečíselné znaky
            }
        });
        if (AppConfig.TEMP.exists()) {
            startButton.setText("Continue");
            BufferedReader br = new BufferedReader(new FileReader(AppConfig.TEMP));
            String line = br.readLine();
            if (line.startsWith("Thread Count")) {
                TCPConnections = Integer.parseInt(line.split(" ")[1]);
                textFieldTcp.setText(String.valueOf(TCPConnections));
                textFieldTcp.setDisable(true);
            }
            br.close();
        }
    }


    @FXML
    void startCopyButtonAction(ActionEvent event) throws UnknownHostException {
        int threadCount = Integer.parseInt(textFieldTcp.getText());
        clientService = new ClientService(threadCount);
        progressBar.progressProperty().bind(clientService.bindProgress());
        labelCurrent.textProperty().bind(clientService.bindProgress().multiply(100).asString("%.2f%%"));
        clientService.setOnSucceeded(workerStateEvent -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Completed");
            alert.setHeaderText(null);
            alert.setContentText("File successfully copied!");
            alert.showAndWait();
        });
        clientService.start();
        startButton.setDisable(true);
        stopButton.setDisable(false);
        textFieldTcp.setDisable(true);
        clientService.setOnSucceeded(a -> {
            stopButton.setDisable(true);
        });
    }

    @FXML
    void stopCopyButtonAction(ActionEvent event) {
        stopButton.setDisable(true);
        startButton.setDisable(false);
        clientService.cancel();
        System.out.println("Stop button pressed");


    }


    void close() {
        if (clientService == null) {
            System.exit(0);
        } else {
            clientService.cancel();
            System.out.println("Aplikácia bola ukončená");
            System.exit(0);
        }
    }

}
