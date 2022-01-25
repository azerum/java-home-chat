package chat.serverside;

import chat.serverside.util.Threads;
import chat.serverside.util.process.NonIoProcess;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Broadcaster extends NonIoProcess {
    private record Broadcast(ClientWriter writerToSkip, String message) {}

    private final BlockingQueue<Broadcast> pendingBroadcasts =
        new LinkedBlockingQueue<>();

    private final Set<ClientWriter> clientWriters =
        ConcurrentHashMap.newKeySet();

    private Thread broadcastingThread;

    @Override
    public void start() {
        broadcastingThread = new Thread(this::sendBroadcasts, "broadcaster");
        broadcastingThread.start();
    }

    @Override
    protected void interruptSelf() {
        broadcastingThread.interrupt();
    }

    public void broadcast(ClientWriter writerToSkip, String message) {
        Threads.handleInterrupted(
            () -> pendingBroadcasts.put( new Broadcast(writerToSkip, message) )
        );
    }

    private void sendBroadcasts() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Broadcast broadcast = pendingBroadcasts.take();

                //Duplicate messages to stdout for easier debugging
                System.out.println(broadcast.message);

                clientWriters.forEach(w -> {
                    if (w != broadcast.writerToSkip) {
                        w.write(broadcast.message);
                    }
                });
            }
        }
        catch (InterruptedException ignored) {}

        onStopped();
    }

    public void addClientWriter(ClientWriter writer) {
        clientWriters.add(writer);
    }

    public void removeClientWriter(ClientWriter writer) {
        clientWriters.remove(writer);
    }
}
