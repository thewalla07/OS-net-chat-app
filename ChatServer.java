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

            String inputLine, outputLine, username;

            // Attach a printer to the socket's output stream
            PrintWriter socketOut =
                new PrintWriter(socket.getOutputStream(), true);

            // Attach a reader to the socket's input stream
            BufferedReader in = 
                new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            // Print connection message for user joining server
            username = in.readLine();
            System.out.println(username + " has joined the server");

            // Send data to the client
            while ((inputLine = in.readLine()) != null) {

                System.out.println(inputLine);
            }

            // Close things that were opened
            socketOut.close();
            socket.close();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    private void handleIncoming (String inputLine) {

    }
}

public class ChatServer implements Runnable {
    private ServerThread[] clients;
    private int numClients;
    private ServerSocket serverSocket;
    private int maxUsers = 10;

    public ChatServer() {
        serverSocket = null;
        numClients = 0;
        clients = new ServerThread [maxUsers];
        new Thread(this).start();
    }

    public void run() {

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
            try {
                clients[numClients] = new ServerThread(serverSocket.accept());
                new Thread(clients[numClients]).start();
                numClients++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String [] args) throws IOException {
        System.out.println("Hello from the ChatServer");
        new ChatServer();
    }
}