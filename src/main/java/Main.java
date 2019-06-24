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





   private static void initialize() {



        log("Initializing client");
      client = new Client();
       client.start();
       try {
           log("Connecting client to command");
           client.connect(5000, COMMANDIP,COMMANDPORT);
           log("Connected to command");
           Kryo kryo = client.getKryo();
           kryo.register(RegisterRequest.class);
           kryo.register(AuthenticationConfirmation.class);
           kryo.register(KeepAlive.class);
           log("Registered kryonet classes");

       } catch (Exception e) {
           e.printStackTrace();
           error("Unable to connect to command");
           System.exit(0);
       }
       client.addListener(new Listener() {
           public void received (Connection connection, Object object) {
               if (object instanceof AuthenticationConfirmation) {
                   authenticated = true;
                   log("Successfully authenticated by command");

               }
           }
       });
       client.sendTCP(craftRegisterRequest());
       log("Sent authentication request");
       startKeepAlive();





   }
   private static void startKeepAlive() {
        //TODO timer that checks for keep alive packets from the server and  shuts donw if it doesn't see any!

   }

   private static RegisterRequest craftRegisterRequest() {
        String hostname = "null";
        String ipAddress = "null";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e ) {
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
        return new RegisterRequest(hostname,ipAddress);


   }

    public static void log(String message){
        System.out.println(ANSI_GREEN+"[Command] "+ANSI_RESET+message);
    }
    public static void error(String message) {
        System.out.println(ANSI_RED+"[Command] "+ANSI_RESET+message);
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
