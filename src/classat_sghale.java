//TODO WOKING VERY COORCTLY Clinet class
/*
public class Client {
    private static final int PORT = 5000; // منفذ ثابت
    private static String token = null;
    private static Role userRole = null;
    private static String currentUser = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter server IP [localhost]: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "localhost";

        handleAuthentication(scanner, ip);
        handleOperations(scanner, ip);
    }

    private static void handleAuthentication(Scanner scanner, String ip) {
        while (token == null) {
            System.out.println("\n🔐 Authentication");
            System.out.println("1. Register\n2. Login\n3. Exit");
            System.out.print("Choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice == 3) System.exit(0);

                try (Socket socket = new Socket(ip, PORT)) {
                    Request request = createAuthRequest(scanner, choice);
                    if (request == null) continue;

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(request);
                    out.flush(); // Ensure data is sent

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Object response = in.readObject();

                    if (response instanceof String) {
                        processAuthResponse((String) response);
                    } else {
                        System.err.println("🚫 Unexpected response type from server");
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Please enter a valid number");
            } catch (Exception e) {
                System.err.println("🚫 Connection error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static Request createAuthRequest(Scanner scanner, int choice) {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (choice == 1) {
            System.out.print("Role (1. Manager / 2. Employee): ");
            Role role = (Integer.parseInt(scanner.nextLine()) == 1 ? Role.MANAGER : Role.EMPLOYEE);

            String department = null;
            if (role == Role.EMPLOYEE) {
                System.out.print("Department (development/design/qa): ");
                department = scanner.nextLine();
            }

            return new Request(OperationType.REGISTER, new User(username, password, role, department));
        }

        return new Request(OperationType.LOGIN, new User(username, password, null, null));
    }

    private static void processAuthResponse(String response) {
        if (response.startsWith("LOGIN_SUCCESS:")) {
            String[] parts = response.split(":");
            token = parts[1];
            currentUser = parts[2];
            userRole = Role.valueOf(parts[3]);
            System.out.println("🎉 Login successful as " + currentUser + " [" + userRole + "]");
        }
    }

        private static void handleOperations(Scanner scanner, String ip) {
        while (true) {
            printMenu();
            int choice = Integer.parseInt(scanner.nextLine());

            if (choice == (userRole == Role.MANAGER ? 6 : 5)) break;

            try {
                Socket socket = new Socket(ip, PORT);
                Request request = createOperationRequest(scanner, choice);

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(request);

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Server response: " + in.readObject());

            } catch (Exception e) {
                System.err.println("🚫 Operation failed: " + e.getMessage());
            }
        }
    }

    private static Request createOperationRequest(Scanner scanner, int choice) {
        String operationType;
        String department = null;
        String fileName = null;
        String content = null;

        switch (choice) {
            case 1: // Add File
                operationType = "add";
                System.out.print("Enter department (development/design/qa): ");
                department = scanner.nextLine();
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();
                System.out.print("Enter file content: ");
                content = scanner.nextLine();
                break;

            case 2: // Update File
                operationType = "update";
                System.out.print("Enter department (development/design/qa): ");
                department = scanner.nextLine();
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();
                System.out.print("Enter new content: ");
                content = scanner.nextLine();
                break;

            case 3: // Delete File
                operationType = "delete";
                System.out.print("Enter department (development/design/qa): ");
                department = scanner.nextLine();
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();
                break;

            case 4: // Request File
                operationType = "request";
                System.out.print("Enter department (development/design/qa): ");
                department = scanner.nextLine();
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();
                break;

            case 5: // Register User (Manager only)
                if (userRole != Role.MANAGER) {
                    System.out.println("❌ Unauthorized access!");
                    return null;
                }
                System.out.print("Enter new username: ");
                String newUsername = scanner.nextLine();
                System.out.print("Enter new password: ");
                String newPassword = scanner.nextLine();
                System.out.print("Enter role (1. Manager / 2. Employee): ");
                Role newRole = (Integer.parseInt(scanner.nextLine()) == 1) ? Role.MANAGER : Role.EMPLOYEE;

                String newDept = null;
                if (newRole == Role.EMPLOYEE) {
                    System.out.print("Enter department (development/design/qa): ");
                    newDept = scanner.nextLine();
                }

                return new Request(OperationType.REGISTER, new User(newUsername, newPassword, newRole, newDept));

            default:
                System.out.println("❌ Invalid choice");
                return null;
        }

        return new Request(
                (choice == 4) ? OperationType.FILE_REQUEST : OperationType.FILE_OPERATION,
                token,
                currentUser,
                operationType,
                department,
                fileName,
                content
        );
    }

    private static void printMenu() {
        System.out.println("\n📋 Main Menu");
        System.out.println("1. Add File");
        System.out.println("2. Update File");
        System.out.println("3. Delete File");
        System.out.println("4. Request File");
        if (userRole == Role.MANAGER) {
            System.out.println("5. Register User");
        }
        System.out.println((userRole == Role.MANAGER ? "6" : "5") + ". Exit");
        System.out.print("Choice: ");
    }
}
*/

