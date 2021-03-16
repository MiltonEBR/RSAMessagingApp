# RSAMessagingApp
This application was made as a project to understand how RSA encryption works.

It that handles a 1 on 1 conversation between the host (server) and a user (client).

The server works by using sockets on a local port and the client can connect to said server in order to send messages.

When you attempt to join a server with an ongoing conversation, the new client will wait until the server finishes the previous conversation and then stablish connection.

Once both clients have stablished a connection, the public RSA keys will be interchanged and all the messages sent will be encrypted and decrypted when received.

It uses random q and p values between 127 and 500 in order to keep a quick interchange of information.

(You can see in the console the encrypted data sent and the values obtained after decrypting).


![Program Screen Shot](https://github.com/MiltonEBR/RSAMessagingApp/blob/main/RSAChat.png)
