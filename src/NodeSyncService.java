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
        LocalTime targetSyncTime = LocalTime.of(syncHour, syncMinute);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextSync = now.with(targetSyncTime);

                if (now.isAfter(nextSync)) {
                    nextSync = nextSync.plusDays(1);
                    System.out.printf("[SYNC] Time passed. Next sync scheduled for: %s%n", nextSync);
                }

                long delayMillis = Duration.between(now, nextSync).toMillis();
                System.out.printf("[SYNC] Waiting %d seconds until next sync...%n", delayMillis / 1000);
                Thread.sleep(delayMillis);

                System.out.printf("[SYNC][%s] Starting synchronization...%n", LocalDateTime.now());
                performFullSync();
                System.out.printf("[SYNC][%s] Synchronization completed.%n", LocalDateTime.now());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[SYNC] Service was interrupted.");
            } catch (Exception e) {
                System.err.printf("[SYNC] Error during synchronization: %s%n", e.getMessage());
                try {
                    Thread.sleep(300_000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
    }

    //التزامن المجدول (مزامنة نهاية اليوم)
    void performFullSync() {
        for (Node source : nodes) {
            if (!source.isNodeAlive()) continue;

            source.cleanupOldBackups(7);

            for (Node target : nodes) {
                if (!source.equals(target) && target.isNodeAlive()) {
                    syncNode(source, target);
                }
            }
        }
    }

    private void syncNode(Node source, Node target) {
        try {
            System.out.printf("[SYNC-DETAIL] Syncing from %s to %s%n", source.getName(), target.getName());

            String syncUserId = "SYSTEM_SYNC_USER";
            List<String> sourceFiles = source.getAllFiles();
            List<String> targetFiles = target.getAllFiles();

            System.out.printf("[SYNC-DETAIL] Source files: %d, Target files: %d%n", sourceFiles.size(), targetFiles.size());

            for (String filePath : sourceFiles) {
                String[] parts = filePath.split("/");
                String dept = parts[0];
                String fileName = parts[1];

                if (!targetFiles.contains(filePath)) {
                    System.out.printf("[SYNC-DETAIL] Copying %s to %s%n", filePath, target.getName());
                    String content = source.getFileContent(dept, fileName);
                    target.addFile(dept, fileName, content, 0, syncUserId);
                }
            }

            for (String filePath : targetFiles) {
                if (!sourceFiles.contains(filePath)) {
                    String[] parts = filePath.split("/");
                    System.out.printf("[SYNC-DETAIL] Deleting %s from %s%n", filePath, target.getName());
                    String result = target.deleteFile(parts[0], parts[1], syncUserId);
                    System.out.printf("[SYNC-DETAIL] Delete result: %s%n", result);
                }
            }

        } catch (Exception e) {
            System.err.printf("[SYNC-ERROR] Sync %s to %s failed: %s%n", source.getName(), target.getName(), e.getMessage());
        }
    }
}





