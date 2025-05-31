import java.rmi.RemoteException;
import java.util.List;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.*;
import java.util.*;


/*public class Server {
    private static final int RMI_PORT = 1099;
    private static final int SYNC_START_PORT = 6001;
    private static final int HEARTBEAT_CHECK_INTERVAL = 30000; // 30 ثانية

    public static void main(String[] args) {
        try {
            // 1. إعداد سجل RMI
            setupRMIRegistry();

            // 2. إنشاء وتسجيل خدمة المنسق
            CoordinatorImpl coordinator = new CoordinatorImpl();
            registerCoordinatorService(coordinator);

            // 3. إعداد البيئة الافتراضية مع العقد
            List<Node> nodes = setupDefaultEnvironment(coordinator);


            // 4. بدء الخدمات المساعدة
            startSupportServices(nodes);

        } catch (Exception e) {
            System.err.println("[ERROR] Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    *//*  private static void startSupportServices(List<Node> nodes) {
        // بدء خدمة المزامنة اليومية
        startDailySyncService(nodes, 11, 8080, 35); // المزامنة الساعة 11:35

        // بدء خدمة مراقبة العقد (Heartbeat)
        startHeartbeatService(nodes);

        // بدء المزامنة الفورية لأغراض الاختبار
        triggerImmediateSyncForTesting(nodes);
    }*//*
    private static void startSupportServices(List<Node> nodes) {
      // بدء المزامنة الفورية لأغراض الاختبار
      triggerImmediateSyncForTesting(nodes);

      // بدء خدمة المزامنة اليومية (الساعة 11:35)
      startDailySyncService(nodes, 11, 8080, 35);

      // بدء خدمة مراقبة العقد
      startHeartbeatService(nodes);
  }
    private static void triggerImmediateSyncForTesting(List<Node> nodes) {
        System.out.println("\n[TEST] Starting immediate sync for all nodes...");
        new Thread(() -> {
            NodeSyncService syncService = new NodeSyncService(nodes, 11, 8080, 35);
            syncService.performFullSync();
        }).start();
    }
    private static void startHeartbeatService(List<Node> nodes) {
        HeartbeatChecker heartbeatChecker = new HeartbeatChecker(nodes, HEARTBEAT_CHECK_INTERVAL);
        Thread heartbeatThread = new Thread(heartbeatChecker);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
        System.out.println("[MAIN] Heartbeat service started with interval: " +
                (HEARTBEAT_CHECK_INTERVAL/1000) + " seconds");
    }
    *//*
    private static void triggerImmediateSyncForTesting(List<Node> nodes) {
        System.out.println("[TEST] Triggering initial sync for testing...");
        new Thread(() -> {
            new NodeSyncService(nodes, 11, 8080, 35).run();
        }).start();
    }
*//*
    private static void setupRMIRegistry() throws RemoteException {
        try {
            LocateRegistry.createRegistry(RMI_PORT);  // Start RMI registry
            System.out.println("[MAIN] RMI Server running on port " + RMI_PORT);
        } catch (RemoteException e) {
            throw new RemoteException("Error starting RMI registry", e);
        }
    }
    private static void registerCoordinatorService(CoordinatorImpl coordinator) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("CoordinatorService", coordinator);
            System.out.println("[MAIN] CoordinatorService registered successfully.");
        } catch (RemoteException e) {
            throw new RemoteException("Error registering CoordinatorService in RMI registry", e);
        }
    }
    private static List<Node> setupDefaultEnvironment(Coordinator coordinator) throws RemoteException {
        String tempDir = "C:/distributed_system/";
        new File(tempDir).mkdirs();

        // Define departments
        List<String> departments = Arrays.asList("development", "design", "qa");
        List<Node> nodes = new ArrayList<>();

        Registry registry = LocateRegistry.getRegistry();  // <-- Add this

        for (int i = 1; i <= 3; i++) {
            String nodeName = "Node" + i;
            String nodePath = tempDir + "/node" + i;
            new File(nodePath).mkdirs();

            for (String dept : departments) {
                new File(nodePath + "/" + dept).mkdirs();
            }

            Node node = new Node(nodeName, departments, nodePath, coordinator);
            coordinator.addNode(node);
            nodes.add(node);

            // Start sync server ,asi eselete eno node1 =6001 ,node2=6002,node3=6003
            startSyncServer(node, SYNC_START_PORT + i - 1);

            // Bind remote NodeInterface
            NodeInterface remoteNode = new NodeImpl(node);
            registry.rebind(nodeName, remoteNode);  // <-- This is key
            System.out.println("[RMI] NodeInterface bound for " + nodeName);
        }

        return nodes;
    }
    private static void startSyncServer(Node node, int port) {
        new Thread(() -> {
            try {
                new SyncServer(node, port).run();
                System.out.println("[SYNC] Sync Server started for " + node.getName() + " on port " + port);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to start sync server for " + node.getName() + ": " + e.getMessage());
            }
        }).start();
    }
    public static void startDailySyncService(List<Node> nodes, int syncHour, int syncPort, int syncMinute) {
        NodeSyncService syncService = new NodeSyncService(nodes, syncHour, syncPort, syncMinute);
        Thread syncThread = new Thread(syncService);
        syncThread.start();
        System.out.println("[MAIN] Daily sync service started at " + syncHour + ":" + syncMinute);
    }}*/


