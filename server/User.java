package server;

import java.net.*;
public class User {
   //Fields
   private String username;
   private Socket socket;

   //Constructor
   public User(String username, Socket socket) {
      this.username = username;
      this.socket = socket;
   }

   //Getter methods
   public String getUsername() {
      return username;
   }
   public Socket getSocket() {
      return socket;
   }
}
