/*
 * CS 4065 - Computer Networks & Networked Computing
 * University of Cincinnati - Fall 2023
 * Project #2: Bulletin Board
 * Project Team: Preston Buterbaugh, Madilyn Coulson, Chloe Belletti
 * Class for representing messages sent on the bulletin board
 */

package server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    //Fields
    private String id;
    private String sender;
    private Date date;
    private String subject;
    private String content;

    //Constructor
    public Message(String id, String sender, String subject, String content) {
        this.id = id;
        this.sender = sender;
        this.date = new Date();
        this.subject = subject;
        this.content = content;
    }

    //Getter methods
    public String getId() {
        return id;
    }

    public String getSenderName() {
        return sender;
    }

    public String getPostDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Format as needed
        return dateFormat.format(date);
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    //Other methods
    @Override
    public String toString() {
        return id + " " + sender + " " + getPostDate() + " " + subject;
    }

    public String toJsonString() {
        String JsonString = "{\"id\": \"" + id + "\", ";
        JsonString = JsonString + "\"sender\": \"" + sender + "\", ";
        JsonString = JsonString + "\"date\": \"" + getPostDate() + "\", ";
        JsonString = JsonString + "\"subject\": \"" + subject + "\"}";
        return JsonString;
    }
}