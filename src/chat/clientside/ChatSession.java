package chat.clientside;

import chat.util.Stoppable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatSession extends Stoppable {
    private final Socket socket;
    private final String nickname;

    private Thread receivingThread;

    public ChatSession(Socket socket, String nickname) {
        this.socket = socket;
        this.nickname = nickname;
    }

    public void start() throws IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        Scanner scanner = new Scanner(in);
        PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8);

        writer.println(nickname);

        receivingThread = new Thread(() -> receiveMessages(scanner), "receiver");

        receivingThread.start();
        sendMessages(writer);
    }

    @Override
    protected void doStop() {
        receivingThread.interrupt();

        try {
            socket.close();
        }
        catch (IOException ignored) {}
    }

    private void receiveMessages(Scanner scanner) {
        try {
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                System.out.println(message);
            }
        }
        catch (IllegalStateException ignored) {}

        stop();
    }

    private void sendMessages(PrintWriter writer) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            if (writer.checkError()) {
                break;
            }

            String message = scanner.nextLine();
            writer.println(message);
        }

        stop();
    }
}
