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
client_usernames = ['', '', '', '', '']


def main() -> int:
    """
    @brief  This is the main function for the Bulletin Board Client
    @return: (int) Whether the client exited with an error
    """
    # Print menu options
    print('Bulletin Board Client Options: ')
    print('connect [server address] [server connection port] - Connects to a bulletin board server')
    print('groupjoin [group ID] - Joins the specified group')
    print('grouppost [group ID] [subject]: [message text] - Posts a message to the specified group with the given subject')
    print('groupusers [group ID] - Lists the users currently on the message board')
    print('groupleave [group ID] - Leaves the message board')
    print('groupmessage [group ID] [message ID] - Gets the content of a message')
    print('exit - Leaves the message board (if applicable) and exits the client program')

    # Create listen_to_sever thread
    server_listen_thread = threading.Thread(target=listen_to_server)

    # Loop until command is "exit"
    while True:
        # Get next command
        command = input()

        # Check command
        if command == 'exit':
            if client_usernames[0]:
                leave_group(0)
                sleep(1)
            if client_usernames[1]:
                leave_group(1)
                sleep(1)
            if client_usernames[2]:
                leave_group(2)
                sleep(1)
            if client_usernames[3]:
                leave_group(3)
                sleep(1)
            if client_usernames[4]:
                leave_group(4)
                sleep(1)
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
        elif command.startswith('groupjoin'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments specified for groupjoin command. Requires [group ID] and [username]')
                continue
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue
            if check_joined_group_id(args[0]):
                print(f'You cannot join group {args[0]} because you are already in that group')
                continue

            # Call function to join group
            join_group(args[0], args[1])
        elif command.startswith('post'):
            args = ' '.join(command.split(' ')[1:]).split(': ')
            if len(args) < 2:
                print('Not enough arguments supplied for post command. Format [subject]: [message text]')
                continue

            post_message(f'{args[0]}: {args[1]}')
        elif command.startswith('message'):
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments supplied for message command. Requires [message ID]')
                continue
            message_content(args[0])
        elif command.startswith('groupusers'):
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments specified for groupusers command. Requires [group ID]')
                continue
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue
            if not check_joined_group_id(args[0]):
                print(f'You cannot see the users in group {args[0]} because you are not a member of that group')
                continue
            user_list(args[0])
        elif command.startswith('groupleave'):
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments specified for groupleave command. Requires [group ID]')
                continue
            if not check_valid_group_id(args[0]):
                print('Invalid group ID specified. Must be between 1 and 5')
                continue
            if not check_joined_group_id(args[0]):
                print(f'You cannot leave group {args[0]} because you are not a member of that group')
                continue
            # Call function to leave group
            leave_group(args[0])
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
    try:
        server_data = json.loads(data_string[0:len(data_string) - 1])
    except json.decoder.JSONDecodeError:
        print(f'Invalid server response sent:\n{data_string[0:len(data_string) - 1]}')
        sys.exit(1)

    # Loop until server sends exit command
    while server_data['message_type'] != 'exit':
        if server_data['message_type'] == 'notification':
            print(server_data['message'])
        elif server_data['message_type'] == 'connection_data':
            if server_data['groups']:
                for group in server_data['groups']:
                    print(group)
                print()
        elif server_data['message_type'] == 'join_data':
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

        # Listen for server data
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
    print('Successfully connected to bulletin board server')
    return 0


def join_group(group_id: str, username: str):
    """
    @brief  Joins the group using the specified username
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

    print('Successfully joined the group')
    global client_usernames
    client_usernames[int(group_id) - 1] = username


def post_message(everything: str):
    """
    @brief  Posts a message on the bulletin board
    @param  (str) everything: All the data to send to the server
    """
    # Construct the post command
    post_command = f'post {client_username} {everything}\n'

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

    # Send the message command to the server
    try:
        connection_socket.sendall(message_command.encode('utf-8'))
    except OSError:
        print('Unable to retrieve message. You are not connected to a bulletin board server.')


def user_list(group_id: str):
    """
    @brief  Retrieves a list of users from the server
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
    global client_usernames

    # Construct leave command
    message = f'groupleave {group_id} {client_usernames[int(group_id) - 1]}\n'
    
    # Send leave command
    try:
        connection_socket.sendall(message.encode('utf-8'))
    except OSError:
        print('Unable to remove the user from the group.')
        return

    # Clear client_username variable so that the client knows that it is not in the group anymore
    client_usernames[int(group_id) - 1] = ''


def check_valid_group_id(group_id: str) -> bool:
    group_id_int = int(group_id)
    return group_id_int > 0 and group_id_int < 6


def check_joined_group_id(group_id: str) -> bool:
    group_id_int = int(group_id)
    return bool(client_usernames[group_id_int - 1])


def send_exit_command():
    connection_socket.sendall('exit\n'.encode('utf-8'))


if __name__ == "__main__":
    sys.exit(main())
