import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static final String COMMANDIP = "178.128.228.52";
    private static final int COMMANDPORT = 80;
    static Client client;
    private static boolean authenticated = false;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";


    public static void main(String[] args) {
        initialize();


    }

    private static void registerClasses() {
        Kryo kryo = client.getKryo();
        kryo.register(RegisterRequest.class);
        kryo.register(AuthenticationConfirmation.class);
        kryo.register(KeepAlive.class);
        kryo.register(Command.class);
        log("Registered kryonet classes");
    }

    private static void initialize() {


        log("Initializing client");
        client = new Client();
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
        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof AuthenticationConfirmation) {
                    authenticated = true;
                    log("Successfully authenticated by command");

                } else if (object instanceof KeepAlive) {
                    authenticated = true;
                }
            }
        });
        client.sendTCP(craftRegisterRequest());
        log("Sent authentication request");
        startKeepAliveCheck();
        startCommandListener();


    }

    private static void startCommandListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
               if (object instanceof Command) {
                   //if (!authenticated) return;

                   Command command = (Command) object;
                   commandLog(command.getCommand());



               }
            }
        });

    }

    private static void startKeepAliveCheck() {
        //TODO timer that checks for keep alive packets from the server and  shuts donw if it doesn't see any!
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

        timer.scheduleAtFixedRate(task, 2000, 5000);


    }

    private static void startReconnectionThread() {
        //TODO Start repeating task that keeps trying to reconnect to the command if authenticated == false
        Timer timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    client.close();
                    client.stop();
                    client = new Client();
                    client.start();
                    registerClasses();
                    client.connect(3000, COMMANDIP, COMMANDPORT);
                    log("Successfully reconnected to command");
                    client.addListener(new Listener() {
                        public void received(Connection connection, Object object) {
                            if (object instanceof AuthenticationConfirmation) {
                                authenticated = true;
                                log("Successfully authenticated by command");

                            } else if (object instanceof KeepAlive) {
                                authenticated = true;
                            }
                        }
                    });
                    client.sendTCP(craftRegisterRequest());
                    log("Sent authentication request");
                    startKeepAliveCheck();
                    this.cancel();

                } catch (Exception e) {
                    error(e.getMessage());
                    error("Unable to connect to command");

                }

            }


        };
        timer.scheduleAtFixedRate(task, 0, 5000);


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

    public static void log(String message) {
        System.out.println(ANSI_GREEN + "[Client] " + ANSI_RESET + message);
    }

    public static void error(String message) {
        System.out.println(ANSI_RED + "[Client] " + ANSI_RESET + message);
    }

    public static void commandLog(String message){
        System.out.println(ANSI_BLUE+"[RECEIVED COMMAND] "+ANSI_RESET+message);
    }

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
}
