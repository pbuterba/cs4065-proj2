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

    //User lists
    public static ArrayList<User> group1Users = new ArrayList<User>();
    public static ArrayList<User> group2Users = new ArrayList<User>();
    public static ArrayList<User> group3Users = new ArrayList<User>();
    public static ArrayList<User> group4Users = new ArrayList<User>();
    public static ArrayList<User> group5Users = new ArrayList<User>();
    
    //Message lists
    public static ArrayList<Message> group1Messages = new ArrayList<Message>();
    public static ArrayList<Message> group2Messages = new ArrayList<Message>();
    public static ArrayList<Message> group3Messages = new ArrayList<Message>();
    public static ArrayList<Message> group4Messages = new ArrayList<Message>();
    public static ArrayList<Message> group5Messages = new ArrayList<Message>();
    
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
        //Send connection data to client
        String payload = "{\"message_type\": \"connection_data\", \"groups\": [\"Group 1\", \"Group 2\", \"Group 3\", \"Group 4\", \"Group 5\"]}\n";
        sendToClient(socket, payload);

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
            if(command.equals("groupjoin")) {
                addUser(args.get(0), args.get(1));
            } /*else if(command.equals("post")) {
                //Get username
                String username = args.get(0);
                args.remove(0);
                
                //Get message subject and content
                String subject = "";
                String messageContent = "";
                boolean delimiterFound = false;
                for(String word : args) {
                    if(delimiterFound) {
                        messageContent += word + " ";
                    } else {
                        if(word.contains(":")) {
                            subject += word.substring(0, word.length() - 1);
                            delimiterFound = true;
                        } else {
                            subject += word + " ";
                        }
                    }
                }

                //Remove space at the end of message
                messageContent = messageContent.substring(0, messageContent.length() - 1);

                //Call createPost function
                createPost(username, subject, messageContent);
            } else if(command.equals("message")) {
                retrieveMessage(args.get(0));
            }*/ else if (command.equals("groupusers")) {
                sendUserList(args.get(0));
            } else if(command.equals("groupleave")){
                removeUser(args.get(0), args.get(1));
            }

            dataLine = readFromClient(socket);
        }

        payload = "{\"message_type\": \"exit\"}\n";
        sendToClient(socket, payload);
        
        //Close the socket
        System.out.println(socket.getRemoteSocketAddress() + " disconnected");
        socket.close();
    }

    //Function for user joining the group
    public void addUser(String groupId, String username) throws Exception {
        System.out.println(username + " joined group " + groupId);

        //Gather information to send to the new user formatted as JSON string
        String payload = "{\"message_type\": \"join_data\",";
        payload += "\"users\": [";

        //Get user and message lists
        ArrayList<User> connectedUsers;
        ArrayList<Message> groupMessages;
        if(groupId.equals("1")) {
            connectedUsers = group1Users;
            groupMessages = group1Messages;
        } else if(groupId.equals("2")) {
            connectedUsers = group2Users;
            groupMessages = group2Messages;
        } else if(groupId.equals("3")) {
            connectedUsers = group3Users;
            groupMessages = group3Messages;
        } else if(groupId.equals("4")) {
            connectedUsers = group4Users;
            groupMessages = group4Messages;
        } else {
            connectedUsers = group5Users;
            groupMessages = group5Messages;
        }
        
        //Get a list of all currently connected users
        for(User user : connectedUsers) {
            payload += "\"" + user.getUsername() + "\",";
        }
        if(connectedUsers.size() > 0) {
            payload = payload.substring(0, payload.length() - 1); //Remove trailing comma
        }
        payload += "], \"messages\": [";

        //Get last two messages sent
        if(groupMessages.size() >= 2) {
            payload += "\"" + groupMessages.get(groupMessages.size() - 2).toString() + "\",";
        }
        if(groupMessages.size() >= 1) {
            payload += "\"" + groupMessages.get(groupMessages.size() - 1).toString() + "\"";
        }
        payload += "]}\n";

        //Send payload data to client
        sendToClient(socket, payload);
        
        //Inform all other connected clients that the user has joined
        payload = "{\"message_type\": \"notification\",";
        payload += "\"message\": \"" + username + " joined group" + groupId + "\"}\n";
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), payload);
        }

        //Add the newly connected user to the server's list of users
        User newUser = new User(username, socket);
        connectedUsers.add(newUser);
    }  

    // Function to create a new post on the message board
    /* public void createPost(String username, String subject, String messageText) throws Exception {
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
        // Find the message with the given ID
        Message targetMessage = null;
        for(Message message : messages) {
            if(message.getId().equals(messageID)) {
                targetMessage = message;
                break;
            }
        }
        String payload = "{\"message_type\": \"notification\", ";
        payload += "\"message\": \"";
        if(targetMessage != null) {
            // Send the message content to the client
            String messageContent = targetMessage.getContent();
            payload += messageContent;
        } else {
            // Notify the client that the message was not found
            payload += "Message with ID " + messageID + " not found";
        }
        payload += "\"}\n";
        sendToClient(socket, payload);
    } */

    //Function for outputting list of users.
    public void sendUserList(String groupId) throws Exception {
        //Get user list
        ArrayList<User> connectedUsers;
        if(groupId.equals("1")) {
            connectedUsers = group1Users;
        } else if(groupId.equals("2")) {
            connectedUsers = group2Users;
        } else if(groupId.equals("3")) {
            connectedUsers = group3Users;
        } else if(groupId.equals("4")) {
            connectedUsers = group4Users;
        } else {
            connectedUsers = group5Users;
        }

        //Get a list of all currently connected users
        String payload = "{\"message_type\": \"notification\", \"message\": \"Current list of users in group " + groupId + ": ";
        for(User users : connectedUsers) {
            payload += users.getUsername() + ",";
        }
        payload = payload.substring(0, payload.length() - 1);  //Remove trailing comma
        payload += "\"}\n";
        sendToClient(socket, payload);
    }

    //Function for removing user from the group
    public void removeUser(String groupId, String username) throws Exception {
        User user = null;

        //Get user list
        ArrayList<User> connectedUsers;
        if(groupId.equals("1")) {
            connectedUsers = group1Users;
        } else if(groupId.equals("2")) {
            connectedUsers = group2Users;
        } else if(groupId.equals("3")) {
            connectedUsers = group3Users;
        } else if(groupId.equals("4")) {
            connectedUsers = group4Users;
        } else {
            connectedUsers = group5Users;
        }
        
        // Ensure that the user is part of the connected users
        for(User users : connectedUsers) {
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
        payload += user.getUsername() + " left group " + groupId;
        payload += "\"}\n";

        //Print that the user is leaving the group
        System.out.println(user.getUsername() + " left group " + groupId);
        
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