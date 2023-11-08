"""
CS 4065 - Computer Networks and Networked Computing
University of Cincinnati - Fall 2023
Project #2: Bulletin Board
Project Team: Preston Buterbaugh, Madilyn Coulson, Chloe Belletti
Bulletin Board Client
"""

import socket
import sys


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

    # Set starting variables
    command = ''
    connection_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # Loop until command is "exit"
    while True:
        # Get next command
        command = input()

        # Check command
        if command == 'exit':
            print('Exiting')
            return 0
        elif command.startswith('connect'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 2:
                print('Not enough arguments supplied for connect command. Requires [hostname] and [port number]')
                continue

            # Get host and port number from args
            host = args[0]
            try:
                port = int(args[1])
            except ValueError:
                print(f'{args[1]} is not a valid port number')
                continue

            # Create socket connection
            try:
                connection_socket.connect((host, port))
            except socket.gaierror:
                print(f'Could not resolve {host}:{port} into a valid IP address')
                continue
            except ConnectionRefusedError:
                print(f'No server found at address {host}:{port}')
                continue
            print('Successfully connected to bulletin board server')
        elif command.startswith('join'):
            # Get command arguments
            args = command.split(' ')[1:]
            if len(args) < 1:
                print('Not enough arguments specified for join command. Requires [username]')
                continue

            # Construct join command
            username = args[0]
            message = f'join {username}\n'

            # Send join command
            try:
                connection_socket.sendall(message.encode('utf-8'))
            except OSError:
                print('Unable to join group. You are not connected to a bulletin board server.')
                continue

            print('Successfully joined the group')

            # Listen for user list
            user_list = connection_socket.recv(4096).decode('utf-8')

            # Remove newline character
            if user_list.endswith('\n'):
                user_list = user_list[0:len(user_list) - 1]

            # Convert to list
            user_list = user_list.split(',')

            # Print if there are any items in the list
            if user_list[0]:
                print('Currently online users:')
                for username in user_list:
                    print(username)
        elif command.startswith('post') or command.startswith('message') or command == 'users' or command == 'leave':
            print('This command is not yet implemented')
        else:
            print('Invalid command')


if __name__ == "__main__":
    sys.exit(main())
