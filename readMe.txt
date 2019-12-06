No external libraries/jars needed. Simply run ServerMain.java, open up windows command prompt, and use telnet

In windows command prompt, type 'telnet localhost 6666' and you will be connected to the client.
This program is designed to accomodate multiple instances of clients.

There is no ClientMain.java in use (and hence no client main function). Rather, it runs on multiple threads of clients initialized by the server.

