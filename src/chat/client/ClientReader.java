package chat.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ClientReader {
    private static final int NICKNAME_READ_TIMEOUT_IN_SECONDS = 10;

    private final InputStream in;
    private final Scanner scanner;

    private final Consumer<String> onNicknameRead;
    private final Runnable onReadingNicknameFailed;
    private final Consumer<String> onMessageRead;

    private final Runnable onStoppedItself;

    private final AtomicBoolean interrupted = new AtomicBoolean(false);

    public ClientReader(
        InputStream in,
        Consumer<String> onNicknameRead,
        Runnable onReadingNicknameFailed,
        Consumer<String> onMessageRead,
        Runnable onStoppedItself
    ) {
        this.in = in;
        scanner = new Scanner(in);

        this.onNicknameRead = onNicknameRead;
        this.onReadingNicknameFailed = onReadingNicknameFailed;
        this.onMessageRead = onMessageRead;

        this.onStoppedItself = onStoppedItself;
    }

    public void start() {
        Thread workingThread = new Thread(this::run, "client-reader");
        workingThread.start();
    }

    public void interrupt() {
        if (!interrupted.getAndSet(true)) {
            try {
                in.close();
            }
            catch (IOException ignored) {}
        }
    }

    private void run() {
        String nickname = readNicknameOrTimeout();

        //Timeout
        if (nickname == null) {
            onReadingNicknameFailed.run();
            return;
        }

        onNicknameRead.accept(nickname);
        readMessages();
    }

    private String readNicknameOrTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> readNickname = executor.submit(scanner::nextLine);

        try {
            String nickname = readNickname.get(
                NICKNAME_READ_TIMEOUT_IN_SECONDS,
                TimeUnit.SECONDS
            );

            return nickname;
        }
        catch (InterruptedException | TimeoutException | ExecutionException ignored) {
            return null;
        }
        finally {
            executor.shutdown();
        }
    }

    private void readMessages() {
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            onMessageRead.accept(message);
        }

        if (!interrupted.get()) {
            onStoppedItself.run();
        }
    }
}
