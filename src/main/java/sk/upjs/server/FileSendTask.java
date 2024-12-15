package sk.upjs.server;

import sk.upjs.common.AppConfig;
import sk.upjs.common.FileInfo;
import sk.upjs.common.FileRequest;

import java.io.*;
import java.net.Socket;

public class FileSendTask implements Runnable{

    private final File fileToSend;
    private final Socket socket;

    public FileSendTask(File fileToSend, Socket socket) throws FileNotFoundException {
        this.fileToSend = fileToSend;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream ois = null;
            ObjectOutputStream oos = null;
            try(RandomAccessFile raf = new RandomAccessFile(fileToSend, "r")) {
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());
                String command = ois.readUTF();
                if (command.equals("info")) {
                    oos.writeObject(new FileInfo(fileToSend.getName(), fileToSend.length()));
                    oos.flush();
                    return;
                }
                if (! command.equals("file")) {
                    oos.writeUTF("unknown command");
                    return;
                }
                FileRequest fileRequest = (FileRequest) ois.readObject();
                if (fileRequest.offset < 0 || fileRequest.length < 0 || fileRequest.offset + fileRequest.length > fileToSend.length()) {
                    throw new RuntimeException(socket.getInetAddress() + ":" + socket.getPort() + " : "
                            + fileRequest + " exceeds the file size " + fileToSend.length());
                }
                raf.seek(fileRequest.offset);
                byte[] buffer = new byte[AppConfig.BLOCK_SIZE];
                for (long send = 0; send < fileRequest.length; send += AppConfig.BLOCK_SIZE) {
                    if (ois.available() > 0) {
                        throw new RuntimeException(socket.getInetAddress() + ":" + socket.getPort() + " : "
                                + "Premature closing data stream after " + send + " bytes send for " + fileRequest);
                    }
                    int size = (int) Math.min(AppConfig.BLOCK_SIZE, fileRequest.length - send);
                    raf.read(buffer, 0, size);
                    oos.write(buffer, 0, size);
                }
                oos.flush();

            } finally {
                if (oos != null) oos.close();
                if (ois != null) ois.close();
                if (socket != null && socket.isConnected()) socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
