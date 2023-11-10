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
            ArrayList<String> args = new ArrayList<String>();
            while(tokens.hasMoreTokens()) {
                args.add(tokens.nextToken());
            }

            //Take action based on what command was entered
            if(command.equals("join")) {
                addUser(args.get(0));
            } else if(command.equals("post")) {
                createPost(args.get(0), args.get(1), args.get(2));
            } else if(command.equals("message")) {
                System.out.println("Message command detected");
                retrieveMessage(args.get(0));
            } else if (command.equals("users")) {
                sendUserList();
            } else if(command.equals("leave")){
                removeUser(args.get(0));
            }

            dataLine = readFromClient(socket);
        }

        String payload = "{\"message_type\": \"exit\"}\n";
        sendToClient(socket, payload);
        
        //Close the socket
        System.out.println(socket.getRemoteSocketAddress() + " disconnected");
        socket.close();
    }

    //Function for user joining the group
    public void addUser(String username) throws Exception {
        System.out.println(username + " issued join command");

        //Gather information to send to the new user formatted as JSON string
        String payload = "{\"message_type\": \"join_data\",";
        payload += "\"users\": [";
        
        //Get a list of all currently connected users
        for(User user : connectedUsers) {
            payload += "\"" + user.getUsername() + "\",";
        }
        if(connectedUsers.size() > 0) {
            payload = payload.substring(0, payload.length() - 1); //Remove trailing comma
        }
        payload += "], \"messages\": [";

        //Get last two messages sent
        if(messages.size() >= 2) {
            payload += messages.get(messages.size() - 2).toString() + ",";
        }
        if(messages.size() >= 1) {
            payload += messages.get(messages.size() - 1).toString();
        }
        payload += "]}\n";

        //Send payload data to client
        sendToClient(socket, payload);
        
        //Inform all other connected clients that the user has joined
        payload = "{\"message_type\": \"notification\",";
        payload += "\"message\": \"" + username + " joined the group\"}\n";
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), payload);
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
        String messageContent = newMessage.toString();
        String payload = "{\"message_type\": \"notification\",";
        payload += "\"message\": \"" + messageContent + "\"}\n";
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), payload);
        }
    }
    
    // Function to retrieve the content of a specific message by its ID
    public void retrieveMessage(String messageID) throws Exception {
        System.out.println("Searching for message " + messageID);
        // Find the message with the given ID
        Message targetMessage = null;
        for(Message message : messages) {
            System.out.println("Checking message " + message.getId());
            if(message.getId().equals(messageID)) {
                targetMessage = message;
                System.out.println("Message found");
                break;
            }
        }
        String payload = "{\"message_type\": \"notification\", ";
        payload += "\"message\": \"";
        if(targetMessage != null) {
            // Send the message content to the client
            String messageContent = targetMessage.getContent();
            System.out.println("Adding message content to payload");
            payload += messageContent;
        } else {
            // Notify the client that the message was not found
            System.out.println("Adding error message to payload");
            payload += "Message with ID " + messageID + " not found";
        }
        payload += "\"}\n";
        System.out.println("Sending payload: " + payload);
        sendToClient(socket, payload);
    }

    //Function for outputting list of users.
    public void sendUserList() throws Exception {
        //Get a list of all currently connected users
        String payload = "{\"message_type\": \"notification\", \"message\": \"Current list of users: ";
        for(User users : connectedUsers) {
            payload += users.getUsername() + ",";
        }
        payload = payload.substring(0, payload.length() - 1);  //Remove trailing comma
        payload += "\"}\n";
        sendToClient(socket, payload);
    }

    //Function for removing user from the group
    public void removeUser(String username) throws Exception {
        System.out.println("Started removeUser function for username " + username);
        User user = null;
        
        // Ensure that the user is part of the connected users
        for(User users : connectedUsers) {
            System.out.println("Checking user: " + users.getUsername());
            if(users.getUsername().equals(username)) {
                user = users;
                break;
            }
        }

        if (user == null) {
            throw new IllegalArgumentException("User not found in group.");
        }
        
        String payload = "{\"message_type\": \"notification\", \"message\": \"";
        
        //Store the username into the message
        payload += user.getUsername() + " left the group";
        payload += "\"}\n";

        //Print that the user is leaving the group
        System.out.println(user.getUsername() + " is leaving the group. ");
        
        //Remove the user from connectUsers
        connectedUsers.remove(user);

        //Inform all other connected clients that the user has left the group
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), payload);
        }
        
        payload = "{\"message_type\": \"notification\", \"message\": \"Successfully left the group\"}\n";

        //Send user list to client
        sendToClient(socket, payload);
    }

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