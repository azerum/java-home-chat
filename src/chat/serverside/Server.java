package chat.serverside;

import chat.util.Stoppable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Stoppable {
    private final int port;
    private final Set<Client> clients = ConcurrentHashMap.newKeySet();

    private ServerSocket serverSocket;
    private Broadcaster broadcaster;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        broadcaster = new Broadcaster();
        broadcaster.start();

        Thread thread = new Thread(
            this::acceptIncomingConnections,
            "connection-listener"
        );

        thread.start();
    }

    @Override
    protected void doStop() {
        try {
            serverSocket.close();
        }
        catch (IOException ignored) {}
    }

    private void acceptIncomingConnections() {
        while (true) {
            Socket clientSocket;

            try {
                clientSocket = serverSocket.accept();
            }
            catch (IOException e) {
                break;
            }

            Client client = new Client(clientSocket, broadcaster);
            client.handleOnStoppedItself(() -> clients.remove(client));

            try {
                client.start();
                clients.add(client);
            }
            catch (IOException ignored) {}
        }

        broadcaster.stop();
        clients.forEach(Client::stop);
    }
}
