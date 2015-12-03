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
        clients.remove(userID);
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
            for (int i = 0; i < clients.getTotal(); i++) {

                ProducerThread tmp = clients.getThread(i);

                if (tmp != null) {

                    tmp.sendMessage(out);
                }
            }
        }
    }
}

abstract class Buffer<T> {

    protected class Node<T> {

        T item;
        Node<T> prev, next;

        Node(T t) {

            item = t;
            prev = null; next = null;
        }
    }

    protected Node<T> head, tail;
    protected boolean dataAvailable;

    Buffer() {

        head = null; tail = null;
        dataAvailable = false;
    }

    public synchronized void add(T t) {

        if (head == null) {

            head = new Node<T>(t);
            tail = head;

        } else {

            Node<T> n = new Node<T>(t);
            tail.next = n;
            n.prev = tail;
            tail = n;
        }
    }

    public synchronized T remove() {

        T t = head.item;
        head = head.next;

        if (head == null) { dataAvailable = false; }
        else { head.prev = null; }

        return t;
    }
}

class MessageBuffer extends Buffer<String> {

    public synchronized void add(String s) {

        super.add(s);
        dataAvailable = true;
        notifyAll();
    }

    public synchronized String remove() {

        try { while (!dataAvailable) {wait();} }
        catch (InterruptedException e) { return null; }

        String s = super.remove();
        notifyAll();
        return s;
    }
}

class ClientList extends Buffer<ProducerThread> {

    private int size, total;

    public synchronized int getSize() {

        return size;
    }

    public synchronized int getTotal() {

        return total;
    }

    public synchronized void add(
        Socket s, MessageBuffer m, ClientArray c) {

        super.add(new ProducerThread(s, m, c, total));

        new Thread(getThread(total)).start();

        size++; total++;
        System.out.println("Clients: " + size);
    }

    public synchronized void remove(int userID) {

        Node<ProducerThread> n = new Node<ProducerThread>(getThread(userID));

        if (n.prev == null && n.next == null) {

            // Remove singleton item
            head = null; tail = null;

        } else if (n.prev == null) {

            // Remove head
            head = n.next;

        } else if (n.next == null) {

            // Remove tail
            tail = n.prev; tail.next = null;

        } else { 

            // Remove item from anywhere in middle
            n.prev.next = n.next;
            n.next.prev = n.prev;
        }

        size--;
        System.out.println("Clients: " + size);
    }

    public synchronized ProducerThread getThread(int userID) {

        Node<ProducerThread> n = head;

        while (n != null && n.item.getID() != userID) {
            
            n = n.next;
        }

        if (n == null) { return null; }
        return n.item;
    }
}

public class ChatServerAbstract {

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
        ClientArray clients = new ClientArray();
        ConsumerThread consumer = new ConsumerThread(buffer, clients);

        new Thread(consumer).start();

        // Loop forever
        while (true) {
            
            try {

                clients.add(serverSocket.accept(), buffer, clients);
            
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }
}