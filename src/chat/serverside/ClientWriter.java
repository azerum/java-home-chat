package chat.serverside;

import chat.serverside.util.Threads;
import chat.serverside.util.process.NonIoProcess;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientWriter extends NonIoProcess {
    private final BlockingQueue<String> messagesToWrite =
        new LinkedBlockingQueue<>();

    private final OutputStream out;
    private Thread writingThread;

    public ClientWriter(OutputStream out) {
        this.out = out;
    }

    @Override
    public void start() {
        writingThread = new Thread(this::writeMessages, "client-writer");
        writingThread.start();
    }

    @Override
    protected void interruptSelf() {
        writingThread.interrupt();
    }

    public void write(String message) {
        Threads.handleInterrupted(
            () -> messagesToWrite.put(message)
        );
    }

    private void writeMessages() {
        PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8);

        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (writer.checkError()) {
                    onStopped();
                    break;
                }

                String message = messagesToWrite.take();
                writer.println(message);
            }
        }
        catch (InterruptedException ignored) {}
    }
}
