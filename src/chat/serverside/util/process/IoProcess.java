package chat.serverside.util.process;

import java.io.IOException;

public abstract class IoProcess extends Process {
    public abstract void start() throws IOException;
}
