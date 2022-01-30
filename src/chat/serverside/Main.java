package chat.serverside;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        final int port = 4242;
        Server server = new Server(port);

        try {
            server.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping...");
            server.stop();
        }));

        System.out.println("Running on port " + port);
        System.out.println("Use Ctrl + C to stop the server");
    }
}
