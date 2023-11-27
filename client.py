"""
CS 4065 - Computer Networks and Networked Computing
University of Cincinnati - Fall 2023
Project #2: Bulletin Board
Project Team: Preston Buterbaugh, Madilyn Coulson, Chloe Belletti
Bulletin Board Client
"""

import json
import socket
import sys
import threading
from time import sleep

# Create global socket variable
connection_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Create variable to track if the client is currently connected to a server
connected = False

# Create array to track the usernames which the client user has used to join each group
client_usernames = ['', '', '', '', '']


def main() -> int:
    """
    @brief  This is the main function for the Bulletin Board Client. It continuously loops as the user enters commands
            to interact with the server, until the exit command is entered
    @return: (int)
            - 0 if exited without error
            - 1 otherwise
    """
    # Print menu options
    print('Bulletin Board Client Options: ')
    print('connect [server address] [server connection port] - Connects to a bulletin board server')
    print('groupjoin [group ID] [username] - Joins the specified group')
    print('grouppost [group ID] [subject]: [message text] - Posts a message to the specified group with the given subject')
    print('groupusers [group ID] - Lists the users currently in the specified group')
    print('groupleave [group ID] - Leaves the specified group')
    print('groupmessage [group ID] [message ID] - Gets the content of a specified message that was posted in the given group')
    print('exit - Leaves any groups that are currently joined and exits the client program')

    # Create listen_to_sever thread which continually listens for messages from the server
    server_listen_thread = threading.Thread(target=listen_to_server)

    # Loop until command is "exit"
    command = ''
    while command != 'exit':
        # Get next command
        command = input()

        # Check command
        if command.startswith('connect'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments supplied for connect command. Requires [hostname] and [port number]')
                continue

            # Verify that port number is an integer and call function to connect to the server
            try:
                connection_failed = client_connect(args[0], int(args[1]))
            except ValueError:
                print(f'{args[1]} is not a valid port number')
                continue

            # Start server listening thread
            if not connection_failed:
                server_listen_thread.start()
        elif command.startswith('groupjoin'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments specified for groupjoin command. Requires [group ID] and [username]')
                continue

            # Check if the specified group ID is valid
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue

            # Check if the user has already joined the specified group
            if check_joined_group_id(args[0]):
                print(f'You cannot join group {args[0]} because you are already in that group')
                continue

            # Call function to join group
            join_group(args[0], args[1])
        elif command.startswith('grouppost'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 3:
                print('Not enough arguments supplied for grouppost command. Format [group ID] [subject]: [message text]')
                continue

            # Check if the specified group ID is valid
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue

            # Check if the user is a member of the group into which they are trying to post
            if not check_joined_group_id(args[0]):
                print(f'You cannot post to group {args[0]} because you are not a member of that group')
                continue

            # Save the group ID and parse the rest of the arguments into a message subject and body
            group_id = args[0]
            message_components = ' '.join(args[1:]).split(': ')

            # Check that both components of the message were included
            if len(message_components) < 2:
                print('Not enough arguments supplied for grouppost command. Format [group ID] [subject]: [message text]')
                continue

            # Call the function to post a message
            post_message(group_id, f'{message_components[0]}: {message_components[1]}')
        elif command.startswith('groupmessage'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments supplied for groupmessage command. Requires [group ID] [message ID]')
                continue

            # Check that entered group ID is valid
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue

            # Check if the user is a member of the group in which they are trying to retrieve the message
            if not check_joined_group_id(args[0]):
                print(f'You cannot retrieve messages from group {args[0]} because you are not a member of that group')
                continue

            # Call the function to retrieve message contents
            message_content(args[0], args[1])
        elif command.startswith('groupusers'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments specified for groupusers command. Requires [group ID]')
                continue

            # Check if entered group ID is valid
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue

            # Check if user is a member of the group in which they are attempting to see the logged in users
            if not check_joined_group_id(args[0]):
                print(f'You cannot see the users in group {args[0]} because you are not a member of that group')
                continue

            # Call user list function
            user_list(args[0])
        elif command.startswith('groupleave'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments specified for groupleave command. Requires [group ID]')
                continue

            # Check if entered group ID is valid
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue

            # Check that user is in the group they are trying to leave
            if not check_joined_group_id(args[0]):
                print(f'You cannot leave group {args[0]} because you are not a member of that group')
                continue

            # Call function to leave group
            leave_group(args[0])
        elif command != 'exit':
            print('Invalid command')

    # Leave any groups that the client is still in
    for idx, client_username in enumerate(client_usernames):
        if client_username:
            leave_group(str(idx + 1))
            sleep(1)  # Wait to allow the server to finish processing that the client has left the group

    # Print message to the client console
    print('Exiting')

    # Disconnect from the server if connected
    if connected:
        # Send a message to the server to inform it that this client is disconnecting
        client_disconnect()

        # Terminate the thread listening to the server
        server_listen_thread.join()

        # Shut down the socket
        connection_socket.shutdown(socket.SHUT_RDWR)

    # Close the socket and exit the client program without error
    connection_socket.close()
    return 0


def listen_to_server():
    """
    @brief      Listens to the socket to process any incoming messages from the sever
    """
    # Listen for server data. Each JSON payload from the server ends with a newline character
    data_string = ''
    while '\n' not in data_string:
        data_string = data_string + connection_socket.recv(4096).decode('utf-8')

    # Convert JSON string into a dictionary, dropping the newline character off the end
    try:
        server_data = json.loads(data_string[0:len(data_string) - 1])
    except json.decoder.JSONDecodeError:
        # This case should only occur when debugging changes to server messaging protocol
        # all messages from the server should be valid JSON
        print(f'Invalid server response sent:\n{data_string[0:len(data_string) - 1]}')
        sys.exit(1)

    # Loop until server sends exit command
    while server_data['message_type'] != 'exit':
        # Use the message_type field from the server data to determine how to handle the message
        if server_data['message_type'] == 'notification':
            # "Notification" message is already formatted to be printed straight to the client console
            print(server_data['message'])
        elif server_data['message_type'] == 'connection_data':
            # "Connection Data" message indicates successful server connection and contains a list of groups on the server
            print('Successfully connected to bulletin board server. ')
            if server_data['groups']:
                print('Available groups on this server are:')
                for group in server_data['groups']:
                    print(group)
            else:
                print('There are no groups available on this server')
            print()
        elif server_data['message_type'] == 'join_data':
            # "Join Data" message contains information about a group that the user just joined (user list and last two messages)
            if server_data['users']:
                print('Currently online users: ')
                for user in server_data['users']:
                    print(user)
                print()
            if server_data['messages']:
                print('Previously sent messages: ')
                for message in server_data['messages']:
                    print(message)
                print()

        # Return to listening for server data
        data_string = ''
        while '\n' not in data_string:
            data_string = data_string + connection_socket.recv(4096).decode('utf-8')

        try:
            server_data = json.loads(data_string[0:len(data_string) - 1])
        except json.decoder.JSONDecodeError:
            print(f'Invalid server response sent:\n{data_string[0:len(data_string) - 1]}')
            sys.exit(1)


def client_connect(host: str, port: int) -> int:
    """
    @brief  Connects the client socket to the server at the specified address
    @param  (str) host: The internet address of the server to connect to
    @param  (int) port: The port to connect to on the server
    @return: (int)
            - 0 if successful connection
            - 1 otherwise
    """
    # Create socket connection
    try:
        connection_socket.connect((host, port))
    except socket.gaierror:
        print(f'Could not resolve {host}:{port} into a valid IP address')
        return 1
    except ConnectionRefusedError:
        print(f'No server found at address {host}:{port}')
        return 1

    # Mark connection status as true and return successful connection
    global connected
    connected = True
    return 0


def join_group(group_id: str, username: str):
    """
    @brief  Joins a group using the specified username
    @param  (str) group_id: The ID of the group to join
    @param  (str) username: The name with which to join the group
    @return: (int)
            - 0 if successful group join
            - 1 otherwise
    """
    # Construct join command
    message = f'groupjoin {group_id} {username}\n'

    # Send join command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to join group. You are not connected to a bulletin board server.')
        return

    # Print message indicating successful join, and update the appropriate spot in the client_usernames list
    # to show that that group has been joined
    print(f'Successfully joined group {group_id}')
    global client_usernames
    client_usernames[int(group_id) - 1] = username


def post_message(group_id: str, message: str):
    """
    @brief  Posts a message to a group
    @param  (str) group_id: The ID of the group in which to post the message
    @param  (str) message: A colon separated string containing the subject and content of the message
    """
    # Construct the post command
    post_command = f'grouppost {client_usernames[int(group_id) - 1]} {group_id} {message}\n'

    # Send the post command to the server
    try:
        connection_socket.sendall(post_command.encode('utf-8'))
    except OSError:
        print('Unable to post message. You are not connected to a bulletin board server.')


def message_content(group_id: str, message_id: str):
    """
    @brief  Retrieves the content of a message with the given message ID
    @param  (str) group_id: The ID of the group to retrieve the message from
    @param  (str) message_id: The ID of the message to retrieve
    """
    # Construct the message command
    message_command = f'groupmessage {group_id} {message_id}\n'

    # Send the message command to the server
    try:
        connection_socket.sendall(message_command.encode('utf-8'))
    except OSError:
        print('Unable to retrieve message. You are not connected to a bulletin board server.')


def user_list(group_id: str):
    """
    @brief  Retrieves a list of users in the specified group
    @param  (str) group_id: The ID of the group from which to get the user list
    """
    # Construct users command
    message = f'groupusers {group_id}\n'
    
    # Send users command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to list the users of the group.')


def leave_group(group_id: str):
    """
    @brief  Leaves a specified group
    @param  (str) group_id: The ID of the group to leave
    """
    global client_usernames

    # Construct leave command
    message = f'groupleave {group_id} {client_usernames[int(group_id) - 1]}\n'
    
    # Send leave command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to remove the user from the group.')
        return

    # Clear that spot in the client_usernames list so that the client knows that it is not in that group anymore
    client_usernames[int(group_id) - 1] = ''


def check_valid_group_id(group_id: str) -> bool:
    """
    @brief      Checks if a group ID is valid (between 1 and 5 inclusive)
    @param      (str) group_id: The group ID to check
    @return:    (bool)
                - True if group ID is between 1 and 5 inclusive
                - False otherwise
    """
    try:
        group_id_int = int(group_id)
    except ValueError:
        # If group ID can't be converted to integer, it is invalid
        return False
    return group_id_int > 0 and group_id_int < 6


def check_joined_group_id(group_id: str) -> bool:
    """
    @brief      Checks if the client has already joined a group
    @param      (str) group_id: The ID of the group to see if the client has already joined
    @return:    (bool)
                - True if the client has joined the group
                - False otherwise
    """
    group_id_int = int(group_id)
    return bool(client_usernames[group_id_int - 1])


def client_disconnect():
    """
    @brief  Sends a message to the server to inform it that it is going to disconnect. This allows the server
            to disconnect from its end
    """
    connection_socket.sendall('exit\n'.encode('utf-8'))
    global connected
    connected = False


if __name__ == "__main__":
    sys.exit(main())
