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
    private Process parent;

    @Nullable
    private Runnable onStoppedItself = null;

    public void setParent(Process parent) {
        this.parent = parent;
    }

    public void setOnStoppedItself(Runnable onStoppedItself) {
        this.onStoppedItself = onStoppedItself;
    }

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

    protected final void spawnChild(NonIoProcess child) {
        spawnChild(child, null);
    }

    protected final void spawnChild(NonIoProcess child, Runnable onStoppedItself) {
        child.setParent(this);
        child.setOnStoppedItself(onStoppedItself);

        child.start();
        children.add(child);
    }

    protected final boolean trySpawnChild(IoProcess child) {
        return trySpawnChild(child, null);
    }

    protected final boolean trySpawnChild(
        IoProcess child,
        Runnable onStoppedItself
    ) {
        child.setParent(this);
        child.setOnStoppedItself(onStoppedItself);

        try {
            child.start();

            children.add(child);
            return true;
        }
        catch (IOException ignored) {
            return false;
        }
    }

    private void removeChild(Process child) {
        children.remove(child);
    }
}
