package server;

import java.io.*;
import java.util.*;
import java.net.*;
public class ClientConnection implements Runnable {
    //Fields
    private Socket socket;

    //Constructor
    public ClientConnection(Socket socket) {
        this.socket = socket;
    }

    //Methods
    public void run() {
        try {
            waitForClientData();
        } except(Exception e) {
            System.out.println(e);
        }
    }

    private void waitForClientData() throws Exception {
        //Get line of text sent from client
        BufferedReader streamFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String dataLine = streamFromClient.readLine();

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
            
        }
        
        //Close the socket
        System.out.println(socket.getRemoteSocketAddress() + " disconnected");
        socket.close();
    }

    public static void addUser() {}
    public static void createPost() {}
    public static void sendUserList() {}
    public static void removeUser() {}
    public static void retrieveMessage() {}
    public static void closeServer() {}
}
