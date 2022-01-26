package chat.serverside;

import java.io.IOException;
import java.util.Scanner;

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

        Scanner scanner = new Scanner(System.in);

        System.out.println("Running on port " + port);
        System.out.println("Enter anything to stop the server");

        scanner.nextLine();
        server.interrupt();
    }
}
