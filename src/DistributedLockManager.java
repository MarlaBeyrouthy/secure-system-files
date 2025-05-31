import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DistributedLockManager extends Remote {
    boolean acquireLock(String filePath, String userId) throws RemoteException;
    boolean releaseLock(String filePath, String userId) throws RemoteException;
    String getLockOwner(String filePath) throws RemoteException;
}

