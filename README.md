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
## Kryonet
This project uses the kryonet library from EsotericSoftware (https://github.com/EsotericSoftware/kryonet) Kryonet is an asynchronous communication library for Java built upon Java NIO and makes communication throughout this program seamless.