//TODO WORKING CORRCTLY Server class
/*
public class Server {
    private static final int PORT = 5000; // منفذ ثابت

    public static void main(String[] args) {
        Coordinator coordinator = new CoordinatorImpl();
        setupDefaultEnvironment(coordinator);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket, coordinator)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
        }
    }

    private static void setupDefaultEnvironment(Coordinator coordinator) {
        String tempDir = "C:/distributed_system/";

        // Ensure directory exists
        new File(tempDir).mkdirs();

        coordinator.registerUser(new User("admin", "admin123", Role.MANAGER, null));

        List<String> departments = Arrays.asList("development", "design", "qa");
        for (int i = 1; i <= 3; i++) {
            String nodePath = tempDir + "/node" + i;
            new File(nodePath).mkdirs(); // Create node folder if it doesn't exist
            coordinator.addNode(new Node("Node" + i, departments, nodePath));
        }
    }

    private static void handleClient(Socket socket, Coordinator coordinator) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            // Add this to verify the incoming object
            Object received = in.readObject();
            if (!(received instanceof Request)) {
                out.writeObject("ERROR: Invalid request format");
                return;
            }

            Request request = (Request) received;
            String response = new OperationHandler(coordinator).handle(request);
            out.writeObject(response);

        } catch (Exception e) {
            System.err.println("⚠️ Client handling error: " + e.getMessage());
            try {
                // Try to send error message back to client
                socket.getOutputStream().write(("ERROR: " + e.getMessage()).getBytes());
            } catch (IOException ex) {
                System.err.println("Failed to send error to client: " + ex.getMessage());
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }
    }}*/

