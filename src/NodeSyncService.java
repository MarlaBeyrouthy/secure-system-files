import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

//كلاس للمزامنة
//n7na hon 3mlna el tzamon bn2a 3al el w2t ou el d2i2a ya3ni iza bdna
//nghyer mnrou7 3l server bl str 71 mn hounik mnghyir
//mouzamne yomiye
//  كل العمليات تتم عبر توابع Node
public class NodeSyncService implements Runnable {
    private final List<Node> nodes;
    private final int syncHour;
    private final int syncMinute;

    public NodeSyncService(List<Node> nodes, int syncHour, int syncPort, int syncMinute) {
        this.nodes = nodes;
        this.syncHour = syncHour;
        this.syncMinute = syncMinute;
    }

    @Override
    public void run() {
        // Define the fixed daily sync time
        LocalTime targetSyncTime = LocalTime.of(syncHour, syncMinute);

        // Keep running unless the thread is interrupted
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Get current time
                LocalDateTime now = LocalDateTime.now();

                // Set today's sync time (with same date as now)
                LocalDateTime nextSync = now.with(targetSyncTime);

                // If sync time today already passed, schedule it for tomorrow
                if (now.isAfter(nextSync)) {
                    nextSync = nextSync.plusDays(1);
                    System.out.printf("[SYNC] Time passed. Next sync scheduled for: %s%n", nextSync);
                }

                // Calculate how long to wait (in milliseconds)
                long delayMillis = Duration.between(now, nextSync).toMillis();
                System.out.printf("[SYNC] Waiting %d seconds until next sync...%n", delayMillis / 1000);

                // Sleep until the scheduled time
                Thread.sleep(delayMillis);

                // Start the synchronization process
                System.out.printf("[SYNC][%s] Starting synchronization...%n", LocalDateTime.now());
                performFullSync();
                System.out.printf("[SYNC][%s] Synchronization completed.%n", LocalDateTime.now());

            } catch (InterruptedException e) {
                // If interrupted, stop the thread
                Thread.currentThread().interrupt();
                System.out.println("[SYNC] Service was interrupted.");
            } catch (Exception e) {
                // On error, log it and wait 5 minutes before trying again
                System.err.printf("[SYNC] Error during synchronization: %s%n", e.getMessage());
                try {
                    Thread.sleep(300_000); // Wait 5 minutes
                } catch (InterruptedException ie) {
                    break; // Stop if interrupted again
                }
            }
        }
    }

    //التزامن المجدول (مزامنة نهاية اليوم)
    void performFullSync() {
        // Loop through every node as the "source" node
        for (Node source : nodes) {

            // Skip this node if it's not online (dead or unreachable)
            if (!source.isNodeAlive()) continue;

            // Clean up old backup files from the source node (older than 7 days)
            source.cleanupOldBackups(7);

            // Now sync this source node with every other "target" node
            for (Node target : nodes) {

                // Only sync to other nodes that are alive and not the same as the source
                if (!source.equals(target) && target.isNodeAlive()) {

                    // Perform synchronization from source to target
                    syncNode(source, target);
                }
            }
        }
    }


    private void syncNode(Node source, Node target) {
        try {
            // Print a log message showing which two nodes are syncing
            System.out.printf("[SYNC-DETAIL] Syncing from %s to %s%n", source.getName(), target.getName());

            // This is a special user ID used for system-level sync operations
            String syncUserId = "SYSTEM_SYNC_USER";

            // Get a list of all file paths on the source node
            List<String> sourceFiles = source.getAllFiles();

            // Get a list of all file paths on the target node
            List<String> targetFiles = target.getAllFiles();

            // Print the total number of files on both sides
            System.out.printf("[SYNC-DETAIL] Source files: %d, Target files: %d%n", sourceFiles.size(), targetFiles.size());

            // ===============================
            // Step 1: Copy files from source to target if they don't exist on target
            // ===============================
            for (String filePath : sourceFiles) {
                // Split the file path into department and filename
                // Example: "sales/report.txt" -> dept = "sales", fileName = "report.txt"
                String[] parts = filePath.split("/");
                String dept = parts[0];
                String fileName = parts[1];

                // If the file is missing on the target node, copy it from source
                if (!targetFiles.contains(filePath)) {
                    System.out.printf("[SYNC-DETAIL] Copying %s to %s%n", filePath, target.getName());

                    // Read the file's content from the source
                    String content = source.getFileContent(dept, fileName);

                    // Add the file to the target node
                    // Version is set to 0, syncUserId used to track the sync origin
                    target.addFile(dept, fileName, content, 0, syncUserId);
                }
            }

            // ===============================
            // Step 2: Delete files from target that don't exist on the source
            // ===============================
            for (String filePath : targetFiles) {

                // If a file exists on the target but NOT on the source → delete it
                if (!sourceFiles.contains(filePath)) {
                    String[] parts = filePath.split("/");

                    System.out.printf("[SYNC-DETAIL] Deleting %s from %s%n", filePath, target.getName());

                    // Call delete on the target node
                    String result = target.deleteFile(parts[0], parts[1], syncUserId);

                    // Log the result of the deletion (success or error)
                    System.out.printf("[SYNC-DETAIL] Delete result: %s%n", result);
                }
            }

        } catch (Exception e) {
            // Log any exception that happens during this sync operation
            System.err.printf("[SYNC-ERROR] Sync %s to %s failed: %s%n",
                    source.getName(), target.getName(), e.getMessage());
        }
    }
}





