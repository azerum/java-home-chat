package chat.serverside;

import chat.util.Stoppable;
import chat.util.Threads;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Broadcaster extends Stoppable {
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

    @Override
    protected void doStop() {
        if (broadcastingThread != null) broadcastingThread.interrupt();
    }

    public void broadcast(MessageWriter writerToSkip, String message) {
        Broadcast b = new Broadcast(writerToSkip, message);
        Threads.handleInterrupted(() -> pendingBroadcasts.put(b));
    }

    private void sendBroadcasts() {
        while (!Thread.currentThread().isInterrupted()) {
            Broadcast broadcast;

            try {
                broadcast = pendingBroadcasts.take();
            }
            catch (InterruptedException e) {
                break;
            }

            //Дублируем все сообщения в консоль для облегчения отладки
            System.out.println(broadcast.message);

            messageWriters.forEach(w -> {
                if (w != broadcast.writerToSkip) {
                    w.write(broadcast.message);
                }
            });
        }
    }

    public void addMessageWriter(MessageWriter writer) {
        messageWriters.add(writer);
    }

    public void removeMessageWriter(MessageWriter writer) {
        messageWriters.remove(writer);
    }
}
