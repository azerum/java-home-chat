package chat.serverside;

import chat.util.Stoppable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client extends Stoppable {
    private static final int NICKNAME_READING_TIMEOUT = 10;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final Socket socket;
    private final Broadcaster broadcaster;

    private Scanner scanner;
    private String nickname;

    private MessageWriter writer;

    public Client(Socket socket, Broadcaster broadcaster) {
        this.socket = socket;
        this.broadcaster = broadcaster;
    }

    public void start() throws IOException {
        scanner = new Scanner(socket.getInputStream());
        writer = new MessageWriter(socket.getOutputStream());

        Thread thread = new Thread(this::run, "client");
        thread.start();
    }

    @Override
    protected void doStop() {
        writer.stop();

        try {
            socket.close();
        }
        catch (IOException ignored) {}
    }

    private void run() {
        handleClient();
        stopSelf();
    }

    private void stopSelf() {
        if (!isStopped()) {
            stop();
            notifyStoppedItself();
        }
    }

    private void handleClient() {
        nickname = readNicknameOrTimeout();

        if (nickname == null) {
            resetConnection();
            return;
        }

        writer.setOnStoppedItself(this::stopSelf);
        writer.start();

        broadcaster.addMessageWriter(writer);
        broadcaster.broadcast(writer, nickname + " has joined the chat");

        readMessages();

        broadcaster.removeMessageWriter(writer);
        broadcaster.broadcast(writer, nickname + " has left the chat");
    }

    @Nullable
    private String readNicknameOrTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> readNickname = executor.submit(scanner::nextLine);

        try {
            return readNickname.get(NICKNAME_READING_TIMEOUT, TIMEOUT_UNIT);
        }
        catch (TimeoutException | ExecutionException e) {
            //Истекло время ожидания сообщения с никнеймом, или
            //scanner.nextLine() выбросил исключение из-за
            //проблем с сокетом
            return null;
        }
        catch (InterruptedException | CancellationException e) {
            //Этих исключений никогда не должно произойти

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

    private void readMessages() {
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            broadcaster.broadcast(writer, nickname + ": " + message);
        }
    }
}
