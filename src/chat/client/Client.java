package chat.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.BiConsumer;

public class Client {
    private final Socket socket;

    @FunctionalInterface
    public interface OnMessageReadHandler {
        void accept(ClientWriter writerToSkip, String nickname, String message);
    }

    @FunctionalInterface
    public interface OnDisconnectedHandler {
        void accept(Client client, ClientWriter writer, String nickname);
    }

    private final BiConsumer<ClientWriter, String> onReady;
    private final OnMessageReadHandler onMessageRead;
    private final OnDisconnectedHandler onDisconnected;

    private ClientReader reader;
    private ClientWriter writer;

    private String nickname;

    public Client(
        Socket socket,
        BiConsumer<ClientWriter, String> onReady,
        OnMessageReadHandler onMessageRead,
        OnDisconnectedHandler onDisconnected
    ) {
        this.socket = socket;

        this.onReady = onReady;
        this.onMessageRead = onMessageRead;
        this.onDisconnected = onDisconnected;
    }

    public void start() throws IOException {
        InputStream in = socket.getInputStream();

        reader = new ClientReader(
            in,
            this::onNicknameRead,
            this::interrupt,
            message -> onMessageRead.accept(writer, nickname, message),
            this::interrupt
        );

        reader.start();
    }


    private void onNicknameRead(String nickname) {
        this.nickname = nickname;

        OutputStream out;

        try {
            out = socket.getOutputStream();
        }
        catch (IOException ignored) {
            return;
        }

        writer = new ClientWriter(out, this::interrupt);
        writer.start();

        onReady.accept(writer, nickname);
    }

    public void interrupt() {
        reader.interrupt();

        if (writer != null) {
            writer.interrupt();
        }

        ensureSocketIsClosed();
        onDisconnected.accept(this, writer, nickname);
    }

    private void ensureSocketIsClosed() {
        if (socket.isClosed()) {
            return;
        }

        try {
            socket.close();
        }
        catch (IOException ignored) {}
    }
}
