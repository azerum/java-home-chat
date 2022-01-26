package chat.serverside.util.process;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Process {
    private final Set<Process> children = ConcurrentHashMap.newKeySet();

    private final Object stoppedLock = new Object();
    private boolean stopped = false;

    @Nullable
    /*package-private*/ Process parent;

    @Nullable
    /*package-private*/ Runnable onStoppedItself = null;

    public final void interrupt() {
        doIfNotStopped(() -> {
            interruptChildrenAndDetachFromParent();
            interruptSelf();
        });
    }

    protected abstract void interruptSelf();

    protected final void onStopped() {
        doIfNotStopped(() -> {
            interruptChildrenAndDetachFromParent();

            if (onStoppedItself != null) {
                onStoppedItself.run();
            }
        });
    }

    private void doIfNotStopped(Runnable stopAction) {
        synchronized (stoppedLock) {
            if (stopped) {
                return;
            }

            stopAction.run();
            stopped = true;
        }
    }

    private void interruptChildrenAndDetachFromParent() {
        children.forEach(Process::interrupt);

        if (parent != null) {
            parent.removeChild(this);
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
