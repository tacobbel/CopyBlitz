package sk.upjs.client;

import sk.upjs.common.AppConfig;
import sk.upjs.common.FileInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class FileInfoReceiver {
    private int serverPort;
    private InetAddress inetAddress;

    public FileInfoReceiver(String serverHost, int serverPort) throws UnknownHostException {
        inetAddress = InetAddress.getByName(serverHost);
        this.serverPort = serverPort;
    }

    public static FileInfo getLocalhostServerFileInfo() {
        try {
            FileInfoReceiver fir = new FileInfoReceiver(AppConfig.HOST, AppConfig.SERVER_PORT);
            return fir.getFileInfo();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public FileInfo getFileInfo() {
        FileInfo fileInfo = null;
        try(Socket socket = new Socket(inetAddress, serverPort)) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeUTF("info");
            oos.flush();
            fileInfo = (FileInfo) ois.readObject();
            oos.close();
            ois.close();
        } catch (ClassNotFoundException e) {
            System.err.println("wrong fileinfo format received");
        } catch (IOException e) {
            System.err.println("Server connection problem. Is server running?");
        }
        return fileInfo;
    }
}
