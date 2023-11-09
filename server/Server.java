/*
 * CS 4065 - Computer Networks & Networked Computing
 * University of Cincinnati - Fall 2023
 * Project #2: Bulletin Board
 * Project Team: Preston Buterbaugh, Madilyn Coulson, Chloe Belletti
 * Bulletin Board Server
 */

package server;

import java.io.*;
import java.net.*;
import java.util.*;
import server.Message;
import server.User;

public class Server implements Runnable {
    //Static server data structures
    public static ArrayList<User> connectedUsers = new ArrayList<User>();
    public static ArrayList<Message> messages = new ArrayList<Message>();
    
    //Field for the client connection socket (used in threads)
    private Socket socket;

    //Constructor for use when starting new threads
    public Server(Socket clientSocket) {
        this.socket = clientSocket;
    }

    //Main function - Listens for connection requests and then 
    public static void main(String[] args) throws Exception {
        //Create server socket
        ServerSocket serverSocket = new ServerSocket(6789);

        //Print message to show that server program has successfully started
        System.out.println("Server started");

        //Loop infinetely until server is closed
        while(true) {
            Socket clientSocket = serverSocket.accept();
            Server clientConnection = new Server(clientSocket);
            System.out.println("New client connected with address " + clientSocket.getRemoteSocketAddress());
            Thread thread = new Thread(clientConnection);
            thread.start();
        }
    }

    //Run function - runs threads
    public void run() {
        try {
            waitForClientData();
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    private void waitForClientData() throws Exception {

        //Get line of text sent from client
        String dataLine = readFromClient(socket);

        //Loop until the text "exit" is sent
        while(!dataLine.equals("exit")) {
            //Split the input line into the command and any following arguments
            StringTokenizer tokens = new StringTokenizer(dataLine);
            String command = tokens.nextToken();
            ArrayList<String> args = new ArrayList<String>(tokens.countTokens() - 1);
            while(tokens.hasMoreTokens()) {
                args.add(tokens.nextToken());
            }

            //Take action based on what command was entered
            if(command.equals("join")) {
                addUser(args.get(0));
            }
            else if(command.equals(anObject:"post")) {
                createPost(args.get(index:0), args.get(index:1), args.get(index:2));
            }
            else if(command.equals(anObject:"message")) {
                retrieveMessage(args.get(index:0), args.get(index:1));
            }

            dataLine = readFromClient(socket);
        }
        
        //Close the socket
        System.out.println(socket.getRemoteSocketAddress() + " disconnected");
        socket.close();
    }

    //Function for user joining the group
    public void addUser(String username) throws Exception {
        System.out.println(username + " issued join command");

        //Gather information to send to the new user formatted as JSON string
        String payload = "{\"users\": [";
        
        //Get a list of all currently connected users
        for(User user : connectedUsers) {
            payload = payload + "\"" + user.getUsername() + "\",";
        }
        if(connectedUsers.size() > 0) {
            payload = payload.substring(0, payload.length() - 1); //Remove trailing comma
        }
        payload += "], \"messages\": [";

        //Get last two messages sent
        if(messages.size() >= 2) {
            payload = payload + messages.get(messages.size() - 2).toJsonString() + ",";
        }
        if(messages.size() >= 1) {
            payload = payload + messages.get(messages.size() - 1).toJsonString();
        }
        payload = payload + "]}\n";

        //Send payload data to client
        sendToClient(socket, payload);
        
        //Inform all other connected clients that the user has joined
        String message = username + " joined the group\n";
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }

        //Add the newly connected user to the server's list of users
        User newUser = new User(username, socket);
        connectedUsers.add(newUser);
    }  

    // Function to create a new post on the message board
    public void createPost(String username, String subject, String messageText) throws Exception {
        // Create a new message with a unique ID, sender, post date, subject, and content
        int messageID = messages.size() + 1; // Generate a unique ID

        // Create the message object
        Message newMessage = new Message(Integer.toString(messageID), username, subject, messageText);

        // Add the new message to the list of messages
        messages.add(newMessage);

        // Broadcast the new message to all connected clients
        String messageContent = newMessage.toJsonString();
        for (User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), messageContent);
        }
    }
    
    // Function to retrieve the content of a specific message by its ID
    public void retrieveMessage(int messageID, Socket clientSocket) throws Exception {
        // Find the message with the given ID
        Message targetMessage = null;
        for (Message message : messages) {
            if (Integer.parseInt(message.getId()) == messageID) {
                targetMessage = message;
                break;
            }
        }
        if (targetMessage != null) {
            // Send the message content to the client
            String messageContent = targetMessage.toJsonString();
            sendToClient(clientSocket, messageContent);
        } else {
            // Notify the client that the message was not found
            String errorMessage = "Message with ID " + messageID + " not found";
            sendToClient(clientSocket, errorMessage);
        }
    }

    public void sendUserList() {}
    public void removeUser() {}
    
    //Static helper functions
    public static String readFromClient(Socket clientSocket) throws Exception {
        BufferedReader streamFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return streamFromClient.readLine();
    }

    public static void sendToClient(Socket clientSocket, String data) throws Exception {
        DataOutputStream streamToClient = new DataOutputStream(clientSocket.getOutputStream());
        streamToClient.writeBytes(data);
    }
}