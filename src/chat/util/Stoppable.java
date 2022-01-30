package chat.util;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Stoppable {
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Nullable
    private Runnable onStoppedItself = null;

    public void setOnStoppedItself(@Nullable Runnable action) {
        onStoppedItself = action;
    }

    protected final void notifyStoppedItself() {
        if (onStoppedItself != null) onStoppedItself.run();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public void stop() {
        if (!stopped.getAndSet(true)) {
            doStop();
        }
    }

    protected abstract void doStop();
}
