package chat.shared;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Stoppable {
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Nullable
    public Runnable onStoppedItself = null;

    public boolean isStopped() {
        return stopped.get();
    }

    public void stop() {
        if (!stopped.getAndSet(true)) {
            doStop();
        }
    }

    protected abstract void doStop();

    protected final void notifyStoppedItself() {
        if (onStoppedItself != null) onStoppedItself.run();
    }
}
