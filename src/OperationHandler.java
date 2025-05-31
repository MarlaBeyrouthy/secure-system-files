import java.io.IOException;
import java.util.List;
import java.rmi.RemoteException;
import java.util.stream.Collectors;
public class OperationHandler {
    private final Coordinator coordinator;
    public OperationHandler(Coordinator coordinator) {
        this.coordinator = coordinator;
    }
    public String handle(Request request) {
        try {
            switch (request.getType()) {
                case REGISTER:
                    return handleRegistration(request);
                case LOGIN:
                    return handleLogin(request);
                case FILE_OPERATION:
                    return handleFileOperation(request);
                case FILE_REQUEST:
                    return handleFileRequest(request);
                case LIST_USERS:
                    return handleListUsers(request);
                default:
                    return "Unknown operation type.";
            }
        } catch (RemoteException e) {
            return "Remote server error: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    private String handleRegistration(Request request) throws RemoteException {
        if (coordinator.getUser(request.getUser().getUsername()) != null) {
            return "REGISTER_FAILED: Username already exists";
        }
        coordinator.registerUser(request.getUser());
        return "REGISTER_SUCCESS";
    }
    private String handleLogin(Request request) throws RemoteException {
        User user = request.getUser();
        User existing = coordinator.getUser(user.getUsername());
        if (existing != null && existing.verifyPassword(user.getPassword())) {
            String token = coordinator.generateToken(user.getUsername());
            return "LOGIN_SUCCESS:" + token + ":" + existing.getUsername() + ":" + existing.getRole();
        }
        return "LOGIN_FAILED";
    }
    private String handleFileOperation(Request request) throws RemoteException {
        try {
            boolean result = coordinator.handleFileOperation(
                    request.getToken(),
                    request.getUsername(),
                    request.getOperation(),
                    request.getDepartment(),
                    request.getFileName(),
                    request.getContent()
            );
            return result ? "Operation successful" : "Operation failed";
        } catch (IOException e) {
            return "File operation error: " + e.getMessage();
        }
    }
   private String handleFileRequest(Request request) throws RemoteException {
    try {
        System.out.println("[DEBUG] Requesting file: " +
                request.getDepartment() + "/" + request.getFileName());
        String content = coordinator.requestFileFromNode(
                request.getToken(),
                request.getUsername(),
                request.getDepartment(),
                request.getFileName()
        );

        if (content == null) {
            return "❌ Error: Null response from server";
        } else if (content.startsWith("Error:")) {
            return "❌ Server error: " + content.substring(6);
        } else if (content.equals("File not found on any available node.")) {
            return "❌ File not found in any active node";
        } else if (content.isEmpty()) {
            return "ℹ️ File is empty";
        }

        return "File content:\n" + content;
    } catch (RemoteException e) {
        return "❌ Network error: " + e.getMessage();
    }
}
    private String handleListUsers(Request request) throws RemoteException {
        try {
            List<User> users = coordinator.getAllUsers(request.getToken());
            return users.stream()
                    .map(User::toString)
                    .collect(Collectors.joining("\n"));
        } catch (RemoteException e) {
            return "Error: " + e.getMessage();}}}