//TODO WOKING VERY COORCTLY CoordinatorImp class
/*public class CoordinatorImpl extends UnicastRemoteObject implements Coordinator {
    private final Map<String, User> users = new HashMap<>();
    private final List<Node> nodes = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final LoadBalancer loadBalancer = new LoadBalancer(nodes);

    private static final String USERS_FILE = "users.dat";
    public User currentUser;

    private static final String BACKUP_LOG = "backup_log.txt";
    private int currentNodeIndex = 0;
    private static final Logger backupLogger = Logger.getLogger("BackupLogger");

    private NodeSyncService syncService;
    public CoordinatorImpl() throws RemoteException {
        super();
        loadUsers();
        new NodeSyncService(nodes, 23, 0, 45); // Pass 0 or any intended port
        try {
            FileHandler fileHandler = new FileHandler("backup_operations.log");
            backupLogger.addHandler(fileHandler);
            backupLogger.addHandler(new ConsoleHandler()); // This sends logs to console
        } catch (IOException e) {
            System.err.println("Failed to setup backup logger: " + e.getMessage());
        }
    }
    @Override
    public synchronized void registerUser(User user) throws RemoteException {
        if (users.containsKey(user.getUsername())) {
            System.out.println("[INFO] User '" + user.getUsername() + "' already exists");
            return;
        }
        users.put(user.getUsername(), user);
        saveUsers();
    }
    @Override
    public synchronized String generateToken(String username) throws RemoteException {
        if (users.containsKey(username)) {
            String token = username + "_TOKEN_" + System.currentTimeMillis();
            users.get(username).setToken(token);
            return token;
        }
        return null;
    }
    @Override
    public synchronized void addNode(Node node) throws RemoteException {
        nodes.add(node);
    }
    //bihad el tabe3 3m nstkhdm 3mliyat 7zf idafe.. ou fi 7al el fshl mnstkhdm el rollback
    @Override
    public synchronized boolean handleFileOperation(String token, String username, String operation,String department, String fileName, String content) throws RemoteException, IOException {

        // Authentication and validation checks
        User user = users.get(username);
        if (user == null || !token.equals(user.getToken())) {
            System.out.println("[Auth] Failed: Invalid token or user not found");
            return false;
        }

        if (user.getRole() == Role.EMPLOYEE && !department.equalsIgnoreCase(user.getDepartment())) {
            System.out.println("[Permission] Denied: Employee cannot access this department");
            return false;
        }

        if (!Arrays.asList("development", "design", "qa").contains(department.toLowerCase())) {
            System.out.println("[Error] Invalid department");
            return false;
        }

        if (nodes.isEmpty()) {
            System.out.println("[Error] No available nodes");
            return false;
        }

        // Backup before update or delete
        if (operation.equalsIgnoreCase("update") || operation.equalsIgnoreCase("delete")) {
            String existingContent = getFileContentFromAnyNode(department, fileName);
            if (existingContent != null) {
                backupLogger.info(String.format(
                        "Creating backup for %s/%s (operation: %s by user: %s)",
                        department, fileName, operation, username));

                for (Node node : nodes) {
                    if (node.isNodeAlive()) {
                        String backupResult = node.backupFile(department, fileName, existingContent);
                        backupLogger.info(String.format(
                                "Backup result from %s: %s", node.getName(), backupResult));
                        break;
                    }
                }}}

        // Get expected version
        int expectedVersion = 0;
        try {
            Node referenceNode = nodes.get(0);
            expectedVersion = referenceNode.getFileVersion(department, fileName);
        } catch (Exception e) {
            // File doesn't exist yet
        }

        boolean globalResult = true;
        List<Node> successfulNodes = new ArrayList<>();

        // Perform operation on all nodes
        for (Node node : nodes) {
            if (!node.isNodeAlive()) {
                System.out.println("[Replication] Skipping inactive node: " + node.getName());
                continue;
            }

            try {
                boolean nodeResult = false;
                switch (operation.toLowerCase()) {
                    case "add":
                        nodeResult = "Success".equals(node.addFile(department, fileName, content, expectedVersion));
                        break;
                    case "update":
                        nodeResult = "Success".equals(node.updateFile(department, fileName, content, expectedVersion));
                        break;
                    case "delete":
                        nodeResult = "Success".equals(node.deleteFile(department, fileName));
                        break;
                    default:
                        return false;
                }

                if (nodeResult) {
                    successfulNodes.add(node);
                } else {
                    globalResult = false;
                }
            } catch (Exception e) {
                globalResult = false;
            }
        }

        // Rollback if needed
        if (!globalResult) {
            rollbackOperation(operation, department, fileName, successfulNodes);
            return false;
        }
        return true;
    }
    private String getFileContentFromAnyNode(String department, String fileName) {
        for (Node node : nodes) {
            try {
                if (node.hasFile(department, fileName)) {
                    String content = node.getFileContent(department, fileName);
                    System.out.println("[READ] File '" + department + "/" + fileName + "' was read from " + node.getName());
                    return content;                }
            } catch (Exception e) {
                System.out.println("Error checking file on node " + node.getName() + ": " + e.getMessage());
            }
        }
        return null;
    }
    public synchronized String requestFileFromNode(String token, String username,String department, String fileName) throws RemoteException {

       System.out.println("\n[COORD] Starting request...");
    System.out.println("[LB] Current node loads: " + loadBalancer.getNodeLoads());

    User user = users.get(username);
    if (user == null || !token.equals(user.getToken())) {
        return "Authentication failed";
    }

    if (user.getRole() == Role.EMPLOYEE && !department.equalsIgnoreCase(user.getDepartment())) {
        return "Permission denied";
    }

    int startIndex = currentNodeIndex;
       int retries = 0;
       while (retries < nodes.size()) {
           Node node = null;
           try {
               node = loadBalancer.getNextAvailableNode();
               System.out.printf("[COORD] Trying node: %s (Attempt: %d)%n",
                       node.getName(), retries + 1);

               String content = node.getFileContent(department, fileName);
               if (content != null) {
                   System.out.printf("[COORD] Success from node: %s%n", node.getName());
                   return content;
               }
           } catch (FileNotFoundException e) {
               System.out.println("[COORD] File not found in this node");
               return "File not found";
           } catch (Exception e) {
               System.out.printf("[COORD] Node error: %s%n", e.getMessage());
               loadBalancer.nodeFailed(node);
               retries++;
           }
       }
       return "Error: All nodes failed";
   }
    private void rollbackOperation(String operation, String department, String fileName,List<Node> successfulNodes) {

        System.out.println("[Rollback] Starting rollback for " + operation);

        if (operation.equalsIgnoreCase("add")) {
            backupLogger.info(String.format(
                    "Initiating rollback for %s operation on %s/%s",
                    operation, department, fileName));
            // Delete added files
            for (Node node : successfulNodes) {
                String result = node.restoreFromBackup(department, fileName);
                backupLogger.info(String.format(
                        "Rollback result from %s: %s", node.getName(), result));
            }
        } else if (operation.equalsIgnoreCase("update") || operation.equalsIgnoreCase("delete")) {
            // Restore from persistent backups
            for (Node node : successfulNodes) {
                String result = node.restoreFromBackup(department, fileName);
                System.out.println("[Rollback] " + node.getName() + " result: " + result);
            }
        }
    }
    public synchronized User getUser(String username) throws RemoteException {
        return users.get(username);
    }
    @Override
    public synchronized List<User> getAllUsers(String token) throws RemoteException {
        User requester = users.values().stream()
                .filter(u -> token.equals(u.getToken()))
                .findFirst()
                .orElseThrow(() -> new RemoteException("Invalid token"));

        if (requester.getRole() != Role.MANAGER) {
            throw new RemoteException("Only managers can view all users");
        }

        List<User> userList = new ArrayList<>();
        for (User user : users.values()) {
            userList.add(new User(
                    user.getUsername(),
                    user.getPassword(),
                    user.getRole(),
                    user.getDepartment()
            ));
        }
        return userList;
    }
    private void saveUsers() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            out.writeObject(users);  // Write the users map to the file
            System.out.println("[Save] Users have been saved.");
        } catch (IOException e) {
            System.out.println("[Error] Failed to save users: " + e.getMessage());
        }
    }
    private void loadUsers() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
            Map<String, User> loadedUsers = (Map<String, User>) in.readObject();
            users.putAll(loadedUsers);  // Load the users into the current map
            System.out.println("[Load] Users have been loaded.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Error] Failed to load users: " + e.getMessage());
        }
    }
}*/

