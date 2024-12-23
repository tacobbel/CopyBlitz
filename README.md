#### Fast like Blitzcrank's hook ⚡

This project is a Java-based client-server application developed as part of a university course on Concurrent Programming. It is designed to copy files over a network using TCP sockets. The application utilizes parallel threads to optimize file transfer and supports resuming interrupted downloads. The client interface is built with JavaFX, providing a graphical interface for users to interact with the application.

### Requirements
* Java 17 or higher
* JavaFX (for the client graphical interface)

## How to run
1. Server
   * Configure constants in 'ApConfig.java' to configure shared file, destination folder and other server settings. 
   * Run the server application by executing the `Server` class.
2. Client
   * Run the `Starter` class to start the client.
   * Use the graphical interface to specify the number of threads and initiate or resume file downloading.
3. File Integrity Check
   * After the transfer is complete, click the "Check Integrity" button on the client interface to verify the downloaded file.

### Limitations
* The application is designed for use on a local network or localhost.
* Resuming downloads assumes the temporary file is not deleted or modified.