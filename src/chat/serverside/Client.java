package chat.serverside;

import chat.shared.BlockingMessageReader;
import chat.shared.MessageWriter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client {
    private static final int NICKNAME_READING_TIMEOUT = 10;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final Socket socket;
    private final Broadcaster broadcaster;

    private Scanner scanner;
    private OutputStream out;

    private String nickname;
    private MessageWriter writer;
    private BlockingMessageReader reader;

    @Nullable
    public Runnable onStoppedItself = null;

    public Client(Socket socket, Broadcaster broadcaster) {
        this.socket = socket;
        this.broadcaster = broadcaster;
    }

    public void start() throws IOException {
        InputStream in = socket.getInputStream();
        scanner = new Scanner(in);

        out = socket.getOutputStream();

        Thread readingThread = new Thread(this::run, "client");
        readingThread.start();
    }

    public void stop() {
        if (writer != null) writer.stop();
        if (reader != null) reader.stop();

        try {
            socket.close();
        }
        catch (IOException ignored) {}
    }

    private void run() {
        doRun();
        stop();
    }

    private void doRun() {
        nickname = readNicknameOrTimeout();

        if (nickname == null) {
            resetConnection();
            return;
        }

        writer = new MessageWriter(out);
        writer.onStoppedItself = this::onWriterStopped;

        writer.start();

        broadcaster.addMessageWriter(writer);
        broadcast(nickname + " has joined the chat");

        reader = new BlockingMessageReader(scanner, this::onMessageRead);
        reader.start();

        broadcaster.removeMessageWriter(writer);
        broadcast(nickname + " has left the chat");
    }

    private String readNicknameOrTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> readNickname = executor.submit(scanner::nextLine);

        try {
            return readNickname.get(NICKNAME_READING_TIMEOUT, TIMEOUT_UNIT);
        }
        catch (TimeoutException | ExecutionException e) {
            //Nickname reading timed out or scanner.nextLine() has
            //failed because of socket problem
            return null;
        }
        catch (InterruptedException | CancellationException e) {
            System.err.println("Should not happen:");
            e.printStackTrace();
            assert false;

            return null;
        }
        finally {
            executor.shutdown();
        }
    }

    private void resetConnection() {
        try {
            socket.setSoLinger(true, 0);
        }
        catch (SocketException e) {
            System.err.println("Error setting SO_LINGER (resetting connection): ");
            e.printStackTrace();
        }
    }

    private void onWriterStopped() {
        stop();
        if (onStoppedItself != null) onStoppedItself.run();
    }

    private void onMessageRead(String message) {
        broadcast(nickname + ": " + message);
    }

    private void broadcast(String message) {
        broadcaster.broadcast(writer, message);
    }
}
