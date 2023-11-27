# CS4065 Project 2: A Simple Bulletin Board Using Socket Programming
Team Members: Preston Buterbaugh, Madi Coulson, and Chloe Belletti

## Goal of Project:
The goal of this project was to build a bulletin board socket program in which the user can join the server along with one of five different groups. The user is able to post messages to their group's message board, retreive a list of users in their group, retreive the contents of a specified message in their group, leave the group, and exit the program. Along with this, the user is notfied when other users join, post messages, or leave their group. We implement this through a Java command line server and Python client application. To communicate protocol messages, JSON representations are used.

## Instructions to Run Program:
In order to run the program, the following steps should be followed:
1. Start up the Java server, Server.java, by compiling and running the Java program. You should get the output: Server started.
2. Start up the Python client, client.py, by running the Python program You should get a list of options, including: connect, groupjoin, grouppost, groupusers, groupleave, groupmessage, and exit.
3. The first option you want to choose is connect. On the command line, type: connect localhost 6789. This will connect you to the server running on localhost and port 6789. If the server is not running on your localhost, you will need to substitue this with the correct IP address.
4. Next, you will want to choose a group to join. Join a group by typing: groupjoin [group ID]. group ID must be 1-5.
5. Now, you are connected and in a group. You can now post messages to the group, view messages in the group, view users in the group, leave the group, or exit the program. For details on how to run each of the commands, see the section Usability Notes.

## Usability Notes:
A description and the corresponding command for the client program is described below:
* connect: Connect the client to the server. The command is as follows: connect [server address] [server connection port]. If running on your machine, the server address is localhost. If not, the server address will be the IP address the server is running on. The server connection port is 6789.
* groupjoin: Allows a user to join a group. A user cannot join a group more than once. Also, the group numbers are 1-5. A group cannot be joined outside of this range of values. The command is as follows: groupjoin [group ID].
* grouppost: Allows a user to post a message to a specified group. The message must have a subject that is seperated from the message by a colon. The command is as follows: groupost [group ID] [subject] : [message text]
* groupusers: Lists the users within a group. The command is as follows: groupusers [group ID].
* groupleave: Allows the user to leave the group it is part of. The correct ID must be entered for the user to leave its proper group. The command is as follows: groupleave [group ID].
* groupmessage: Allows the user to view a specified message within a group. The group ID and message ID must be input. The command is as follows: groupmessage [group ID] [message ID].
* exit: Allows the user to leave the message board (if applicable) and exits the client program. The command is as follows: exit.

## Major Issues Faced and How we Solved Them:
One challenge that we faced was choosing how to communicate protocol messages. Ultimately, we chose using JSON objects, and had to learn how to properly parse the objects so the messages sent correctly. Along with this, we faced difficulty when implementing the exit command, for initially it was having issues exiting both the group and client program when a user was still within the group. We were able to solve this by ensuring that if the exit command was called prior to a leave command, the leave command was still called before the exit command leaves the cient program. Finally, we had issues parsing the subject and messages when implementing the grouppost command, and at first could only have a single word subject and single word message. We updated our parsing method on both the client and server side to ensure that the entire subject and message was translated properly to both ends of the socket. 


