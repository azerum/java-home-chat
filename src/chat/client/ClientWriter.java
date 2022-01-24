package chat.client;

import chat.Threads;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientWriter {
    private final BlockingQueue<String> messagesToWrite
        = new LinkedBlockingQueue<>();

    private final Runnable onStoppedItself;
    private final OutputStream out;

    private Thread writingThread;

    public ClientWriter(OutputStream out, Runnable onStoppedItself) {
        this.out = out;
        this.onStoppedItself = onStoppedItself;
    }

    public void start() {
        writingThread = new Thread(this::writeMessages, "client-writer");
        writingThread.start();
    }

    public void interrupt() {
        writingThread.interrupt();
    }

    public void write(String message) {
        Threads.handleInterrupted(
            () -> messagesToWrite.put(message)
        );
    }

    private void writeMessages() {
        PrintWriter writer =
            new PrintWriter(out, false, StandardCharsets.UTF_8);

        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (writer.checkError()) {
                    onStoppedItself.run();
                    break;
                }

                String message = messagesToWrite.take();
                writer.println(message);
            }
        }
        catch (InterruptedException ignored) {}
    }
}
