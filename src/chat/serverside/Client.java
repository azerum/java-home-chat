package chat.serverside;

import chat.serverside.util.process.IoProcess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client extends IoProcess {
    private final int NICKNAME_READING_TIMEOUT = 10;
    private final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final Socket socket;
    private final Broadcaster broadcaster;

    private Scanner scanner;

    private ClientWriter writer;
    private String nickname;

    public Client(Socket socket, Broadcaster broadcaster) {
        this.socket = socket;
        this.broadcaster = broadcaster;
    }

    @Override
    public void start() throws IOException {
        InputStream in = socket.getInputStream();
        scanner = new Scanner(in);

        Thread readingThread = new Thread(this::run, "client");
        readingThread.start();
    }

    @Override
    protected void interruptSelf() {
        try {
            socket.close();
        }
        catch (IOException ignored) {}
    }

    private void run() {
        nickname = readNicknameOrTimeout();

        if (nickname == null) {
            resetConnection();
            return;
        }

        writer = spawnWriter();

        if (writer == null) {
            onStopped();
            return;
        }

        broadcaster.addClientWriter(writer);
        broadcast(nickname + " has joined the chat");

        readMessages();

        broadcaster.removeClientWriter(writer);
        broadcast(nickname + " has left the chat");

        onStopped();
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
            socket.close();
        }
        catch (IOException ignored) {}
    }

    private ClientWriter spawnWriter() {
        OutputStream out;

        try {
            out = socket.getOutputStream();
        }
        catch (IOException ignored) {
            return null;
        }

        writer = new ClientWriter(out);
        spawnChild(writer, this::onWriterStopped);

        return writer;
    }

    private void onWriterStopped() {
        interruptSelf();
    }

    private void readMessages() {
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            broadcast(nickname + ": " + message);
        }
    }

    private void broadcast(String message) {
        broadcaster.broadcast(writer, message);
    }
}
