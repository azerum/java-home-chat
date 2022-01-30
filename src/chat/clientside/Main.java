package chat.clientside;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Connecting to a chat room...");

        System.out.print("Enter host: ");
        final String host = scanner.nextLine();

        System.out.print("Enter port: ");

        final int port = scanner.nextInt();

        //scanner.nextInt() оставляет за собой недочитанный символ
        //новой строки - дочитываем его
        scanner.nextLine();

        System.out.print("Enter your nickname: ");
        final String nickname = scanner.nextLine();

        Socket socket;

        try {
            socket = new Socket(host, port);
        }
        catch (IOException e) {
            System.out.println("Failed to connect to the chat room: ");
            e.printStackTrace();
            return;
        }

        ChatSession session = new ChatSession(socket, nickname);

        try {
            session.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Runtime.getRuntime().addShutdownHook( new Thread(session::stop) );
    }
}
