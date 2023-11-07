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
    public static HashMap<String, Message> messages = new HashMap<String, Message>();
    
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
        }
        
        //Close the socket
        System.out.println(socket.getRemoteSocketAddress() + " disconnected");
        socket.close();
    }

    //Function for user joining the group
    public void addUser(String username) throws Exception {
        System.out.println(username + " issued join command");

        //Get a list of all currently connected users
        String userList = "";
        for(User user : connectedUsers) {
            userList = userList + user.getUsername() + ",";
        }
        userList = userList.substring(0, userList.length() - 1) + "\n"; //Replace trailing comma with newline

        //Send user list to client
        sendToClient(socket, userList);

        //Add the newly connected user to the server's list of users
        User newUser = new User(username, socket);
        connectedUsers.add(newUser);

        //Inform all other connected clients that the user has joined
        String message = username + " joined the group";
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }
    }

    public void createPost() {}

    //Function for outputting list of users
    public void sendUserList() throws Exception{
        //Get a list of all currently connected users after removing username
        String userList = "";
        for(User users : connectedUsers) {
            userList = userList + users.getUsername() + ",";
        }
        userList = userList.substring(0, userList.length() - 1) + "\n"; //Replace trailing comma with newline

        String message = "Current list of users: " + userList;
        //Inform all other connected clients that the user has left the group
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }


    }
    
    //Function for removing user from the group
    public void removeUser(User user) throws Exception{
        System.out.println("Leaving the group. ");

        //Store the username into the messafe
        String message = user.getUsername() + " left the group";
        
        connectedUsers.remove(user);

        //Get a list of all currently connected users after removing username
        String userList = "";
        for(User users : connectedUsers) {
            userList = userList + users.getUsername() + ",";
        }
        userList = userList.substring(0, userList.length() - 1) + "\n"; //Replace trailing comma with newline

        //Send user list to client
        sendToClient(socket, userList);

        //Inform all other connected clients that the user has left the group
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }
    }

    //Function for closing the server
    public static void closeServer() {
        System.out.println("Closing the server and exiting the client program.");
        System.exit(0);
    }

    public void retrieveMessage() {}

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