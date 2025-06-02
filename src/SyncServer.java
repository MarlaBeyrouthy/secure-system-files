import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

//POJO" – Plain Old Java Object) that holds the details of a synchronization request in a distributed system

//In distributed systems or RMI, objects must be serializable to be transmitted between processes or machines.

//  "Carries sync request data; each request contains a node name and a command"

//// Class to represent a synchronization request in a distributed system.
public class SyncServer implements Runnable {
    private final Node node;
    private final int port;
    public SyncServer(Node node, int port) {
        this.node = node;
        this.port = port;
    }
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Start the sync server socket on the specified port
            System.out.println("[SYNC] SyncServer started for " + node.getName() + " on port " + port);

            // Continuously accept incoming socket connections (clients)
            while (true) {
                // Accept a new connection from a client (e.g., another node)
                Socket socket = serverSocket.accept();

                // Handle the sync request in a separate thread to avoid blocking
                new Thread(() -> handleSyncRequest(socket)).start();
            }
        } catch (IOException e) {
            // Handle any exceptions related to the server socket (e.g., port in use)
            System.err.println("[SYNC] Server error: " + e.getMessage());
        }
    }

    private void handleSyncRequest(Socket socket) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Read the sync request sent by the client (another node)
            SyncRequest request = (SyncRequest) in.readObject();

            // If the command is "SYNC_REQUEST", respond with a list of files
            if ("SYNC_REQUEST".equals(request.getCommand())) {
                // Get all file paths stored in this node
                List<String> allFiles = node.getAllFiles();

                // Send the list back to the client
                out.writeObject(allFiles);
            }
        } catch (Exception e) {
            // Handle exceptions such as IO errors or class casting errors
            System.err.println("[SYNC] Handling error: " + e.getMessage());
        }
    }

}