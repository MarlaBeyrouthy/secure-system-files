import java.io.Serializable;

// يحمل معلومات طلب مزامنة محدد، حيث كل طلب يحتوي على اسم العقدة وأمر معين
public class SyncRequest implements Serializable {
    private final String nodeName;
    private final String command;
    public SyncRequest(String nodeName, String command) {
        this.nodeName = nodeName;
        this.command = command;
    }
    // Getters
    public String getCommand() { return command; }
}