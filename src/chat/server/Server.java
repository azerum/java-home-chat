package chat.server;

import chat.client.Client;
import chat.client.ClientWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final Set<Client> clients = ConcurrentHashMap.newKeySet();

    private final int port;
    private ServerSocket serverSocket;

    private final Broadcaster broadcaster;

    public Server(int port) {
        this.port = port;
        broadcaster = new Broadcaster();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        Thread listeningThread =
            new Thread(this::acceptIncomingConnections, "listener");

        listeningThread.start();
        broadcaster.start();
    }

    public void interrupt() {
        try {
            serverSocket.close();
        }
        catch (IOException ignored) {}
    }

    private void acceptIncomingConnections() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                Client client = new Client(
                    socket,
                    this::onClientReady,
                    this::onMessageRead,
                    this::onClientDisconnected
                );

                try {
                    client.start();
                    clients.add(client);
                }
                catch (IOException ignored) {}
            }
        }
        catch (IOException e) {
            if (!(e instanceof SocketException)) {
                e.printStackTrace();
            }
        }

        stopRestOfProgram();
    }

    private void onClientReady(ClientWriter writer, String nickname) {
        broadcaster.addClientWriter(writer);
        broadcaster.broadcast(writer, nickname + " has joined the chat");
    }

    private void onMessageRead(ClientWriter writerToSkip, String nickname, String message) {
        broadcaster.broadcast(writerToSkip, nickname + ": " + message);
    }

    private void onClientDisconnected(Client client, ClientWriter writer, String nickname) {
        if (writer != null) {
            broadcaster.removeClientWriter(writer);
        }

        clients.remove(client);
        broadcaster.broadcast(writer, nickname + "has left the chat");
    }

    private void stopRestOfProgram() {
        broadcaster.interrupt();
        clients.forEach(Client::interrupt);
    }
}
