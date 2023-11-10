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

# Create global socket variable
connection_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_username = ""


def main() -> int:
    """
    @brief  This is the main function for the Bulletin Board Client
    @return: (int) Whether the client exited with an error
    """
    # Print menu options
    print('Bulletin Board Client Options: ')
    print('connect [server address] [server connection port] - Connects to a bulletin board server')
    print('join - Joins the message board on the connected server')
    print('post [subject] [message text] - Posts a message on the bulletin board')
    print('users - Lists the users currently on the message board')
    print('leave - Leaves the message board')
    print('message [message ID] - Gets the content of a message')
    print('exit - Leaves the message board (if applicable) and exits the client program')

    # Create listen_to_sever thread
    server_listen_thread = threading.Thread(target=listen_to_server)

    # Loop until command is "exit"
    while True:
        # Get next command
        command = input()

        # Check command
        if command == 'exit':
            print('Exiting')
            send_exit_command()
            server_listen_thread.join()
            connection_socket.shutdown(socket.SHUT_RDWR)
            connection_socket.close()
            return 0
        elif command.startswith('connect'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments supplied for connect command. Requires [hostname] and [port number]')
                continue

            # Verify that port number is an integer and call function to join group
            try:
                connection_failed = client_connect(args[0], int(args[1]))
            except ValueError:
                print(f'{args[1]} is not a valid port number')
                continue

            # Start server listening thread
            if not connection_failed:
                server_listen_thread.start()

        elif command.startswith('join'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments specified for join command. Requires [username]')
                continue

            # Call function to join group
            join_group(args[0])

        elif command.startswith('post'):
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments supplied for post command. Requires [subject] and [message text]')
                continue

            subject = args[0]
            message_text = ' '.join(args[1:])
            post_message(subject, message_text)
        
        elif command.startswith('message'):
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments supplied for message command. Requires [message ID]')
                continue
            message_content(args[0])
        elif command == 'users':
            user_list()
        elif command == 'leave':
            # Call function to leave group
            leave_group()
        else:
            print('Invalid command')


def listen_to_server():
    """
    @brief      Listens to the socket to process any incoming messages from the sever
    """
    # Listen for server data
    data_string = ''
    while '\n' not in data_string:
        data_string = data_string + connection_socket.recv(4096).decode('utf-8')

    # Convert JSON string into a dictionary, dropping the newline character off the end
    server_data = json.loads(data_string[0:len(data_string) - 1])

    # Loop until server sends exit command
    while server_data['message_type'] != 'exit':
        if server_data['message_type'] == 'notification':
            print(server_data['message'])
        elif server_data['message_type'] == 'join_data':
            if server_data['users']:
                print('Currently online users: ')
                for user in server_data['users']:
                    print(user)
            if server_data['messages']:
                print('Previously sent messages: ')
                for message in server_data['messages']:
                    print(message)

        # Listen for server data
        data_string = ''
        while '\n' not in data_string:
            data_string = data_string + connection_socket.recv(4096).decode('utf-8')
        server_data = json.loads(data_string[0:len(data_string) - 1])


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
    print('Successfully connected to bulletin board server')
    return 0


def join_group(username: str):
    """
    @brief  Joins the group using the specified username
    @param  (str) username: The name with which to join the group
    @return: (int)
            - 0 if successful group join
            - 1 otherwise
    """
    # Construct join command
    message = f'join {username}\n'

    # Send join command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to join group. You are not connected to a bulletin board server.')
        return

    print('Successfully joined the group')
    global client_username
    client_username = username


def post_message(subject: str, message_text: str):
    """
    @brief  Posts a message on the bulletin board
    @param  (str) subject: The subject of the message
    @param  (str) message_text: The content of the message
    """
    # Construct the post command
    post_command = f'post {client_username} {subject} {message_text}\n'

    # Send the post command to the server
    try:
        connection_socket.sendall(post_command.encode('utf-8'))
    except OSError:
        print('Unable to post message. You are not connected to a bulletin board server.')


def message_content(message_id: str):
    """
    @brief  Retrieves the content of a message with the given message ID
    @param  (str) message_id: The ID of the message to retrieve
    """
    # Construct the message command
    message_command = f'message {message_id}\n'
    print(f'Sending command {message_command}')

    # Send the message command to the server
    try:
        connection_socket.sendall(message_command.encode('utf-8'))
    except OSError:
        print('Unable to retrieve message. You are not connected to a bulletin board server.')


def user_list():
    # Construct users command
    message = 'users\n'
    
    # Send users command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to list the users of the group.')


def leave_group():
    # Construct leave command
    message = f'leave {client_username}\n'
    print(f'Sending command leave {client_username}')
    
    # Send leave command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to remove the user from the group.')


def send_exit_command():
    print('Sending exit command')
    connection_socket.sendall('exit\n'.encode('utf-8'))


if __name__ == "__main__":
    sys.exit(main())
