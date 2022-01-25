package chat.serverside;

import chat.serverside.util.process.IoProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends IoProcess {
    private final int port;

    private ServerSocket serverSocket;
    private Broadcaster broadcaster;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        broadcaster = new Broadcaster();
        spawnChild(broadcaster);

        Thread thread = new Thread(
            this::acceptIncomingConnections,
            "connection-listener"
        );

        thread.start();
    }

    private void acceptIncomingConnections() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                Client client = new Client(clientSocket, broadcaster);
                spawnChild(client);
            }
        }
        catch (IOException ignored) {}

        onStopped();
    }

    @Override
    protected void interruptSelf() {
        try {
            serverSocket.close();
        }
        catch (IOException ignored) {}
    }
}
