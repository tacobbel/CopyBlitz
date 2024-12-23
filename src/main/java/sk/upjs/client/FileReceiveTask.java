package sk.upjs.client;

import sk.upjs.common.FileRequest;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;


public class FileReceiveTask implements Callable<String> {
    private static final int BUFFER_SIZE = 16384;
    private MyFileWriter myFileWriter;
    private long offset;
    private long length;
    private InetAddress inetAddress;
    private int serverPort;
    private ClientService clientService;
    private long dataRead = 0L;
    private String data = new String();

    public FileReceiveTask(File fileToSave, long fileSize, long offset, long length, InetAddress inetAddress, int serverPort, ClientService clientService) throws IOException {
        this.offset = offset;
        this.length = length;
        this.inetAddress = inetAddress;
        this.serverPort = serverPort;
        this.clientService = clientService;
        myFileWriter = MyFileWriter.getInstance(fileToSave, fileSize);
    }

    // Main method to execute the task and handle file transfer
    @Override
    public String call() throws Exception {
        try (Socket socket = new Socket(inetAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Notify the server of the file request
            oos.writeUTF("file");
            oos.flush();

            // Send details of the file segment to download
            FileRequest fileRequest = new FileRequest(offset, length);
            oos.writeObject(fileRequest);
            oos.flush();

            long fileOffset = offset; // Initialize file offset for writing data
            while (dataRead < length) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Task interrupted: Offset " + offset);
                    return incompleteData(); // Return details of unfinished data
                }

                // Read data from the server
                byte[] bytes = ois.readNBytes(BUFFER_SIZE);
                if (bytes.length > 0) {
                    // Write data to the file
                    myFileWriter.write(fileOffset, bytes, 0, bytes.length);
                    dataRead += bytes.length;
                    clientService.updateProgress(bytes.length); // Update client progress
                }


                if (bytes.length < BUFFER_SIZE) {
                    System.out.println("End of data reached for offset: " + offset);
                    break;
                }

                fileOffset += bytes.length;

                // Log progress after processing every 1000 chunks
                if ((fileOffset / BUFFER_SIZE) % 1000 == 0) {
                    System.out.println("Task in progress by thread: " + Thread.currentThread().getName() +
                            ", FileOffset: " + fileOffset);
                }
            }

            clientService.cdl.countDown();
            if (clientService.cdl.getCount() == 0) {
                myFileWriter.close();
            }
            return offset + " " + length; // Return details of completed segment

        } catch (EOFException | SocketException e) {
            System.out.println("Connection lost with server. Task offset: " + offset);
            return incompleteData(); // Return details of unfinished data
        } catch (IOException e) {
            System.err.println("IOException in FileReceiveTask: " + e.getMessage());
            return incompleteData(); // Return details of unfinished data
        }
    }

    private String incompleteData() {
        if (dataRead < length) {
            return (offset + dataRead) + " " + (length - dataRead); // Return remaining offset and length
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