//TODO WOKING VERY COORCTLY Node class
/*public class Node {
    private final String nodeName;
    private final Map<String, File> departmentFolders;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private volatile boolean isAlive = true;
    private volatile long lastResponseTime = System.currentTimeMillis();
    private final List<String> departments;
    private final String nodePath;
    private final File backupDir;
    private final Map<String, FileMetadata> fileMetadataMap = new ConcurrentHashMap<>();
    private static final Logger nodeLogger = Logger.getLogger("NodeLogger");
    private AtomicInteger requestCount = new AtomicInteger(0);
    private final Map<String, FileLock> fileLocks = new ConcurrentHashMap<>();
    private static final long LOCK_TIMEOUT_MS = 30000;

    public Node(String nodeName, List<String> departments, String basePath) {
        this.nodeName = nodeName;
        this.departments = departments;
        this.nodePath = basePath;
        this.backupDir = new File(nodePath + "/backups");
        this.backupDir.mkdirs();

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
    public boolean hasFile(String department, String fileName) {
        rwLock.readLock().lock();
        try {
            File file = new File(departmentFolders.get(department), fileName);
            return file.exists() && file.isFile();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public String addFile(String department, String fileName, String content, int expectedVersion) {
        rwLock.writeLock().lock();
        try {
            System.out.printf("[NODE-%s] Adding file: %s/%s (v%d)%n",
                    this.nodeName, department, fileName, expectedVersion);
            File dir = departmentFolders.get(department.toLowerCase());
            if (dir == null) return "Invalid department";

            File file = new File(dir, fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                String fileKey = department + "/" + fileName;
                fileMetadataMap.put(fileKey, new FileMetadata(expectedVersion + 1, System.currentTimeMillis()));
                return "Success";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String updateFile(String department, String fileName,
                             String content, int expectedVersion) {
        rwLock.writeLock().lock();
        try {
            // Validate department
            File dir = departmentFolders.get(department.toLowerCase());
            if (dir == null) {
                return "Invalid department";
            }

            // Check file existence
            File file = new File(dir, fileName);
            if (!file.exists()) {
                return "FILE_NOT_FOUND";
            }

            // Strict version check
            int currentVersion = getFileVersion(department, fileName);
            if (currentVersion != expectedVersion) {
                System.out.printf("[NODE-%s] VERSION_CONFLICT %s/%s: Client sent v%d, actual v%d\n",
                        nodeName, department, fileName, expectedVersion, currentVersion);
                return "VERSION_CONFLICT";
            }

            // Perform update
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                // Update metadata with new version
                fileMetadataMap.put(department + "/" + fileName,
                        new FileMetadata(expectedVersion + 1, System.currentTimeMillis()));

                System.out.printf("[NODE-%s] UPDATED %s/%s to v%d\n",
                        nodeName, department, fileName, expectedVersion + 1);
                return "Success";
            }
        } catch (Exception e) {
            System.out.printf("[NODE-%s] UPDATE_FAILED %s/%s: %s\n",
                    nodeName, department, fileName, e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public String deleteFile(String department, String fileName) {
        rwLock.writeLock().lock();
        try {
            String fileKey = department + "/" + fileName;
            File file = new File(departmentFolders.get(department), fileName);

            if (file.exists() && file.delete()) {
                fileMetadataMap.remove(fileKey);
                return "Success";
            }
            return "Delete failed";
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public String backupFile(String department, String fileName, String content) {
        rwLock.writeLock().lock();
        try {
            File backupFile = new File(backupDir, department + "_" + fileName + ".bak");
            nodeLogger.info(String.format(
                    "[%s] Creating backup for %s/%s at %s",
                    nodeName, department, fileName, backupFile.getAbsolutePath()));

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write(content);
                String result = "Backup successful";
                nodeLogger.info(String.format("[%s] %s", nodeName, result));
                return result;
            }
        } catch (Exception e) {
            String error = "Backup failed: " + e.getMessage();
            nodeLogger.severe(String.format("[%s] %s", nodeName, error));
            return error;
        } finally {
            rwLock.writeLock().unlock();
        }
    }    public FileVersion getFileWithVersion(String department, String fileName) throws FileNotFoundException {
        rwLock.readLock().lock();
        try {
            String fileKey = department + "/" + fileName;
            File file = new File(departmentFolders.get(department), fileName);

            if (!file.exists()) {
                throw new FileNotFoundException("File not found");
            }

            try (Scanner scanner = new Scanner(file)) {
                String content = scanner.useDelimiter("\\Z").next();
                FileMetadata metadata = fileMetadataMap.get(fileKey);

                return new FileVersion(
                        content,
                        metadata != null ? metadata.version : 0,
                        metadata != null ? metadata.lastModified : 0
                );
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public String restoreFromBackup(String department, String fileName) {
        rwLock.writeLock().lock();
        try {
            File backupFile = new File(backupDir, department + "_" + fileName + ".bak");
            nodeLogger.info(String.format(
                    "[%s] Attempting restore from backup: %s",
                    nodeName, backupFile.getAbsolutePath()));

            if (!backupFile.exists()) {
                String error = "Backup not found";
                nodeLogger.warning(String.format("[%s] %s", nodeName, error));
                return error;
            }

            try (Scanner scanner = new Scanner(backupFile)) {
                String content = scanner.useDelimiter("\\Z").next();
                String result = this.addFile(department, fileName, content, 0);
                nodeLogger.info(String.format(
                        "[%s] Restore completed: %s", nodeName, result));
                return result;
            }
        } catch (Exception e) {
            String error = "Restore failed: " + e.getMessage();
            nodeLogger.severe(String.format("[%s] %s", nodeName, error));
            return error;
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
    */

