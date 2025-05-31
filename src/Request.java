import java.io.Serializable;

//had el class krmal etlob requestat mou3yne bl clinet
public class Request implements Serializable {
    private OperationType type;
    private User user;
    private String token;
    private String username;
    private String operation;
    private String department;
    private String fileName;
    private String content;

    public Request(OperationType type, User user) {
        this.type = type;
        this.user = user;
    }
    public Request(OperationType type, String token, String username, String operation, String department, String fileName, String content) {
        this.type = type;
        this.token = token;
        this.username = username;
        this.operation = operation;
        this.department = department;
        this.fileName = fileName;
        this.content = content;
    }
    public Request(OperationType type, String token, String username) {
        this.type = type;
        this.token = token;
        this.username = username;
    }
    public OperationType getType() { return type; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getOperation() { return operation; }
    public String getDepartment() { return department; }
    public String getFileName() { return fileName; }
    public String getContent() { return content; }
}
