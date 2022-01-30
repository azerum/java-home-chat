package chat.shared;

import java.util.Scanner;
import java.util.function.Consumer;

public class BlockingMessageReader extends Stoppable {
    private final Scanner scanner;
    private final Consumer<String> onMessageRead;

    public BlockingMessageReader(Scanner scanner, Consumer<String> onMessageRead) {
        this.scanner = scanner;
        this.onMessageRead = onMessageRead;
    }

    public void start() {
        readMessages();
    }

    @Override
    protected void doStop() {
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

        notifyStoppedItself();
    }
}