//TODO WOKING VERY COORCTLY NodeSyncService class
/*
public class NodeSyncService implements Runnable {
    private final List<Node> nodes;
    private final int syncHour;
    private final int syncMinute;
    private final int syncPort;
    private final Coordinator coordinator;

    public NodeSyncService(List<Node> nodes, Coordinator coordinator, int syncHour, int syncMinute, int syncPort) {
        this.nodes = nodes;
        this.coordinator = coordinator;
        this.syncHour = syncHour;
        this.syncMinute = syncMinute;
        this.syncPort = syncPort;
    }

    @Override
    public void run() {
        LocalTime targetSyncTime = LocalTime.of(syncHour, syncMinute);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextSync = now.with(targetSyncTime);

                if (now.isAfter(nextSync)) {
                    nextSync = nextSync.plusDays(1);
                }

                long delayMillis = Duration.between(now, nextSync).toMillis();
                Thread.sleep(delayMillis);

                System.out.println("[SYNC] Starting synchronization at " + LocalDateTime.now());
                performFullSync();
                System.out.println("[SYNC] Synchronization completed at " + LocalDateTime.now());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[SYNC] Service interrupted");
            } catch (Exception e) {
                System.err.println("[SYNC ERROR] " + e.getMessage());
                try { Thread.sleep(300_000); } catch (InterruptedException ie) { break; }
            }
        }
    }

    private void performFullSync() {
        // 1. المزامنة من Coordinator إلى جميع العقد
        syncFromCoordinatorToNodes();

        // 2. المزامنة بين العقد بعضها البعض
        syncBetweenNodes();
    }

    private void syncFromCoordinatorToNodes() {
        try {
            System.out.println("[SYNC] Syncing from Coordinator to all nodes");
            Map<String, String> allFiles = coordinator.getAllFilesBackup();

            for (Node node : nodes) {
                if (!node.isNodeAlive()) continue;

                for (Map.Entry<String, String> entry : allFiles.entrySet()) {
                    String[] parts = entry.getKey().split("/");
                    String dept = parts[0];
                    String filename = parts[1];
                    String content = entry.getValue();

                    if (!node.hasFile(dept, filename)) {
                        node.addFile(dept, filename, content, 0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SYNC ERROR] Coordinator to nodes: " + e.getMessage());
        }
    }

    private void syncBetweenNodes() {
        for (Node source : nodes) {
            if (!source.isNodeAlive()) continue;

            for (Node target : nodes) {
                if (!target.isNodeAlive() || source.equals(target)) continue;

                try {
                    System.out.println("[SYNC] Syncing from " + source.getName() + " to " + target.getName());

                    // المزامنة في اتجاه واحد (من المصدر إلى الهدف)
                    syncUnidirectional(source, target);
                } catch (Exception e) {
                    System.err.println("[SYNC ERROR] " + source.getName() + " to " + target.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void syncUnidirectional(Node source, Node target) throws RemoteException , FileNotFoundException {
        List<String> sourceFiles = source.getAllFiles();
        List<String> targetFiles = target.getAllFiles();

        // إضافة/تحديث الملفات الناقصة
        for (String filePath : sourceFiles) {
            String[] parts = filePath.split("/");
            String dept = parts[0];
            String filename = parts[1];

            if (!targetFiles.contains(filePath)) {
                String content = source.getFileContent(dept, filename);
                target.addFile(dept, filename, content, 0);
            } else {
                // إذا كان الملف موجوداً في كلا العقدتين، نتحقق من أيهما أحدث
                FileVersion sourceVersion = source.getFileWithVersion(dept, filename);
                FileVersion targetVersion = target.getFileWithVersion(dept, filename);

                if (sourceVersion.getLastModified() > targetVersion.getLastModified()) {
                    target.updateFile(dept, filename, sourceVersion.getContent(), targetVersion.getVersion());
                }
            }
        }

        // حذف الملفات الزائدة (اختياري)
        for (String filePath : targetFiles) {
            if (!sourceFiles.contains(filePath)) {
                String[] parts = filePath.split("/");
                target.deleteFile(parts[0], parts[1]);
            }
        }
    }
}*/
/*public class NodeSyncService implements Runnable {
    private final List<Node> nodes;
    private final int syncHour;
    private final int syncMinute;
    private  int syncPort;

    public NodeSyncService(List<Node> nodes, int syncHour, int syncPort,int syncMinute) {
        this.nodes = nodes;
        this.syncHour = syncHour;
        this.syncMinute = syncMinute;
    }
    @Override
    public void run() {
        // إعداد وقت المزامنة المطلوب (مثلاً 12 مساءً)
        LocalTime targetSyncTime = LocalTime.of(syncHour, syncMinute);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextSync = now.with(targetSyncTime);

                // إذا كانت الساعة المطلوبة قد مرت اليوم، نحددها لليوم التالي
                if (now.isAfter(nextSync)) {
                    nextSync = nextSync.plusDays(1);
                    System.out.println("[SYNC] Time for sync has passed, scheduling for next day: " + nextSync);

                }

                // حساب المدة حتى المزامنة القادمة
                long delayMillis = Duration.between(now, nextSync).toMillis();
                System.out.println("[SYNC] Waiting for " + delayMillis / 1000 + " seconds until synchronization...");

                // النوم حتى وقت المزامنة
                Thread.sleep(delayMillis);

                System.out.println("[SYNC][" + LocalDateTime.now() + "] Starting synchronization...");
                performFullSync();
                System.out.println("[SYNC][" + LocalDateTime.now() + "] Synchronization completed!");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[SYNC] Service was interrupted");
            } catch (Exception e) {
                System.err.println("[SYNC] Error during synchronization: " + e.getMessage());
                // إعادة المحاولة بعد 5 دقائق في حالة الخطأ
                try { Thread.sleep(300_000); } catch (InterruptedException ie) { break; }
            }
        }
    }//يينفّذ حلقة زمنية تنتظر وتنفّذ المزامنة عند الوقت المحدد
    private void syncNode(Node source, Node target) {
       try {
           System.out.printf("[SYNC-DETAIL] Syncing from %s to %s%n",
                   source.getName(), target.getName());

           // استخدام معرف مستخدم خاص للمزامنة
           String syncUserId = "SYSTEM_SYNC_USER";

           List<String> sourceFiles = source.getAllFiles();
           List<String> targetFiles = target.getAllFiles();

           System.out.printf("[SYNC-DETAIL] Source files: %d, Target files: %d%n",
                   sourceFiles.size(), targetFiles.size());

           // إضافة/تحديث الملفات الناقصة
           for (String filePath : sourceFiles) {
               String[] parts = filePath.split("/");
               String dept = parts[0];
               String fileName = parts[1];

               if (!targetFiles.contains(filePath)) {
                   System.out.printf("[SYNC-DETAIL] Copying %s to %s%n",
                           filePath, target.getName());
                   String content = source.getFileContent(dept, fileName);
                   target.addFile(dept, fileName, content, 0, syncUserId);
               }
           }

           // حذف الملفات الزائدة
           for (String filePath : targetFiles) {
               if (!sourceFiles.contains(filePath)) {
                   String[] parts = filePath.split("/");
                   System.out.printf("[SYNC-DETAIL] Deleting %s from %s%n",
                           filePath, target.getName());
                   String result = target.deleteFile(parts[0], parts[1], syncUserId);
                   System.out.printf("[SYNC-DETAIL] Delete result: %s%n", result);
               }
           }
       } catch (Exception e) {
           System.err.printf("[SYNC-ERROR] %s to %s: %s%n",
                   source.getName(), target.getName(), e.getMessage());
       }
   }//	يقوم بمقارنة الملفات ونسخ أو حذفها من أجل المزامنة
    void performFullSync() {
        for (Node source : nodes) {
            if (!source.isNodeAlive()) continue;

            source.cleanupOldBackups(7);
            for (Node target : nodes) {
                if (!source.equals(target) && target.isNodeAlive()) {
                    syncNode(source, target);}}}}//يزامن جميع العقد الحية مع بعضها
 }*/

