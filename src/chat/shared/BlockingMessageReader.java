package chat.shared;

import org.jetbrains.annotations.Nullable;

import java.util.Scanner;
import java.util.function.Consumer;

public class BlockingMessageReader {
    private final Scanner scanner;
    private final Consumer<String> onMessageRead;

    @Nullable
    public Runnable onStoppedItself = null;

    public BlockingMessageReader(Scanner scanner, Consumer<String> onMessageRead) {
        this.scanner = scanner;
        this.onMessageRead = onMessageRead;
    }

    public void start() {
        readMessages();
    }

    public void stop() {
        scanner.close();
    }

    private void readMessages() {
        try {
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                onMessageRead.accept(message);
            }
        }
        catch (IllegalStateException e) {
            return;
        }

        if (onStoppedItself != null) onStoppedItself.run();
    }
}
