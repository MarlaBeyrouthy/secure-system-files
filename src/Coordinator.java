import java.io.IOException;
import java.util.List;
//TODO WORKING COORCTLTY
/*interface Coordinator {
    void registerUser(User user);
    String generateToken(String username);
    boolean authenticate(String username, String token);
    void addNode(Node node);
    boolean handleFileOperation(String token, String username, String operation, String department, String fileName, String content) throws IOException;
    String requestFileFromNode(String token, String username, String department, String fileName);
    User getUser(String username);

    List<Node> getNodes();}*/



import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Coordinator extends Remote {
    void registerUser(User user) throws RemoteException;
    String generateToken(String username) throws RemoteException;
    void addNode(Node node) throws RemoteException;
    //method t7akok mn al sla7iyat
    boolean handleFileOperation(String token, String username, String operation, String department, String fileName, String content)throws RemoteException, IOException;
    String requestFileFromNode(String token, String username,String department, String fileName) throws RemoteException;
    User getUser(String username) throws RemoteException;
    List<User> getAllUsers(String token) throws RemoteException;
    boolean checkLock(String filePath, String userId) throws RemoteException;
    String getFileLockStatus(String filePath)throws RemoteException;
    String checkLockStatus(String filePath) throws RemoteException;
    boolean acquireLock(String filePath, String username) throws RemoteException;
    boolean releaseLock(String filePath, String username) throws RemoteException;

}



