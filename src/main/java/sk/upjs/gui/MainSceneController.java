package sk.upjs.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import sk.upjs.common.AppConfig;
import sk.upjs.client.ClientService;
import sk.upjs.common.IntegrityChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;


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
    @FXML
    private Button checkIntegrityButton;


    private ClientService clientService;
    private int TCPConnections;


    @FXML
    void initialize() throws IOException {
        stopButton.setDisable(true);
        checkIntegrityButton.setDisable(true);

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
                TCPConnections = Integer.parseInt(line.split(" ")[2]);
                textFieldTcp.setText(String.valueOf(TCPConnections));
                textFieldTcp.setDisable(true);
            }
            br.close();
        } else {
            checkFileExistence();
        }

        progressBar.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Done");
                    alert.setHeaderText(null);
                    alert.setContentText("Selected file was copied to the destination!");
                    alert.showAndWait();
                });
            }
        });
    }


    @FXML
    void startCopyButtonAction(ActionEvent event) throws UnknownHostException {
        int threadCount;
        if (textFieldTcp.getText().isEmpty()) {
            threadCount = 1;
            textFieldTcp.setText("1");
            System.out.println("No thread count specified, using default value 1");
        } else {
            threadCount = Integer.parseInt(textFieldTcp.getText());
        }
        clientService = new ClientService(threadCount);
        progressBar.progressProperty().bind(clientService.bindProgress());
        labelCurrent.textProperty().bind(clientService.bindProgress().multiply(100).asString("%.2f%%"));
        clientService.setOnSucceeded(workerStateEvent -> {
            checkIntegrityButton.setDisable(false);
            stopButton.setDisable(true);
        });
        clientService.start();
        startButton.setDisable(true);
        stopButton.setDisable(false);
        textFieldTcp.setDisable(true);
    }

    @FXML

        void checkIntegrityButtonAction(ActionEvent event) {
            ProgressIndicator progressIndicator = new ProgressIndicator();
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("Checking Integrity");
            progressAlert.setHeaderText("Please wait...");
            progressAlert.setGraphic(progressIndicator);
            progressAlert.getDialogPane().setDisable(true);
            progressAlert.show();

            // new task for integrity check
            Task<Boolean> integrityCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return IntegrityChecker.compareFiles(
                            AppConfig.FILE_TO_COPY,
                            new File(AppConfig.COPY_TO + File.separator + AppConfig.FILE_TO_COPY.getName())
                    );
                }
            };

            // when task is done
            integrityCheckTask.setOnSucceeded(workerStateEvent -> {
                progressAlert.close();
                boolean result = integrityCheckTask.getValue();

                Alert resultAlert = new Alert(result ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                resultAlert.setTitle(result ? "Completed" : "Error");
                resultAlert.setHeaderText(null);
                resultAlert.setContentText(result ? "File successfully copied!" : "File corrupted during transfer!");
                resultAlert.showAndWait();
            });

            // when task fails
            integrityCheckTask.setOnFailed(workerStateEvent -> {
                progressAlert.close();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Integrity check failed");
                errorAlert.setContentText("An error occurred while comparing files.");
                errorAlert.showAndWait();
            });

            // Spustenie Task v novom vlákne
            new Thread(integrityCheckTask).start();

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

    void checkFileExistence() {
        File newFile = new File(AppConfig.COPY_TO + File.separator + AppConfig.FILE_TO_COPY.getName());

        if (newFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("File already exists");
            alert.setHeaderText("File " + AppConfig.FILE_TO_COPY.getName() + " already exists in the destination folder.");
            alert.setContentText("Would you like to overwrite it?");

            ButtonType overwriteButton = new ButtonType("Overwrite", ButtonBar.ButtonData.YES);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(overwriteButton, cancelButton);

            alert.showAndWait().ifPresent(response -> {
                if (response == cancelButton) {
                    System.out.println("Shutting down Application...");
                    System.exit(0); // Ukončí aplikáciu
                } else if (response == overwriteButton) {
                    System.out.println("Overwriting existing file...");
//                    newFile.delete(); // Odstráni existujúci súbor
                }
            });
        }
    }

//    @FXML
//    void selectFileButtonAction(ActionEvent event) {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.setTitle("Select File to Copy");
//
//        // Nastavenie počiatočného priečinka, voliteľné
//        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
//
//        // Filtrovanie súborov, voliteľné
//        fileChooser.getExtensionFilters().addAll(
//                new FileChooser.ExtensionFilter("All Files", "*.*"),
//                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
//                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png")
//        );
//
//        Stage stage = (Stage) selectFileButton.getScene().getWindow();
//        File selectedFile = fileChooser.showOpenDialog(stage);
//
//        if (selectedFile != null) {
//            // Nastavíme FILE_TO_COPY v AppConfig na vybraný súbor
//
//            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
//
//            // Aktualizujeme label alebo textField, ak chcete zobraziť vybraný súbor
//            filePathTextField.setText(selectedFile.getName());
//            startServer();
//        } else {
//            System.out.println("File selection cancelled.");
//        }
//
//    }

//    private void startServer() {
//        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
//        serverExecutor.submit(() -> {
//            try {
//                new Server().start();
//            } catch (IOException e) {
//                System.err.println("Error while starting server: " + e.getMessage());
//            }
//        });
//    }

}
