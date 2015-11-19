import java.net.*;
import java.io.*;

class ServerThread implements Runnable {

    // socket is to be passed in by the creator
    private Socket socket = null;
    private int userID;
    public ClientsArray clients;
    public MessageBuffer messages;
    private PrintWriter socketOut;


    public ServerThread(MessageBuffer m0, ClientsArray c0, Socket s0, int uID0) {

        this.socket = s0;
        this.userID = uID0;
        this.clients = c0;
        this.messages = m0;

    }

    // Handles the connection
    public void run() {

        try {

            String inputLine, outputLine, username;

            // Attach a printer to the socket's output stream
            socketOut =
                new PrintWriter(socket.getOutputStream(), true);

            // Attach a reader to the socket's input stream
            BufferedReader in = 
                new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            // Print connection message for user joining server
            username = in.readLine();
            messages.addMessage(username + " just joined the chatroom...");
            System.out.println(username + " just joined the chatroom...");

            // Send data to the client
            while ((inputLine = in.readLine()) != null) {
                messages.addMessage(username + " says: " + inputLine);
                System.out.println(username + " says: " + inputLine);
            }

            // Close things that were opened
            socketOut.close();
            socket.close();

        } catch (SocketException e) {

            System.out.println("Client disconnected");
            clients.removeServerThread(userID);
        
        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    public int getID() {

        return userID;

    }

    public void sendMessage(String s) {
        socketOut.println(s);
    }
}

public class ChatServer implements Runnable {
    private ServerSocket serverSocket;
    private ClientsArray clients;
    // Keeps track of clients so id can be assigned
    private int totalUsers; 
    private MessageBuffer messages;
    private MessageDistributor distributor;

    public ChatServer() {
        serverSocket = null;
        clients = new ClientsArray();
        messages = new MessageBuffer();
        distributor = new MessageDistributor(clients, messages);
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
            try {
                clients.addServerThread(messages, clients, serverSocket.accept());
            } catch (IOException e) {

            }
        }
    }

    public static void main(String [] args) throws IOException {
        System.out.println("Hello from the ChatServer");
        new ChatServer();
    }
}

class MessageDistributor implements Runnable {
    private ClientsArray clients;
    private MessageBuffer messages;

    public MessageDistributor(ClientsArray c0, MessageBuffer m0) {

        this.clients = c0;
        this.messages = m0;
        new Thread(this).start();
    }

    public void run() {

        while (true) {
            String out = messages.removeMessage();
            for (int i = 0; i < clients.getSize(); i++) {
                clients.getServerThread(i).sendMessage(out);
            }
        }
    }



}

class ClientsArray {
    private ServerThread[] arr;
    private int numClients, maxClients, totalUsers;

    public ClientsArray(){
        maxClients = 10;
        totalUsers = 0;
        arr = new ServerThread[maxClients];

    }

    public synchronized int getSize() {
        
        return numClients;

    }

    public synchronized void addServerThread(MessageBuffer m, ClientsArray c, Socket s) {

        // Accept a connection (return a new socket)
        // and create a new ServerThread adding it to the
        // clients array
        arr[numClients] = 
            new ServerThread(m, c, s, totalUsers);
        
        // Create a new Thread from the ServerThread
        // Call start on the new thread
        new Thread(arr[numClients]).start();

        // Increment the number of clients connected
        numClients++;
        totalUsers++;
        System.out.println("Clients: " + numClients);

    }

    public synchronized void removeServerThread(int userID) {

        for (int i = 0; i < numClients; i++) {
            if (arr[i].getID() == userID) {
                for (int j = i; j < numClients - 1; j++) {
                    arr[j] = arr[j+1];
                }
                numClients--;
            }
        }
        System.out.println("Clients: " + numClients);
    }

    public synchronized ServerThread getServerThread(int i) {

        return arr[i];

    }
}

class MessageBuffer {
    private String[] queue;
    private int nextIn, nextOut, amountOccupied, maxMessages;

    public MessageBuffer() {
        maxMessages = 10;
        queue = new String[maxMessages];
        nextIn = 0; 
        nextOut = 0;
        amountOccupied = 0;
    }

    public synchronized void addMessage(String s) {
        try {

            while (amountOccupied == maxMessages) {
                wait();
            }

            queue[nextIn] = s;
            nextIn += nextIn % maxMessages;
            amountOccupied++;

            notifyAll();

        } catch (InterruptedException e) {

            System.out.println("Storing message: " + s + " failed");
        }
    }

    public synchronized String removeMessage() {
        try {

            while (amountOccupied == 0) {
                wait();
            }
            String out = queue[nextOut];
            nextOut += nextOut % maxMessages;
            amountOccupied--;

            notifyAll();
            return out;
        } catch (InterruptedException e) {

            System.out.println("Retrieving message: failed");
            return null;
        }
    }

}