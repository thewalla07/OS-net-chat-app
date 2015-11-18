import java.net.*;
import java.io.*;

class ServerThread implements Runnable {

    // socket is to be passed in by the creator
    private Socket socket = null;

    public ServerThread(Socket s0) {

        this.socket = s0;

    }

    // Handles the connection
    public void run() {

        try {

            // Attach a printer to the socket's output stream
            PrintWriter socketOut =
                new PrintWriter(socket.getOutputStream(), true);

            // Send data to the client
            socketOut.println("Hello to client");

            // Close things that were opened
            socketOut.close();
            socket.close();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
}

public class ChatServer{
    public static void main(String [] args) throws IOException {
        System.out.println("Hello from the ChatServer");
        ServerSocket serverSocket = null;

        try {
            
            // Listen on port 7777 by default
            serverSocket = new ServerSocket(7777);

        } catch (IOException e) {

            System.err.println("Could not listen on port: 7777");
            System.exit(-1);

        }

        while (true) {

            /*
             * The following line of code does several things:
             * 1. Accept a connection (returns a new socket)
             * 2. Create a new ServerThread 
             * 3. Create a new Thread from ServerThread
             * 4. Call start on the new thread  
             */
            new Thread(new ServerThread(serverSocket.accept())).start();

        }
    }
}