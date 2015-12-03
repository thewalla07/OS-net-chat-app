/*
 * Jacob O'Keeffe 13356691
 * Ryan Earley 13301871
 * Sean Quinn 13330146
 * Michael Wall 13522003
 */

import java.net.*;
import java.io.*;

// One thread per connection
class ProducerThread implements Runnable {

    // The socket passed from the creator
    private Socket socket = null;
    private MessageBuffer buffer;
    private ClientArray clients;
    private int userID;
    private String username;
    private PrintWriter socketOut = null;

    public ProducerThread(Socket s, MessageBuffer b, ClientArray c, int uID) {

        socket = s; buffer = b; clients = c; userID = uID;
    }

    // Handle the connection
    public void run() {

        try {

            // Attach a printer to the socket's output stream
            socketOut = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader socketIn = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            String input;

            // Get the user's name
            username = socketIn.readLine();
            
            buffer.add(username + " has joined the chat");

            // Get input from the user and send to server
            while ((input = socketIn.readLine()) != null) {

                buffer.add(username + " says: " + input);
            }

            // Close resources that were opened
            socketOut.close();
            socket.close();

            // Exit the chat
            exitChat();
            
        } catch (SocketException e) {
            
            exitChat();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public int getID() {

        // Simply return the user ID for this client
        return userID;
    }

    public void sendMessage(String s) {

        // Send a message to this user client
        socketOut.println(s);
    }

    private void exitChat() {

        // Add a leaving message to the message buffer
        buffer.add(username + " just left the chatroom...");
        // Remove the thread from the list of user clients
        clients.removeThread(userID);
    }
}

// One consumer thread to distribute the messages
class ConsumerThread implements Runnable {

    private MessageBuffer buffer;
    private ClientArray clients;

    public ConsumerThread(MessageBuffer b, ClientArray c) {

        buffer = b; clients = c;
    }

    public void run() {

        while (true) {
            
            // Takes a message from the buffer
            String out = buffer.remove();
            for (int i = 0; i < clients.getSize(); i++) {

                // Sends the message to each of thte connected clients
                clients.getThread(i).sendMessage(out);
            }
        }
    }
}

// One buffer to hold all the messages
class MessageBuffer {

    // Internal class node used to build queue
    private class Node {

        String message;
        Node prev, next;

        Node(String s) {

            message = s;
            prev = null; next = null;
        }
    }

    private Node head, tail;
    private int size;
    private boolean dataAvailable;

    MessageBuffer() {

        head = null; tail = null;
        size = 0;
        dataAvailable = false;
    }

    public synchronized void add(String s) {

        // If there are no messages in the buffer add to the head
        if (head == null) {

            head = new Node(s);
            tail = head;

        } else { // else add to the tail

            Node n = new Node(s);
            tail.next = n;
            n.prev = tail;
            tail = n;
        }

        size++;
        dataAvailable = true;

        // Notify any waiting methods
        notifyAll();
    }

    public synchronized String remove() {

        try {

            // Wait until there is a message available to send
            while (!dataAvailable) {wait();}

            // Take message from the head of the queue and advance the head
            String s = head.message;
            head = head.next;

            // Code to advance the head if there are no messages left
            if (head == null) { dataAvailable = false; }
            else { head.prev = null; }

            size--;

            notifyAll();

            return s;

        } catch (InterruptedException e) { return null; }
    }
}

// Array to hold all the connected clients
class ClientArray {

    private ProducerThread[] clients;
    private int occupied, total;

    public ClientArray(int maxNumClients) {

        // Create an array for the specified number of clients
        clients = new ProducerThread[maxNumClients];
    }

    public synchronized int getSize() {

        // Return the amount of current clients
        return occupied;
    }

    public synchronized void addThread(
        Socket s, MessageBuffer m, ClientArray c) {

        // Add the client to the array
        clients[occupied] =
            new ProducerThread(s, m, c, total);

        // Run the new client as a thread
        new Thread(clients[occupied]).start();

        // Increment clients and print updated server message
        occupied++; total++;
        System.out.println("Clients: " + occupied);
    }

    public synchronized void removeThread(int userID) {

        // Remove the user from the array
        for (int i = 0; i < occupied; i++) {
            
            // Find matching client ID
            if (clients[i].getID() == userID) {
                
                for (int j = i; j < occupied - 1; j++) {
                    
                    // Move the rest of the clients to the left
                    clients[j] = clients[j + 1];
                }

                // Decrement clients and print server message
                occupied--;
                System.out.println("Clients: " + occupied);
                break;
            }
        }
    }

    public synchronized ProducerThread getThread(int i) {

        // Used to returnt the requested client from the array
        return clients[i];
    }
}

public class ChatServer {

    public static void main(String[] args) {
        
        ServerSocket serverSocket = null;

        try {

            // Listen in on port 7777
            serverSocket = new ServerSocket(7777);

        } catch (IOException e) {

            System.err.println("Could not listen on port: 7777");
            System.exit(-1);
        }

        // Set up resources
        MessageBuffer buffer = new MessageBuffer();
        ClientArray clients = new ClientArray(10);
        ConsumerThread consumer = new ConsumerThread(buffer, clients);

        new Thread(consumer).start();

        // Loop forever
        while (true) {
            
            try {

                clients.addThread(serverSocket.accept(), buffer, clients);
            
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }
}