import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
// يمثل سيرفر (خادم) مسؤول عن استقبال طلبات مزامنة (SyncRequests) عبر الشبكة، ويعمل على منفذ (port) محدد، ويتعامل مع عقدة (Node) معينة
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
            System.out.println("[SYNC] SyncServer started for " + node.getName() + " on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleSyncRequest(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[SYNC] Server error: " + e.getMessage());
        }
    }
    private void handleSyncRequest(Socket socket) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            SyncRequest request = (SyncRequest) in.readObject();

            if ("SYNC_REQUEST".equals(request.getCommand())) {
                // إرجاع قائمة جميع الملفات
                List<String> allFiles = node.getAllFiles();
                out.writeObject(allFiles);
            }
        } catch (Exception e) {
            System.err.println("[SYNC] Handling error: " + e.getMessage());
        }
    }
}