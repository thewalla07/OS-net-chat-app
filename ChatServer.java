import java.net.*;
import java.io.*;

public class ChatServer implements Runnable {

    private int totalUsers;
    private ServerSocket serverSocket;
    private ClientsArray clients;
    private MessageBuffer messages;
    private MessageDistributor distributor;

    public ChatServer() {

        this.totalUsers = 0;
        this.serverSocket = null;
        this.clients = new ClientsArray();
        this.messages = new MessageBuffer();
        this.distributor = new MessageDistributor(clients, messages);
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
            
            // Always try to add a new ServerThread
            try {

                clients.addServerThread(messages, clients, serverSocket.accept());
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    public static void main(String [] args) throws IOException {

        // Create a new ChatServer runnable object
        // this allows the server to be run via command line
        new ChatServer();
    }
}

class MessageBuffer {

    private int nextIn, nextOut, amountOccupied, maxMessages;
    private String[] queue;

    public MessageBuffer() {

        nextIn = 0; 
        nextOut = 0;
        amountOccupied = 0;
        maxMessages = 10;
        queue = new String[maxMessages];
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

class ClientsArray {
    
    private int numClients, maxClients, totalUsers;
    private ServerThread[] arr;

    public ClientsArray(){

        numClients = 0;
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
                break;
            }
        }
        System.out.println("Clients: " + numClients);
    }

    public synchronized ServerThread getServerThread(int i) {

        return arr[i];
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

class ServerThread implements Runnable {

    // socket is to be passed in by the creator
    private Socket socket = null;
    private int userID;
    private ClientsArray clients;
    private MessageBuffer messages;
    private PrintWriter socketOut;
    private String username, inputLine;


    public ServerThread(MessageBuffer m0, ClientsArray c0, Socket s0, int uID0) {

        this.socket = s0;
        this.userID = uID0;
        this.clients = c0;
        this.messages = m0;
    }

    // Handles the connection
    public void run() {

        try {

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

            // Send data to the client
            while ((inputLine = in.readLine()) != null) {

                messages.addMessage(username + " says: " + inputLine);
            }

            // Close things that were opened
            socketOut.close();
            socket.close();

        } catch (SocketException e) {

            messages.addMessage(username + " just left the chatroom...");
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