//TODO WOKING VERY COORCTLY OperationHandler class
/*
public class OperationHandler {
    private final Coordinator coordinator;

    public OperationHandler(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    public String handle(Request request) {
        switch (request.getType()) {
            case REGISTER:
                coordinator.registerUser(request.getUser());
                return "User registered successfully.";
            case LOGIN:
                User user = request.getUser();
                if (user == null) return "Invalid login request.";

                User existing = coordinator.getUser(user.getUsername());
                if (existing != null && existing.getPassword().equals(user.getPassword())) {
                    String token = coordinator.generateToken(user.getUsername());
                    return "LOGIN_SUCCESS:" + token;
                } else {
                    return "LOGIN_FAILED";
                }
            case FILE_OPERATION:
                boolean result = coordinator.handleFileOperation(
                        request.getToken(),
                        request.getUsername(),
                        request.getOperation(),
                        request.getDepartment(),
                        request.getFileName(),
                        request.getContent()
                );
                return result ? "Operation successful" : "Operation failed";
            case FILE_REQUEST:
                // Request file from other department nodes
                String fileContent = coordinator.requestFileFromNode(request.getToken(), request.getUsername(), request.getDepartment(), request.getFileName());
                return fileContent != null ? "File Content: " + fileContent : "File not found";
            default:
                return "Unknown operation type.";
        }
    }
}*/