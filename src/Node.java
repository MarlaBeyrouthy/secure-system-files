import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Node {
    private final String nodeName;
    private final Map<String, File> departmentFolders;//قفل قراءة/كتابة لضمان التزامن
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private volatile boolean isAlive = true;
    private final List<String> departments;
    private volatile long lastResponseTime = System.currentTimeMillis();//خر وقت استجابت فيه هذه العقدة
    private final String nodePath;
    private final File backupDir;
    private final Map<String, FileMetadata> fileMetadataMap = new ConcurrentHashMap<>();
    private static final Logger nodeLogger = Logger.getLogger("NodeLogger");
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final Coordinator coordinator;


    public Node(String nodeName, List<String> departments, String basePath, Coordinator coordinator) {
        this.nodeName = nodeName;
        this.departments = departments;
        this.nodePath = basePath;
        this.backupDir = new File(nodePath + "/backups");
        this.backupDir.mkdirs();
        this.coordinator = coordinator;


        this.departmentFolders = new HashMap<>();
        for (String dept : departments) {
            File folder = new File(nodePath + "/" + dept);
            folder.mkdirs();
            departmentFolders.put(dept.toLowerCase(), folder);
        }

        try {
            nodeLogger.addHandler(new ConsoleHandler());
        } catch (SecurityException e) {
            System.err.println("Failed to setup node logger: " + e.getMessage());
        }
    }
    public String addFile(String department, String fileName, String content, int expectedVersion, String userId)  {
        rwLock.writeLock().lock();
        requestCount.incrementAndGet();
        System.out.printf("[%s] %s STARTED | Active: %d%n",
                nodeName, Thread.currentThread().getName(), requestCount.incrementAndGet());

        try {//يتحقق أن الملف ليس مقفلًا من قبل مستخدم آخر
            if (!coordinator.checkLock(department + "/" + fileName, userId)) {
                return "Error: File is locked by another user";
            }

            System.out.printf("[NODE-%s] Adding file: %s/%s (v%d) by %s%n",
                    this.nodeName, department, fileName, expectedVersion, userId);

            File dir = departmentFolders.get(department.toLowerCase());
            if (dir == null) return "Invalid department";

            File file = new File(dir, fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                String fileKey = department + "/" + fileName;
                fileMetadataMap.put(fileKey,
                        new FileMetadata(expectedVersion + 1, System.currentTimeMillis()));

                return "Success";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            System.out.printf("[%s] %s FINISHED | Active: %d%n",
                    nodeName, Thread.currentThread().getName(), requestCount.decrementAndGet());
            requestCount.decrementAndGet();
            rwLock.writeLock().unlock();
        }
    }

    public String deleteFile(String department, String fileName, String userId) throws RemoteException {
        rwLock.writeLock().lock();
        requestCount.incrementAndGet(); // 🔼 تبدأ العدّ

        try {
            if (!coordinator.checkLock(department + "/" + fileName, userId)) {
                return "Error: File is locked by another user";
            }

            System.out.printf("[NODE-%s] Deleting file: %s/%s by %s%n",
                    nodeName, department, fileName, userId);

            String fileKey = department + "/" + fileName;
            File file = new File(departmentFolders.get(department), fileName);

            if (file.exists() && file.delete()) {
                fileMetadataMap.remove(fileKey);
                return "Success";
            }
            return "Delete failed";

        } finally {
            requestCount.decrementAndGet();
            rwLock.writeLock().unlock();
        }
    }

    public String restoreFromBackup(String department, String fileName, String userId) throws RemoteException {
        rwLock.writeLock().lock();
        try {
            File backupFile = new File(backupDir, department + "_" + fileName + ".bak");
            nodeLogger.info(String.format("[%s] Restoring %s/%s by %s",
                    nodeName, department, fileName, userId));

            if (!backupFile.exists()) {
                return "Backup not found";
            }

            try (Scanner scanner = new Scanner(backupFile)) {
                String content = scanner.useDelimiter("\\Z").next();
                return this.addFile(department, fileName, content, 0, userId);
            }
        } catch (Exception e) {
            return "Restore failed: " + e.getMessage();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public void cleanupOldBackups(int daysToKeep) {
        rwLock.writeLock().lock();
        try {
            File[] backups = backupDir.listFiles();
            if (backups != null) {
                long cutoff = System.currentTimeMillis() - (daysToKeep * 86400000L);
                for (File backup : backups) {
                    if (backup.lastModified() < cutoff) {
                        if (backup.delete()) {
                            nodeLogger.info("Deleted old backup: " + backup.getName());
                        }
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public String getFileContent(String department, String fileName) throws RemoteException, FileNotFoundException {
        // الحصول على قفل القراءة
        rwLock.readLock().lock();
        requestCount.incrementAndGet(); // 🔼 تبدأ العدّ
        try {
            // زيادة عداد الطلبات النشطة
            int currentCount = requestCount.incrementAndGet();
            System.out.printf("[NODE-%s] REQ-START | %s/%s | Active: %d%n",
                    nodeName, department, fileName, currentCount);

            // تنفيذ العملية
            File file = new File(departmentFolders.get(department.toLowerCase()), fileName);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }

            try (Scanner scanner = new Scanner(file)) {
                String content = scanner.useDelimiter("\\Z").next();
                return content;
            }
        } finally {
            // تقليل عداد الطلبات النشطة
            requestCount.decrementAndGet();
            // تحرير قفل القراءة
            rwLock.readLock().unlock();
            System.out.printf("[NODE-%s] REQ-END | %s/%s | Active: %d%n",
                    nodeName, department, fileName, requestCount.get());
        }
    }
    public String updateFile(String department, String fileName,String content, int expectedVersion, String userId)  {
        rwLock.writeLock().lock();
        requestCount.incrementAndGet(); // 🔼 تبدأ العدّ

        try {
            if (!coordinator.checkLock(department + "/" + fileName, userId)) {
                return "Error: File is locked by another user";
            }

            File dir = departmentFolders.get(department.toLowerCase());
            if (dir == null) return "Invalid department";

            File file = new File(dir, fileName);
            if (!file.exists()) return "FILE_NOT_FOUND";

            int currentVersion = getFileVersion(department, fileName);
            if (currentVersion != expectedVersion) {
                System.out.printf("[NODE-%s] VERSION_CONFLICT %s/%s: Client v%d, Actual v%d%n",
                        nodeName, department, fileName, expectedVersion, currentVersion);
                return "VERSION_CONFLICT";
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                fileMetadataMap.put(department + "/" + fileName,
                        new FileMetadata(expectedVersion + 1, System.currentTimeMillis()));
                return "Success";
            }

        } catch (Exception e) {
            e.printStackTrace(); // Helps during testing
            CoordinatorImpl.logger.severe("Exception in updateFile: " + e); // Better logging
            return "Error: Exception occurred during update";

        }
        finally {
            requestCount.decrementAndGet();
            rwLock.writeLock().unlock();
        }
    }
    public int getFileVersion(String department, String fileName) {
        String key = department + "/" + fileName;
        FileMetadata metadata = fileMetadataMap.get(key);
        return metadata != null ? metadata.version : 0;
    }
    public boolean isNodeAlive() {
        return isAlive;
    }
    public void setNodeAlive(boolean alive) {
        isAlive = alive;
    }
    public String getName() {
        return nodeName;
    }
    public List<String> getAllFiles() {
        rwLock.readLock().lock();
        try {
            List<String> files = new ArrayList<>();
            for (String dept : departmentFolders.keySet()) {
                File[] deptFiles = departmentFolders.get(dept).listFiles();
                if (deptFiles != null) {
                    for (File file : deptFiles) {
                        files.add(dept + "/" + file.getName());
                    }
                }
            }
            return files;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public long getLastResponseTime() {
        return lastResponseTime;
    }
    public int getActiveRequestsCount() {
        return requestCount.get();
    }
    public boolean ping() throws RemoteException {
        lastResponseTime = System.currentTimeMillis();
        return true;
    }// يُستخدم من HeartbeatChecker للتأكد إن العقدة حية
    public boolean hasFile(String department, String fileName) throws RemoteException {
        rwLock.readLock().lock();// اكتساب قفل قراءة
        try {
            File dir = departmentFolders.get(department.toLowerCase());
            if (dir == null) return false;

            File file = new File(dir, fileName);
            return file.exists() && file.isFile();
        } finally {
            rwLock.readLock().unlock();
        }
    }// يتحقق هل الملف موجود فعليًا في مجلد القسم.
   //had el backup w2t el delete ou update

    private static class FileMetadata implements Serializable {
        final int version;
        final long lastModified;


        FileMetadata(int version, long lastModified) {
            this.version = version;
            this.lastModified = lastModified;


        }
    }

    //yete gouzemgor yes yerpvor idafe enem file backup ounenam as tabe3 bstkhdm payts yete chem ousergor
    //asi comment gnem yev verine bstkhdm(backupfile)yev gertam coordinatorimp handlefileopertaion deghe mekad cooment yeghadz try ga ador vrayen ge hanem commente
    //yev mouyse ge srpem yed barz ge tarna miyan deletin yev updatin
    public String backupFileWithIncrement(String department, String fileName, String content) throws RemoteException {
        rwLock.writeLock().lock();
        try {
            // Create department-specific backup directory
            File deptBackupDir = new File(backupDir, department);
            deptBackupDir.mkdirs();

            // Find the next available backup number
            int backupNumber = 1;
            Pattern pattern = Pattern.compile(Pattern.quote(fileName) + "(\\d+)\\.bak$");
            for (File f : deptBackupDir.listFiles()) {
                Matcher m = pattern.matcher(f.getName());
                if (m.find()) {
                    int currentNum = Integer.parseInt(m.group(1));
                    backupNumber = Math.max(backupNumber, currentNum + 1);
                }
            }

            // Create the new backup file
            String backupName = fileName + backupNumber + ".bak";
            File backupFile = new File(deptBackupDir, backupName);

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write(content);
                return "Backup " + backupNumber + " created successfully";
            }
        } catch (Exception e) {
            return "Backup failed: " + e.getMessage();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public String backupFile(String department, String fileName, String content) throws RemoteException {
        rwLock.writeLock().lock();
        try {
            String backupFileName = department + "_" + fileName + ".bak";
            File backupFile = new File(backupDir, backupFileName);

            // التحقق من وجود نسخة احتياطية حديثة (خلال آخر 5 دقائق)
            if (backupFile.exists() &&
                    System.currentTimeMillis() - backupFile.lastModified() < 80000) {
                return "Recent backup already exists";
            }

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write(content);
                return "Backup successful";
            }
        } catch (Exception e) {
            return "Backup failed: " + e.getMessage();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}












