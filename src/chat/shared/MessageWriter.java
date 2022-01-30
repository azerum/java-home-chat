package chat.shared;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageWriter extends Stoppable {
    private final BlockingQueue<String> messagesToWrite =
        new LinkedBlockingQueue<>();

    private final OutputStream out;
    private Thread writingThread;

    public MessageWriter(OutputStream out) {
        this.out = out;
    }

    public void start() {
        writingThread = new Thread(this::writeMessages, "stream-writer");
        writingThread.start();
    }

    @Override
    protected void doStop() {
        if (writingThread != null) writingThread.interrupt();
    }

    public void write(String message) {
        Threads.handleInterrupted(
            () -> messagesToWrite.put(message)
        );
    }

    private void writeMessages() {
        PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8);

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            if (writer.checkError()) {
                notifyStoppedItself();
                break;
            }

            String message;

            try {
                message = messagesToWrite.take();
            }
            catch (InterruptedException e) {
                break;
            }

            writer.println(message);
        }
    }
}
