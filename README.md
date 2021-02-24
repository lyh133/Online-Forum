# Online-Forum

Program design:
•
•
•
•
•

Client folder containing Client.java represents the client end
Server folder containing Server.java and credentials.txt resents the server end
Thread meta data is stored within the code.
Supports multiple client, server creates a thread for a client once a client connects to it and
then continue to listen to new client.
Every client has two threads one to interact with server and another one to test if the server
is down.

Application layer protocol:
•
•
•
•

Message exchanged between client and java is communicated with UTF string
Uses TCP connection
Command is sent as for eg, “CRT thread1 alex” with username appended at the end of
the string
After server recieves command, it sends response to client and then client will act opon that
response and give message to user

How server command line works:
•
•

cd server
java Server port_number server_password

How client command line works:
•
•

Cd client
java Client address port _number

How the system works:
•

Initially server sits in a loop and asks user to login, after successful login, server sits in a loop
to listen to user command and call functions related to the commands

Design trade-offs
•
•
•

•

Uses multi threading instead of multi process because threading is light weight
UTFString is chosen to communicate because the command is not complicated
Client uses multi-threading to decide if the server is down while blocked waiting for user
input, this has extra overhead but and can ensure responsiveness between clients and
server
Meta data is store in memory instead of in a separate file, this is bad if server needs
maintenance, but in this case the server never stops running anyways .
