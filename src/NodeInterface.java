import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    boolean ping() throws RemoteException;//بتفحص إذا الـNode حي أو متصل
    boolean isNodeAlive() throws RemoteException;//بتقول إذا الـNode شغال أو لأ
}


