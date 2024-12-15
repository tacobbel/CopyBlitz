
        package sk.upjs.client;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import sk.upjs.client.FileInfoReceiver;
import sk.upjs.client.FileReceiveTask;
import sk.upjs.common.AppConfig;
import sk.upjs.common.FileInfo;
import sk.upjs.common.FileRequest;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ClientService extends Service<Void> {

    private final int threadCount;

    private final List<Future<String>> futures = new ArrayList<>();
    private AtomicLong progress = new AtomicLong(0);
    private long fileSize;
    private File newFile;
    private ExecutorService executor;
    public CountDownLatch cdl;
    private final DoubleProperty progressProperty = new SimpleDoubleProperty(0);
    private FileInfo fileInfo;

    public ClientService(int threadCount) {
        this.threadCount = threadCount;
        cdl= new CountDownLatch(threadCount);
        this.executor = Executors.newFixedThreadPool(threadCount);
        fileInfo = FileInfoReceiver.getLocalhostServerFileInfo();
        if (fileInfo == null) {

            return;
        }

        newFile = new File(AppConfig.COPY_TO + File.separator + AppConfig.FILE_TO_COPY.getName());
        fileSize = fileInfo.getFileSize();
    }

    public void startCopy() {

        try {
            long blockSize = fileSize / threadCount;

            for (int i = 0; i < threadCount; i++) {
                long offset = i * blockSize;
                long length = (i == threadCount - 1) ? (fileSize - offset) : blockSize;

                FileReceiveTask task = new FileReceiveTask(newFile, fileSize, offset, length, InetAddress.getByName("localhost"), AppConfig.SERVER_PORT, this);
                Future<String> future = executor.submit(task);
                futures.add(future);
                System.out.println(task);
            }


            cdl.await();


        }  catch (InterruptedException e){


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processFutures() {

        // TODO if temp
        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null) {
                    saveDataToFile(result);
                    System.out.println("Future result: " + result);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();

            }

        }
        if(progress.get()==fileSize){

            System.out.println("Celý súbor bol poslaný");


        }else{

            saveDataToFile("priebeh" + " " + progress.toString());
        }
        if (!executor.isShutdown()) {
            executor.shutdown();
        }



    }

    private void downloadContinue() {
        executor = Executors.newFixedThreadPool(threadCount);
        BufferedReader br;
        FileReader fr;
        try {
            fr = new FileReader(AppConfig.TEMP);
            br = new BufferedReader(fr);
            String info = br.readLine();
            FileReceiveTask task;
            while (info != null) {
                if (info.contains("priebeh")) {
                    progress.set(Long.parseLong(info.split(" ")[1]));

                } else {
                    if (!info.contains("vlakna")) {
                        String[] splitted = info.split(" ");
                        task = new FileReceiveTask(newFile, fileSize, Long.parseLong(splitted[0]),
                                Long.parseLong(splitted[1]), InetAddress.getByName("localhost"), AppConfig.SERVER_PORT, this);
                        futures.add(executor.submit(task));
                    }
                }
                info = br.readLine();
            }
            br.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void saveDataToFile(String returnedFuture) {
        if (!returnedFuture.isEmpty()) {
            try (FileWriter fw = new FileWriter(AppConfig.TEMP, true)) {
                if (AppConfig.TEMP.length() == 0 || !AppConfig.TEMP.isFile()) {
                    fw.write("vlakna " + threadCount + "\n");
                }
                fw.write(returnedFuture + " " + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected Task<Void> createTask() {

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {
                try {
                    executor = Executors.newFixedThreadPool(threadCount);
                    if( AppConfig.TEMP.length() == 0){
                        startCopy();
                        cdl.await();
                        progress.set(fileSize);


                        if (progress.get() == fileSize) {

                            System.out.println("Stahovanie dokoncene ");
                            AppConfig.TEMP.deleteOnExit();
                        }
                    }else{
                        downloadContinue();
                        cdl.await();
                        progress.set(fileSize);
                        executor.shutdown();

                        if (progress.get() == fileSize) {

                            System.out.println("Stahovanie dokoncene ");
                            AppConfig.TEMP.deleteOnExit();
                        }
                    }
                } catch (InterruptedException e ) {

                }
                return null;
            }

            @Override
            public void cancelled() {
                System.out.println(futures);
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                    System.out.println("Stahovanie je prerusene");
                    processFutures();

                }
                for (Future<String> future : futures) {
                    if (!future.isDone() && !future.isCancelled()) {
                        future.cancel(true);
                        System.out.println("Cancelled future: " + future);
                    }
                }

            }
        };
        return task;
    }
    public void updateProgress ( long data) {

            long newProgress = progress.getAndAdd(data);
            if (newProgress > fileSize) {
                progress.set(fileSize); // Zabráň prekročeniu veľkosti
            }

            double currentProgress = Math.min(1.0, (double) progress.get() / fileSize);
            Platform.runLater(() -> progressProperty.set(currentProgress));

            System.out.printf("Progress: %d / %d (%.2f%%)%n", progress.get(), fileSize, currentProgress * 100);

    }

        public DoubleProperty bindProgress() {
        return progressProperty;
    }



    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1 && dotIndex != 0) ? fileName.substring(dotIndex) : "";
    }
    public void addData(long data) {
        progress.getAndAdd(data);

    }
}