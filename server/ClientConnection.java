package server;

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
        // TODO add code to handle various messages that may be received from a client  
    }
}
