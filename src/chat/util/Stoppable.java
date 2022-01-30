package chat.util;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Stoppable {
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Nullable
    private Runnable onStoppedItself = null;

    public void handleOnStoppedItself(@Nullable Runnable handler) {
        onStoppedItself = handler;
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
