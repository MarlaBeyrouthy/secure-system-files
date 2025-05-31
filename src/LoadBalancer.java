import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
public class LoadBalancer {
    private final List<Node> nodes;

    //kharita lltb3 3dd el tlabat 3ala kl 3kde
    private final Map<Node, Integer> nodeLoad = new ConcurrentHashMap<>();
    public LoadBalancer(List<Node> nodes) {
        this.nodes = nodes;
        nodes.forEach(node -> nodeLoad.put(node, 0));
    }
    public synchronized Node getNextAvailableNode() throws RemoteException {
        if (nodes.isEmpty()) throw new RemoteException("No nodes registered");

        // تصفية العقد الحية فقط
        List<Node> aliveNodes = nodes.stream()
                .filter(n -> {
                    try {
                        return n.isNodeAlive();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (aliveNodes.isEmpty()) throw new RemoteException("All nodes are down");

        Node bestNode = aliveNodes.stream()
                .min(Comparator.comparingInt(node -> nodeLoad.getOrDefault(node, 0)))
                .orElse(aliveNodes.get(0));

        // تحديث الحمل
        nodeLoad.put(bestNode, nodeLoad.getOrDefault(bestNode, 0) + 1);

        System.out.printf("[LB] Selected %s | Load: %d | Alive Nodes: %d/%d%n",
                bestNode.getName(),
                nodeLoad.get(bestNode),
                aliveNodes.size(),
                nodes.size());

        return bestNode;
    }//bykhtar el 3kde el ansab
    public void nodeFailed(Node node) {nodeLoad.computeIfPresent(node, (k, v) -> v > 0 ? v - 1 : 0);}//يقلل الحمل على عقدة في حال فشلت (بعد محاولة).
    public synchronized void addNode(Node node) throws RemoteException {
        if (!nodes.contains(node)) {
            nodes.add(node);
            nodeLoad.put(node, 0);
            System.out.printf("[LB] Node added: %s | Total nodes: %d%n",
                    node.getName(), nodes.size());
        }
    }
    public int getNodeLoad(Node node) {return nodeLoad.getOrDefault(node, 0);}// يرجع عدد الطلبات على عقدة معينة
}




//aliyet 3ml el loadbalancer
//يحسب الحمل على كل عقدة (عدد الطلبات النشطة)
//يختار العقدة الأقل حملًا عند كل طلب
//يستبعد العقد المعطلة تلقائيًا