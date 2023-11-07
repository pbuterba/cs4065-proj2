/*
 * CS 4065 - Computer Networks & Networked Computing
 * University of Cincinnati - Fall 2023
 * Project #2: Bulletin Board
 * Project Team: Preston Buterbaugh, Madilyn Coulson, Chloe Belletti
 * Class for representing users connected to a group
 */
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
