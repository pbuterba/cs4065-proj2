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
import server.ClientConnection;
public class Server {
    public static void main(String[] args) throws Exception {
        //Create server socket
        ServerSocket serverSocket = new ServerSocket(6789);

        //Create data structure to track connected clients, connected users, and messages sent
        ArrayList<ClientConnection> connectedClients = new ArrayList<ClientConnection>();
        ArrayList<String> connectedUsers = new ArrayList<String>();
        HashMap<String, Message> messages = new HashMap<String, Message>();

        //Print message to show that server program has successfully started
        System.out.println("Server started");

        //Loop infinetely until server is closed
        while(true) {
            Socket clientSocket = serverSocket.accept();
            ClientConnection newClient = new ClientConnection(clientSocket);
            System.out.println("New client connected with address " + clientSocket.getRemoteSocketAddress());
            connectedClients.add(newClient);
            Thread thread = new Thread(newClient);
            thread.start();
        }
    }
}