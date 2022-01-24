package chat.server;

import chat.Threads;
import chat.client.ClientWriter;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Broadcaster {
    record Broadcast(ClientWriter writerToSkip, String message) {}

    private final BlockingQueue<Broadcast> pendingBroadcasts =
        new LinkedBlockingQueue<>();

    private final Set<ClientWriter> clientWriters = ConcurrentHashMap.newKeySet();

    private Thread broadcastingThread;

    public void start() {
        broadcastingThread = new Thread(this::broadcast, "broadcaster");
        broadcastingThread.start();
    }

    public void interrupt() {
        broadcastingThread.interrupt();
    }

    public void broadcast(ClientWriter writerToSkip, String messages) {
        Threads.handleInterrupted(
            () -> pendingBroadcasts.put(new Broadcast(writerToSkip, messages))
        );
    }

    private void broadcast() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Broadcast broadcast = pendingBroadcasts.take();

                clientWriters.forEach(w -> {
                    if (w != broadcast.writerToSkip) {
                        w.write(broadcast.message);
                    }
                });
            }
        }
        catch (InterruptedException ignored) {}
    }

    public void addClientWriter(ClientWriter writer) {
        clientWriters.add(writer);
    }

    public void removeClientWriter(ClientWriter writer) {
        clientWriters.remove(writer);
    }
}
