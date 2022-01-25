package chat.serverside.util;

public class Threads {
    public interface InterruptibleAction {
        void run() throws InterruptedException;
    }

    public static void handleInterrupted(InterruptibleAction action) {
        try {
            action.run();
        }
        catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
