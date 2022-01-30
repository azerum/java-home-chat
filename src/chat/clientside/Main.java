package chat.clientside;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter chat room host: ");
        final String host = scanner.nextLine();

        System.out.println();

        System.out.print("Enter chat room port: ");
        final int port = scanner.nextInt();

        System.out.println();

        System.out.print("Enter your nickname: ");
        final String nickname = scanner.nextLine();

        System.out.println();

        Socket socket;

        try {
            socket = new Socket(host, port);
        }
        catch (IOException e) {
            System.out.println("Failed to connect to the chat room: ");
            e.printStackTrace();

            return;
        }


    }
}