import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int RMI_PORT = 1099;
    private static final int SYNC_START_PORT = 6001;
    private static final int HEARTBEAT_CHECK_INTERVAL = 30_000; // 30 ثانية


    public static void main(String[] args) {
        try {
            setupRMIRegistry();

            CoordinatorImpl coordinator = new CoordinatorImpl();
            registerCoordinatorService(coordinator);

            List<Node> nodes = setupDefaultEnvironment(coordinator);

            startSupportServices(nodes);

        } catch (Exception e) {
            System.err.println("[ERROR] Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setupRMIRegistry() throws RemoteException {
        try {
            LocateRegistry.createRegistry(RMI_PORT);
            System.out.println("[MAIN] RMI Server running on port " + RMI_PORT);
        } catch (RemoteException e) {
            throw new RemoteException("Error starting RMI registry", e);
        }
    }

    private static void registerCoordinatorService(CoordinatorImpl coordinator) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("CoordinatorService", coordinator);
            System.out.println("[MAIN] CoordinatorService registered successfully.");
        } catch (RemoteException e) {
            throw new RemoteException("Error registering CoordinatorService in RMI registry", e);
        }
    }

    private static List<Node> setupDefaultEnvironment(Coordinator coordinator) throws RemoteException {
        String tempDir = "C:/distributed_system/";
        new File(tempDir).mkdirs();

        List<String> departments = Arrays.asList("development", "design", "qa");
        List<Node> nodes = new ArrayList<>();

        Registry registry = LocateRegistry.getRegistry();

        for (int i = 1; i <= 3; i++) {
            String nodeName = "Node" + i;
            String nodePath = tempDir + "/node" + i;
            new File(nodePath).mkdirs();

            for (String dept : departments) {
                new File(nodePath + "/" + dept).mkdirs();
            }

            Node node = new Node(nodeName, departments, nodePath, coordinator);
            coordinator.addNode(node);
            nodes.add(node);

            startSyncServer(node, SYNC_START_PORT + i - 1);

            NodeInterface remoteNode = new NodeImpl(node);
            registry.rebind(nodeName, remoteNode);
            System.out.println("[RMI] NodeInterface bound for " + nodeName);
        }

        return nodes;
    }

    private static void startSyncServer(Node node, int port) {
        new Thread(() -> {
            try {
                new SyncServer(node, port).run();
                System.out.printf("[SYNC] Sync Server started for %s on port %d%n", node.getName(), port);
            } catch (Exception e) {
                System.err.printf("[ERROR] Failed to start sync server for %s: %s%n", node.getName(), e.getMessage());
            }
        }).start();
    }

    private static void startSupportServices(List<Node> nodes) {
        triggerImmediateSyncForTesting(nodes);
        startDailySyncService(nodes, 11, 8080, 35);
        startHeartbeatService(nodes);
    }

    private static void triggerImmediateSyncForTesting(List<Node> nodes) {
        System.out.println("[TEST] Starting immediate sync for all nodes...");
        new Thread(() -> {
            NodeSyncService syncService = new NodeSyncService(nodes, 11, 8080, 35);
            syncService.performFullSync();
        }).start();
    }

    private static void startDailySyncService(List<Node> nodes, int syncHour, int syncPort, int syncMinute) {
        NodeSyncService syncService = new NodeSyncService(nodes, syncHour, syncPort, syncMinute);
        Thread syncThread = new Thread(syncService);
        syncThread.start();
        System.out.printf("[MAIN] Daily sync service scheduled at %02d:%02d%n", syncHour, syncMinute);
    }

    private static void startHeartbeatService(List<Node> nodes) {
        HeartbeatChecker heartbeatChecker = new HeartbeatChecker(nodes, HEARTBEAT_CHECK_INTERVAL);
        Thread heartbeatThread = new Thread(heartbeatChecker);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
        System.out.printf("[MAIN] Heartbeat service started with interval: %d seconds%n", HEARTBEAT_CHECK_INTERVAL / 1000);
    }
}



























