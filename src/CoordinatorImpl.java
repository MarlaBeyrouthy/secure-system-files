import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class CoordinatorImpl extends UnicastRemoteObject implements Coordinator {
    private final Map<String, User> users = new HashMap<>();
    private final List<Node> nodes = new ArrayList<>();//قائمة بالعُقَد المشاركة في النظام.
    private final ReentrantLock coordinatorLock = new ReentrantLock();//قفل لتأمين الوصول المتزامن تحكم في التزامن
    private final LoadBalancer loadBalancer;
    private String lastError;
    private final DistributedLockManager lockManager;//لإدارة الأقفال على الملفات الموزعة
    private static final String USERS_FILE = "users.dat";
    static final Logger logger = Logger.getLogger("CoordinatorLogger");//tsjil el a7das fi mlfat
    private NodeSyncService syncService;
    private String lastServerResponse;

    public CoordinatorImpl() throws RemoteException {
        super();
        this.loadBalancer = new LoadBalancer(nodes);
        this.lockManager = new DistributedLockManagerImpl(30000); // 30 second timeout
        loadUsers();
        setupLogger();
        this.syncService = new NodeSyncService(nodes, 0, 0, 0);  //sa3a 12 blel
    }
    private void setupLogger() {
        try {
            logger.setUseParentHandlers(false);
            FileHandler fileHandler = new FileHandler("coordinator_operations.log");
            logger.addHandler(fileHandler);
            logger.addHandler(new ConsoleHandler());
        } catch (IOException e) {
            System.err.println("Failed to setup coordinator logger: " + e.getMessage());
        }
    }
    //tsjil moustkhdm jdid bl system bystkhdem coordinatorlockly2men tzamon
    @Override
    public synchronized void registerUser(User user) throws RemoteException {
        coordinatorLock.lock();//to prevent race conditions.
        try {
            //Checks if the user exists.

            if (users.containsKey(user.getUsername())) {
                logger.warning("Registration failed - Username already exists: " + user.getUsername());
                return;
            }
            users.put(user.getUsername(), user);
            saveUsers();
            logger.info("User registered successfully: " + user.getUsername());
        } finally {
            coordinatorLock.unlock();
        }
    }
    //insha2 token frid lluser 3nd el tsjil
    @Override
    public synchronized String generateToken(String username) throws RemoteException {
        coordinatorLock.lock();
        try {
            if (users.containsKey(username)) {
                String token = username + "_TOKEN_" + System.currentTimeMillis();
                users.get(username).setToken(token);
                logger.info("Token generated for user: " + username);
                return token;
            }
            logger.warning("Token generation failed - User not found: " + username);
            return null;
        } finally {
            coordinatorLock.unlock();
        }
    }
    //idafet 3kde jdide llnizam
    @Override
    public synchronized void addNode(Node node) throws RemoteException {
        coordinatorLock.lock();
        try {
            nodes.add(node);
            loadBalancer.addNode(node);
            logger.info("Node added: " + node.getName());
        } finally {
            coordinatorLock.unlock();
        }
    }
   //tnfiz el 3liyat byrtbt bl lockmanger l7jz kfl & loadbalancer lekhtiyar 3kde node ltnfix el 3mliyat & rollbakc listirja3 3nd el fshl

    public synchronized boolean handleFileOperation(String token, String username, String operation, String department, String fileName, String content) throws RemoteException, IOException {

        // Authentication and validation
        User user = users.get(username);
        if (user == null || !token.equals(user.getToken())) {
            logger.warning("Authentication failed for user: " + username);
            return false;
        }



        if (user.getRole() == Role.EMPLOYEE && !operation.equalsIgnoreCase("read") && !department.equalsIgnoreCase(user.getDepartment())) {
            logger.warning("Permission denied for user: " + username + " on department: " + department);
            return false;
        }

        if (!Arrays.asList("development", "design", "qa").contains(department.toLowerCase())) {
            logger.warning("Invalid department: " + department);
            return false;
        }

        if (nodes.isEmpty()) {
            logger.severe("No available nodes for file operation");
            return false;
        }

        String filePath = department + "/" + fileName;

        // Acquire distributed lock
        if (!lockManager.acquireLock(filePath, username)) {
            logger.warning("Lock acquisition failed for: " + filePath + " by user: " + username);
            return false;
        }





        //backup incremnted
             try {
            // Backup for new files, updates, and deletes
            if (operation.equalsIgnoreCase("add") || operation.equalsIgnoreCase("update") || operation.equalsIgnoreCase("delete")) {
                String contentToBackup = null;

                if (operation.equalsIgnoreCase("add")) {
                    contentToBackup = content; // Use the new content for backup
                } else {
                    contentToBackup = getFileContentFromAnyNode(department, fileName); // Get current content for update/delete
                }

                if (contentToBackup != null) {
                    logger.info("Creating backup for: " + filePath + " by user: " + username);

                    // Select any available node for backup
                    Node backupNode = null;
                    for (Node node : nodes) {
                        if (node.isNodeAlive()) {
                            backupNode = node;
                            break;
                        }
                    }

                    if (backupNode != null) {
                        // Modified backup call that creates incremental backups
                        String backupResult = backupNode.backupFileWithIncrement(department, fileName, contentToBackup);
                        logger.info("Backup result from " + backupNode.getName() + ": " + backupResult);

                        // Changed success check to match new backup response
                        if (!backupResult.startsWith("Backup")) {
                            logger.warning("Backup failed on " + backupNode.getName());
                            return false;
                        }
                    } else {
                        logger.warning("No available nodes for backup");
                        return false;
                    }
                }
            }

            // Get expected version from reference node (load-balanced)
            int expectedVersion = 0;
            try {
                Node referenceNode = loadBalancer.getNextAvailableNode();
                expectedVersion = referenceNode.getFileVersion(department, fileName);
                logger.info("[LB] Version check node: " + referenceNode.getName());
            } catch (Exception e) {
                // File doesn't exist yet
            }

            boolean globalResult = true;
            List<Node> successfulNodes = new ArrayList<>();

            // Select primary node using load balancer
            Node primaryNode = loadBalancer.getNextAvailableNode();
            logger.info("[LB] Selected " + primaryNode.getName() + " as primary for " + operation +
                    " | Load: " + loadBalancer.getNodeLoad(primaryNode));

            if (primaryNode == null) {
                logger.severe("No available nodes for file operation");
                return false;
            }

            // Perform operation on primary node
            try {
                boolean primaryResult = false;
                switch (operation.toLowerCase()) {
                    case "add":
                        primaryResult = "Success".equals(
                                primaryNode.addFile(department, fileName, content, expectedVersion, username));
                        break;
                    case "update":
                        primaryResult = "Success".equals(
                                primaryNode.updateFile(department, fileName, content, expectedVersion, username));
                        break;
                    case "delete":
                        primaryResult = "Success".equals(
                                primaryNode.deleteFile(department, fileName, username));
                        break;
                    default:
                        return false;
                }

                if (primaryResult) {
                    successfulNodes.add(primaryNode);



                    // Replicate to other nodes
                    for (Node replicaNode : nodes) {
                        if (!replicaNode.equals(primaryNode) && replicaNode.isNodeAlive()) {
                            try {
                                boolean replicaResult = false;
                                switch (operation.toLowerCase()) {
                                    case "add":
                                        replicaResult = "Success".equals(
                                                replicaNode.addFile(department, fileName, content, expectedVersion, username));
                                        break;
                                    case "update":
                                        replicaResult = "Success".equals(
                                                replicaNode.updateFile(department, fileName, content, expectedVersion, username));
                                        break;
                                    case "delete":
                                        replicaResult = "Success".equals(
                                                replicaNode.deleteFile(department, fileName, username));
                                        break;
                                }

                                if (replicaResult) {
                                    successfulNodes.add(replicaNode);
                                } else {
                                    globalResult = false;
                                    logger.warning("Replication failed on " + replicaNode.getName());
                                    logger.info("Node operation result: " + primaryResult);
                                }
                            } catch (Exception e) {
                                globalResult = false;
                                logger.severe("Replication error on " + replicaNode.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                } else {
                    globalResult = false;
                    logger.severe("Primary operation failed on " + primaryNode.getName());
                }
            } catch (Exception e) {
                globalResult = false;
                logger.severe("Operation failed on primary node " + primaryNode.getName() + ": " + e.getMessage());
            }

            // Rollback if any replication failed
            if (!globalResult) {
                rollbackOperation(operation, department, fileName, successfulNodes, username);
                return false;
            }

            return true;
        }

        finally {

            lockManager.releaseLock(filePath, username);
            logger.info("Lock released for: " + filePath + " by user: " + username);
        }
    }
    //الحصول على محتوى ملف من أي عقدة متاحة
    public synchronized String requestFileFromNode(String token, String username,String department, String fileName) throws RemoteException {

        logger.info("File request initiated by " + username + " for: " + department + "/" + fileName);

        User user = users.get(username);
        if (user == null || !token.equals(user.getToken())) {
            logger.warning("Authentication failed for file request by: " + username);
            return "Authentication failed";
        }

        //asi arach trereyink vor chi grnar gartal ourish ksmeya iza asang chouzets ge hanenk
//        if (user.getRole() == Role.EMPLOYEE && !department.equalsIgnoreCase(user.getDepartment())) {
//            logger.warning("Permission denied for file request by: " + username);
//            return "Permission denied";
//        }

        int retries = 0;
        while (retries < nodes.size()) {
            Node node = null;
            try {
                node = loadBalancer.getNextAvailableNode();
                logger.info("Trying node: " + node.getName() + " (Attempt: " + (retries + 1) + ")");

                String content = node.getFileContent(department, fileName);
                if (content != null) {
                    logger.info("File retrieved successfully from node: " + node.getName());
                    return content;
                }
            } catch (FileNotFoundException e) {
                logger.warning("File not found in node: " + (node != null ? node.getName() : "null"));
                return "File not found";
            } catch (Exception e) {
                logger.warning("Node error: " + e.getMessage());
                if (node != null) {
                    loadBalancer.nodeFailed(node);
                }
                retries++;
            }
        }
        logger.severe("All nodes failed for file request by: " + username);
        return "Error: All nodes failed";
    }
    @Override
    public synchronized User getUser(String username) throws RemoteException {
        coordinatorLock.lock();
        try {
            return users.get(username);
        } finally {
            coordinatorLock.unlock();
        }
    }
    @Override
    public synchronized List<User> getAllUsers(String token) throws RemoteException {
        coordinatorLock.lock();
        try {
            User requester = users.values().stream()
                    .filter(u -> token.equals(u.getToken()))
                    .findFirst()
                    .orElseThrow(() -> new RemoteException("Invalid token"));

            if (requester.getRole() != Role.MANAGER) {
                throw new RemoteException("Only managers can view all users");
            }

            return users.values().stream()
                    .map(user -> new User(
                            user.getUsername(),
                            user.getPassword(),
                            user.getRole(),
                            user.getDepartment()))
                    .collect(Collectors.toList());
        } finally {
            coordinatorLock.unlock();
        }
    }
   // t7kok eno el user 3ndo kfl 3l mlf
    public boolean checkLock(String filePath, String userId) throws RemoteException {
        String lockOwner = lockManager.getLockOwner(filePath);
        return lockOwner != null && lockOwner.equals(userId);
    }
// فقط إذا كان نفس المستخدم

   //ijad ou kira2et mlf mn ay 3kde nshta
    private String getFileContentFromAnyNode(String department, String fileName) {
        for (Node node : nodes) {
            try {
                if (node.hasFile(department, fileName)) {
                    String content = node.getFileContent(department, fileName);
                    logger.info("File content retrieved from node: " + node.getName());
                    return content;
                }
            } catch (Exception e) {
                logger.warning("Error checking file on node " + node.getName() + ": " + e.getMessage());
            }
        }
        return null;
    }
    //estirja3 nskha e7tiyateiye mn mlf iza fshl el t3dil aw el 7zf
    private void rollbackOperation(String operation, String department, String fileName, List<Node> successfulNodes, String userId) {
        logger.warning("Rollback initiated: " + operation + " on " + department + "/" + fileName);
        for (Node node : successfulNodes) {
            try {
                node.restoreFromBackup(department, fileName, userId);
            } catch (RemoteException e) {
                logger.severe("Rollback failed on " + node.getName());
            }
        }
    }
    private void saveUsers() {
        coordinatorLock.lock();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            out.writeObject(users);
            logger.info("Users data saved successfully");
        } catch (IOException e) {
            logger.severe("Failed to save users: " + e.getMessage());
        } finally {
            coordinatorLock.unlock();
        }
    }
    private void loadUsers() {
        coordinatorLock.lock();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
            @SuppressWarnings("unchecked")
            Map<String, User> loadedUsers = (Map<String, User>) in.readObject();
            users.putAll(loadedUsers);
            logger.info("Users data loaded successfully. Total users: " + users.size());
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Failed to load users: " + e.getMessage());
        } finally {
            coordinatorLock.unlock();
        }
    }
    public synchronized String getFileLockStatus(String filePath) throws RemoteException {
        String lockOwner = lockManager.getLockOwner(filePath);
        if (lockOwner != null) {
            return "File is locked by: " + lockOwner;
        }
        return "File is available";
    }
    public String checkLockStatus(String filePath) throws RemoteException {
        return lockManager.getLockOwner(filePath);
    }
    @Override
    public boolean acquireLock(String filePath, String username) throws RemoteException {
        return lockManager.acquireLock(filePath, username);
    }
    @Override
    public boolean releaseLock(String filePath, String username) throws RemoteException {
        return lockManager.releaseLock(filePath, username);
    }

}


























