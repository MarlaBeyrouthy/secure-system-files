import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//يضمن استمرارية عمل العقد ويسقط العقد المعطلة.
//f7s 7alet el 3kde ou el kshf 3n el 3kd el mou3tala
public class HeartbeatChecker implements Runnable {
    private final List<Node> nodes;
    private final long checkInterval;//الوقت بين كل فحص وآخر
    private volatile boolean running = true;
    private final Map<Node, Integer> failureCounts = new ConcurrentHashMap<>();//الحد الأقصى لعدد الفشل قبل اعتبار العقدة ميتة واستبعادها
    private static final int MAX_FAILURES_BEFORE_EVICTION = 3;

    public HeartbeatChecker(List<Node> nodes, long checkIntervalMillis) {
        this.nodes = nodes;
        this.checkInterval = checkIntervalMillis;

    }

    @Override
    public void run() {
        System.out.printf("[Heartbeat] Starting checker with %d nodes, interval: %dms%n",
                nodes.size(), checkInterval);

        while (running && !Thread.currentThread().isInterrupted()) {
            checkNodesHealth();//t7kok mn el 3kd
            sleepUntilNextCheck();//entizar  الانتظار قبل التكرار التالي
            evictUnhealthyNodes();//إزالة العقد التي فشلت أكثر من مرة
            recoverFailedNodes();//mou7awlt i3adet el 3kd el myte
        }
    }

    //iza fshlt el 3kd 3 mrat ytm izalatha mn al ka2ime
    private void evictUnhealthyNodes() {
        failureCounts.entrySet().removeIf(entry -> {
            if (entry.getValue() >= MAX_FAILURES_BEFORE_EVICTION) {
                System.out.printf("[Heartbeat] Evicting node %s after %d consecutive failures%n",
                        entry.getKey().getName(), entry.getValue());
                nodes.remove(entry.getKey());
                return true;
            }
            return false;
        });
    } //يستبعد العقد التي فشلت أكثر من 3 مرات متتالية
    private void checkNodesHealth() {
        for (Node node : nodes) {

            // إيقاف Node2 يدويًا للاختبار,iza bdna n3mel ikaf laa7ad el 3kd mnstkhdm had el if condiation
            /*if (node.getName().equals("Node2")) {
                node.setNodeAlive(false);
                failureCounts.put(node, MAX_FAILURES_BEFORE_EVICTION);
                System.out.println("[TEST] Manually stopped Node2 for testing");
                continue; // تخطي الفحص لهذه العقدة
            }*/

            try {
                boolean isAlive = node.ping();
                node.setNodeAlive(isAlive);

                if (isAlive) {
                    failureCounts.remove(node);
                    logNodeHealth(node);
                } else {
                    failureCounts.merge(node, 1, Integer::sum);
                    System.out.printf("[Heartbeat] Node %s failed %d times%n",
                            node.getName(), failureCounts.get(node));
                }
            } catch (RemoteException e) {
                handleNodeCommunicationError(node, e);
            }
        }
    } //يرسل لكل عقدة نداء ping() ليتأكد أنها حية.
    private void logNodeHealth(Node node) throws RemoteException {
        long timeSinceLastResponse = System.currentTimeMillis() - node.getLastResponseTime();
        System.out.printf("[Health] Node %s - Last response: %dms ago, Active requests: %d%n",
                node.getName(), timeSinceLastResponse, node.getActiveRequestsCount());
    }
    private void handleNodeCommunicationError(Node node, RemoteException e) {
        System.err.printf("[Heartbeat] Error checking node %s: %s%n",
                node.getName(), e.getMessage());
        node.setNodeAlive(false);
    } //يُستخدم عند حدوث استثناء أثناء فحص عقدة
    private void sleepUntilNextCheck() {
        try {
            Thread.sleep(checkInterval);
        } catch (InterruptedException e) {
            System.out.println("[Heartbeat] Checker interrupted, shutting down...");
            running = false;
            Thread.currentThread().interrupt();
        }
    }//تنتظر الفترة الزمنية المحددة قبل التكرار التالي
    private void recoverFailedNodes() {
        for (Node node : new ArrayList<>(nodes)) { // استخدم نسخة من القائمة لتجنب ConcurrentModification
            try {
                if (!node.isNodeAlive() && failureCounts.getOrDefault(node, 0) >= MAX_FAILURES_BEFORE_EVICTION) {
                    System.out.println("[إستعادة] جارٍ إعادة تشغيل العقدة: " + node.getName());

                    // الخطوة الوحيدة المطلوبة: إعادة تعيين حالة العقدة
                    node.setNodeAlive(true);
                    failureCounts.remove(node);

                    System.out.println("[نجاح] تمت استعادة العقدة: " + node.getName());
                }
            } catch (Exception e) {
                System.err.println("خطأ في استعادة العقدة: " + e.getMessage());
            }
        }
    } //محاولة إعادة العقد المعطلة للحياة.
}






