import java.net.*;
import java.io.*;

class ServerThread implements Runnable {

    // socket is to be passed in by the creator
    private Socket socket = null;
    private int userID;

    public ServerThread(int uID0, Socket s0) {

        this.socket = s0;
        this.userID = uID0;

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

        } catch (SocketException e) {

            System.out.println("Client disconnected");
        
        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    public int getID() {

        return userID;

    }

    private void handleIncoming(String inputLine) {

    }
}

public class ChatServer implements Runnable {
    private ServerThread[] clients;
    private int numClients;
    private ServerSocket serverSocket;
    private int maxUsers = 10;

    // Keeps track of clients so id can be assigned
    private int totalUsers; 

    public ChatServer() {
        serverSocket = null;
        numClients, totalUsers = 0;
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
            
            // Always add a new ServerThread
            addServerThread();

        }
    }

    private synchronized void addServerThread() {

        try {

            // Accept a connection (return a new socket)
            // and create a new ServerThread adding it to the
            // clients array
            clients[numClients] = new ServerThread(totalUsers, serverSocket.accept());
            
            // Create a new Thread from the ServerThread
            // Call start on the new thread
            new Thread(clients[numClients]).start();

            // Increment the number of clients connected
            numClients++;
            totalUsers++;
            System.out.println("Clients: " + numClients);


        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public static void main(String [] args) throws IOException {
        System.out.println("Hello from the ChatServer");
        new ChatServer();
    }
}