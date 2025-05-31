import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DistributedLockManagerImpl extends UnicastRemoteObject implements DistributedLockManager {
    private final Map<String, ReentrantLock> fileLocks = new HashMap<>();
    private final Map<String, String> lockOwners = new HashMap<>();
    private final long lockTimeout;

    public DistributedLockManagerImpl(long lockTimeout) throws RemoteException {
        this.lockTimeout = lockTimeout;

    }
   //محاولة حجز قفل على ملف معين من قبل مستخدم
    @Override
    public synchronized boolean acquireLock(String filePath, String userId) throws RemoteException {
        ReentrantLock lock = fileLocks.computeIfAbsent(filePath, k -> new ReentrantLock(true));

        try {
            boolean acquired = lock.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
            if (acquired) {
                lockOwners.put(filePath, userId);
                System.out.printf("[LOCK] Lock acquired for %s by %s%n", filePath, userId);
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[LOCK] Failed to acquire lock for %s by %s%n", filePath, userId);
        return false;
    }
   //تحرير القفل إذا كان المستخدم هو مالكه
    @Override
    public synchronized boolean releaseLock(String filePath, String userId) throws RemoteException {
        ReentrantLock lock = fileLocks.get(filePath);
        if (lock == null ) {
            return false;
        }

        if (userId.equals(lockOwners.get(filePath))) {
            lock.unlock();
            lockOwners.remove(filePath);
            System.out.printf("[LOCK] Lock released for %s by %s%n", filePath, userId);
            return true;
        }
        return false;
    }
   //إرجاع هوية المستخدم الذي يملك القفل حالياً
    @Override
    public synchronized String getLockOwner(String filePath)  {
        return lockOwners.get(filePath);
    }
}



