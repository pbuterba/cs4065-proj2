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
public class Server {
    public static void main(String[] args) throws Exception {
        //Create server socket
        ServerSocket server_socket = new ServerSocket(6789);

        //Create data structure to track connected clients, connected users, and messages sent
        ArrayList<Socket> connected_clients = new ArrayList<Socket>();
        ArrayList<String> connected_users = new ArrayList<String>();
        HashMap<String, Message> messages = new HashMap<String, Message>();

        //Print message to show that server program has successfully started
        System.out.println("Server started");

        //Loop infinetely until server is closed
        while(true) {

        }
    }
    
    public static void acceptConnection() {}
    public static void addUser() {}
    public static void createPost() {}
    public static void sendUserList() {}
    public static void removeUser() {}
    public static void retrieveMessage() {}
    public static void closeServer() {}
}