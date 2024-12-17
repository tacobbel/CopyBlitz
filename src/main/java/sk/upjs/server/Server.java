package sk.upjs.server;

import sk.upjs.common.AppConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public static void main(String[] args) throws IOException {

        ExecutorService executor = Executors.newCachedThreadPool();
        if (!AppConfig.FILE_TO_COPY.exists() || !AppConfig.FILE_TO_COPY.isFile()) {
            throw new FileNotFoundException("No such file: " + AppConfig.FILE_TO_COPY);
        }
        RandomAccessFile raf = new RandomAccessFile(AppConfig.FILE_TO_COPY, "r");
        raf.close();
        try (ServerSocket ss = new ServerSocket(AppConfig.SERVER_PORT)) {
            System.out.println("Sharing file " + AppConfig.FILE_TO_COPY + " with size " + (AppConfig.FILE_TO_COPY.length() / 1_000_000) + " MB");
            System.out.println("Server is running on port " + AppConfig.SERVER_PORT + " ...");

            while (true) {
                try {
                    Socket socket = ss.accept();
                    FileSendTask fileSendTask = new FileSendTask(AppConfig.FILE_TO_COPY, socket);
                    executor.submit(fileSendTask);
                } catch (IOException e) {
                    System.out.println("Server stopped or exception occurred: " + e.getMessage());
                    break;
                }
            }
        }
    }
}
