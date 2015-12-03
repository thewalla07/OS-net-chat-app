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
            username = socketIn.readLine();
            
            buffer.add(username + " has joined the chat");

            while ((input = socketIn.readLine()) != null) {

                buffer.add(username + " says: " + input);
            }

            socketOut.close();
            socket.close();

            exitChat();
            
        } catch (SocketException e) {
            
            exitChat();

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

    private void exitChat() {

        buffer.add(username + " just left the chatroom...");
        clients.removeThread(userID);
    }
}

class ConsumerThread implements Runnable {

    private MessageBuffer buffer;
    private ClientArray clients;

    public ConsumerThread(MessageBuffer b, ClientArray c) {

        buffer = b; clients = c;
    }

    public void run() {

        while (true) {
            
            String out = buffer.remove();
            for (int i = 0; i < clients.getSize(); i++) {

                clients.getThread(i).sendMessage(out);
            }
        }
    }
}

class MessageBuffer {

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

    // what role do ins/outs play?

    MessageBuffer() {

        head = null; tail = null;
        size = 0;
        dataAvailable = false;
    }

    public synchronized void add(String s) {

        if (head == null) {

            head = new Node(s);
            tail = head;

        } else {

            Node n = new Node(s);
            tail.next = n;
            n.prev = tail;
            tail = n;
        }

        size++;
        dataAvailable = true;

        notifyAll();
    }

    public synchronized String remove() {

        try {

            while (!dataAvailable) {wait();}

            String s = head.message;
            head = head.next;

            if (head == null) { dataAvailable = false; }
            else { head.prev = null; }

            size--;

            notifyAll();

            return s;

        } catch (InterruptedException e) { return null; }
    }
}

class ClientArray {

    private ProducerThread[] clients;
    private int occupied, total;

    public ClientArray(int maxNumClients) {

        clients = new ProducerThread[maxNumClients];
    }

    public synchronized int getSize() {

        return occupied;
    }

    public synchronized void addThread(
        Socket s, MessageBuffer m, ClientArray c) {

        clients[occupied] =
            new ProducerThread(s, m, c, total);

        new Thread(clients[occupied]).start();

        occupied++; total++;
        System.out.println("Clients: " + occupied);
    }

    public synchronized void removeThread(int userID) {

        for (int i = 0; i < occupied; i++) {
            
            if (clients[i].getID() == userID) {
                
                for (int j = i; j < occupied - 1; j++) {
                    
                    clients[j] = clients[j + 1];
                }

                occupied--;
                System.out.println("Clients: " + occupied);
                break;
            }
        }
    }

    public synchronized ProducerThread getThread(int i) {

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