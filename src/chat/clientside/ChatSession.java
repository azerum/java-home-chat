package chat.clientside;

import chat.serverside.MessageWriter;
import chat.util.Stoppable;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ChatSession extends Stoppable {
    private final Socket socket;
    private final String nickname;

    private MessageWriter writer;

    public ChatSession(Socket socket, String nickname) {
        this.socket = socket;
        this.nickname = nickname;
    }

    public void start() throws IOException {
        writer = new MessageWriter(socket.getOutputStream());

        BlockingMessageReader

        Thread readingAndSendingThread = new Thread(
            this::readAndSendUserMessages,
            "read-and-send-thread"
        );

        readingAndSendingThread.start();
    }

    @Override
    protected void doStop() {

    }

    private void readAndSendUserMessages() {
        Scanner scanner = new Scanner(System.in);

        try {
            while (true) {
                String message = scanner.nextLine();
                writer.write(message);
            }
        }
        catch (IllegalStateException ignored) {}
    }
}
