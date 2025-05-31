import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
public class NodeImpl extends UnicastRemoteObject implements NodeInterface {
    private final Node node;
    private volatile long lastResponseTime = System.currentTimeMillis();
    public NodeImpl(Node node) throws RemoteException {
        super();
        this.node = node;
    }
    public boolean ping() throws RemoteException {
        try {
            lastResponseTime = System.currentTimeMillis(); // Update last response time
            return true;
        } catch (Exception e) {
            throw new RemoteException("Error during ping", e); // Enhanced error handling
        }
    }
    @Override
    public boolean isNodeAlive() throws RemoteException {
        return node.isNodeAlive();
    }
}

