# CNCClientModule

This is the counterpart CLIENT module of the Command-And-Control communication program that I have written in java.
The SERVER module can be found at https://github.com/Kav-K/CNCCommandModule

Command-And-Control allows you to control multiple machines running a Linux-style operating system from one commanding machine.

## Features
- Command server can send commands to be executed on all clients at once
- Displays the status of individual clients when sending commands
- Displays output for all clients individually when executing a command
- Clients automatically reconnect to command if the command server is offline for x time, or undergoes an update
- Kill function to terminate all clients
- Reconnection feature (Reconnect clients manually from command)
## Todo
- Send commands to individual clients only (WIP)
- Obtain client system information/statistics
- Start clients on system startup

## How To Use
- Build the CNCCommandModule
- Build the CNCClientModule after replacing the COMMANDIP and COMMANDPORT fields in Main.java with the respective ip and port of your command module
- Done! (Simple right?)
## Security
This system is NOT cryptographically secure but does have a system of verification to ensure that server Command requests are authentic. The current security implementation involves the client sending a KeyRequest to the server. The server will then send back a copy of its public key to the client. Authentic communication from the designated server will have a digital signature attached to it, and will be verified on the client side before commands are executed.


Although this system is not fully secure, it can be assumed that attacks that involve ip spoofing posing as the server are limited and will not be able to effectively send commands to a compromised client server. This is because IP-spoofing is limited in its nature and an attacker would not be able to capture any sort of response from a client or a server. That being said, I absolutely do not reccomend using this system for services where passwords or sensitive information may need to be entered into the terminal.

A possible security flaw for this system would be if an attacker were to capture the public key sent by the server, and change it to a malicious public key that matches a malicious private key. The attacker would then need to send spoofed packets making it look like it was from the actual server with Command objects. Kryonet is somewhat good at inherently blocking spoofed connections and packets but will not work all the time.
## Kryonet
This project uses the kryonet library from EsotericSoftware (https://github.com/EsotericSoftware/kryonet) Kryonet is an asynchronous communication library for Java built upon Java NIO and makes communication throughout this program seamless.
