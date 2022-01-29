package chat.serverside;

import chat.serverside.shared.MessageWriter;
import chat.serverside.shared.Threads;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Broadcaster {
    private record Broadcast(MessageWriter writerToSkip, String message) {}

    private final BlockingQueue<Broadcast> pendingBroadcasts =
        new LinkedBlockingQueue<>();

    private final Set<MessageWriter> messageWriters =
        ConcurrentHashMap.newKeySet();

    private Thread broadcastingThread;

    public void start() {
        broadcastingThread = new Thread(this::sendBroadcasts, "broadcaster");
        broadcastingThread.start();
    }

    public void stop() {
        broadcastingThread.interrupt();
    }

    public void broadcast(MessageWriter writerToSkip, String message) {
        Broadcast b = new Broadcast(writerToSkip, message);
        Threads.handleInterrupted(() -> pendingBroadcasts.put(b));
    }

    private void sendBroadcasts() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Broadcast broadcast = pendingBroadcasts.take();

                //Duplicate messages to stdout for easier debugging
                System.out.println(broadcast.message);

                messageWriters.forEach(w -> {
                    if (w != broadcast.writerToSkip) {
                        w.write(broadcast.message);
                    }
                });
            }
        }
        catch (InterruptedException ignored) {}
    }

    public void addMessageWriter(MessageWriter writer) {
        messageWriters.add(writer);
    }

    public void removeMessageWriter(MessageWriter writer) {
        messageWriters.remove(writer);
    }
}
