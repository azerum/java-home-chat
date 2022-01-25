package chat.serverside.util.process;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Process {
    private final Set<Process> children = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean interrupted = new AtomicBoolean(false);

    @Nullable
    /*package-private*/ Process parent;

    @Nullable
    /*package-private*/ Runnable onStoppedItself = null;

    public final void interrupt() {
        if (interrupted.getAndSet(true)) {
            return;
        }

        children.forEach(Process::interrupt);
        interruptSelf();

        if (parent != null) {
            parent.removeChild(this);
        }
    }

    protected abstract void interruptSelf();

    protected final void onStopped() {
        if (!interrupted.get() && onStoppedItself != null) {
            onStoppedItself.run();
        }
    }

    protected final void spawnChild(Process child) {
        spawnChild(child, null);
    }

    protected final void spawnChild(Process child, Runnable onStoppedItself) {
        child.parent = this;
        child.onStoppedItself = onStoppedItself;

        if (child instanceof NonIoProcess nonIoProcess) {
            nonIoProcess.start();
        }
        else if (child instanceof IoProcess ioProcess) {
            try {
                ioProcess.start();
            }
            catch (IOException ignored) {
                return;
            }
        }

        children.add(child);
    }

    private void removeChild(Process child) {
        children.remove(child);
    }
}
