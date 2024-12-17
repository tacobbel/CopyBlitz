package sk.upjs.client;

import sk.upjs.common.FileRequest;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;


public class FileReceiveTask implements Callable<String>{
    private static final int BUFFER_SIZE = 16384;
    private MyFileWriter myFileWriter;
    private long offset;
    private long length; // length of data to be received
    private InetAddress inetAddress;
    private int serverPort;
    private ClientService clientService;
    private long dataRead = 0L;
    private String data = new String();


    public FileReceiveTask(File fileToSave, long fileSize, long offset, long length, InetAddress inetAddress, int serverPort,ClientService clientService) throws IOException {
        this.offset = offset;
        this.length = length;
        this.inetAddress = inetAddress;
        this.serverPort = serverPort;
        this.clientService = clientService;
        myFileWriter = MyFileWriter.getInstance(fileToSave, fileSize);
    }

    @Override
    public String call() throws Exception {
        try (Socket socket = new Socket(inetAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Send file request to server
            oos.writeUTF("file");
            oos.flush();
            FileRequest fileRequest = new FileRequest(offset, length);
            oos.writeObject(fileRequest);
            oos.flush();

            long fileOffset = offset;
            while (dataRead < length) {
                // Check if task was interrupted
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Task interrupted: Offset " + offset);
                    return incompleteData();
                }

                byte[] bytes = ois.readNBytes(BUFFER_SIZE);
                if (bytes.length > 0) {
                    myFileWriter.write(fileOffset, bytes, 0, bytes.length);
                    dataRead += bytes.length;
                    clientService.updateProgress(bytes.length);
                }

                if (bytes.length < BUFFER_SIZE) {
                    System.out.println("End of data reached for offset: " + offset);
                    break;
                }

                fileOffset += bytes.length;

                // Debugging info
                if ((fileOffset / BUFFER_SIZE) % 1000 == 0) {
                    System.out.println("Task in progress by thread: " + Thread.currentThread().getName() +
                            ", FileOffset: " + fileOffset);
                }
            }

            clientService.cdl.countDown();
            if (clientService.cdl.getCount() == 0) {
                myFileWriter.close();
            }
            return offset + " " + length;

        } catch (EOFException | SocketException e) {
            System.out.println("Connection lost with server. Task offset: " + offset);
            return incompleteData();
        } catch (IOException e) {
            System.err.println("IOException in FileReceiveTask: " + e.getMessage());
            return incompleteData();
        }
    }

    // Get the offset and length of incomplete data
    private String incompleteData() {
        if (dataRead < length) {
            return (offset + dataRead) + " " + (length - dataRead);
        }
        return null;
    }

    @Override
    public String toString() {
        return "FileReceiveTask{" +
                "offset=" + offset +
                ", length=" + length +
                '}';
    }

}
