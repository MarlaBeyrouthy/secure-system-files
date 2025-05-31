import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private static Coordinator coordinator;
    private static String token = null;
    private static Role userRole = null;
    private static String currentUser = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt for the server IP
        System.out.print("Enter server IP [localhost]: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "localhost";

        try {
            // Locate the RMI registry and fetch the Coordinator service
            Registry registry = LocateRegistry.getRegistry(ip);
            coordinator = (Coordinator) registry.lookup("CoordinatorService");

            handleAuthentication(scanner);
            handleOperations(scanner);

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void handleAuthentication(Scanner scanner) {
        while (token == null) {
            System.out.println("\nAuthentication");
            System.out.println("1. Register\n2. Login\n3. Exit");
            System.out.print("Choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice == 3) System.exit(0);

                Request request = createAuthRequest(scanner, choice);
                if (request == null) continue;

                String response = new OperationHandler(coordinator).handle(request);

                if (response != null) {
                    processAuthResponse(response);
                }
            } catch (NumberFormatException e) {
                System.err.println("Please enter a valid number");
            } catch (Exception e) {
                System.err.println("Connection error: " + e.getMessage());
            }
        }
    }
    private static Request createAuthRequest(Scanner scanner, int choice) {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (choice == 1) { // Register
            System.out.print("Role (1. Manager / 2. Employee): ");
            int roleChoice = Integer.parseInt(scanner.nextLine());
            Role role = (roleChoice == 1) ? Role.MANAGER : Role.EMPLOYEE;

            String department = null;
            if (role == Role.EMPLOYEE) {
                System.out.print("Department (development/design/qa): ");
                department = scanner.nextLine();
                if (!Arrays.asList("development", "design", "qa").contains(department)) {
                    System.out.println("Invalid department!");
                    return null;
                }
            }

            return new Request(OperationType.REGISTER, new User(username, password, role, department));
        } else { // Login
            return new Request(OperationType.LOGIN, new User(username, password, null, null));
        }
    }
    private static void processAuthResponse(String response) {
        if (response.startsWith("LOGIN_SUCCESS:")) {
            String[] parts = response.split(":");
            token = parts[1];
            currentUser = parts[2];
            userRole = Role.valueOf(parts[3]);
            System.out.println("Login successful as " + currentUser + " [" + userRole + "]");
            checkNodeStatus();
        } else if (response.equals("REGISTER_SUCCESS")) {
            System.out.println("Registration successful. Please login now.");
        } else {
            System.out.println("Operation failed: " + response);
        }
    }
    private static void handleOperations(Scanner scanner) {
        while (true) {
            printMenu();
            int choice = Integer.parseInt(scanner.nextLine());

            if ((userRole == Role.MANAGER && choice == 7) || (userRole != Role.MANAGER && choice == 5)) break;

            if (choice >= 1 && choice <= 4) { // إذا كانت عملية على ملف
                System.out.print("Enter department: ");
                String dept = scanner.nextLine();
                System.out.print("Enter file name: ");
                String file = scanner.nextLine();

                printMenuWithFileStatus(dept, file);}


            try {
                Request request = createOperationRequest(scanner, choice);
                if (request == null) continue;

                String response = new OperationHandler(coordinator).handle(request);
                System.out.println("Server response:\n" + response);

            } catch (Exception e) {
                System.err.println("Operation failed: " + e.getMessage());
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

                //asi yerp vor iragan update bid enenl ge tnnek
          /*  case 2: // Update File

                operationType = "update";
                System.out.print("Enter department (development/design/qa): ");
                department = scanner.nextLine();
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();
                System.out.print("Enter new content: ");
                content = scanner.nextLine();
                break;*/

           //asa vor lockinge test enenk
            case 2: // Update File
                operationType = "update";

                System.out.print("Enter department: ");
                String updateDept = scanner.nextLine();

                System.out.print("Enter file name: ");
                String updateFileName = scanner.nextLine();

                try {
                    // Try to acquire lock first
                    if (!coordinator.acquireLock(updateDept + "/" + updateFileName, currentUser)) {
                        String owner = coordinator.checkLockStatus(updateDept + "/" + updateFileName);
                        System.out.println("\n⚠️ File is currently being edited by: " + owner);
                        System.out.println("Please try again later.\n");
                        break;
                    }

                    // Lock acquired, get new content
                    System.out.print("Enter new content: ");
                     content = scanner.nextLine();

                    // Perform update operation
                    boolean updateResult = coordinator.handleFileOperation(token, currentUser, "update", updateDept, updateFileName, content);

                    if (updateResult) {
                        System.out.println("✅ File updated successfully.");
                    } else {
                        System.out.println("❌ Failed to update file.");
                    }

                } catch (IOException e) {
                    System.err.println("Error during file operation: " + e.getMessage());
                } finally {
                    // Always release the lock after operation attempt
                    try {
                        coordinator.releaseLock(updateDept + "/" + updateFileName, currentUser);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
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

            case 6: // List All Users (Manager only)
                if (userRole != Role.MANAGER) {
                    System.out.println("❌ Unauthorized access!");
                    return null;
                }
                return new Request(OperationType.LIST_USERS, token, currentUser);

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
    private static void checkNodeStatus() {
        try {
            System.out.println("\n[DEBUG] Node Status Check:");
            for (int i = 1; i <= 3; i++) {
                try {
                    NodeInterface node = (NodeInterface) LocateRegistry.getRegistry("localhost", 1099)
                            .lookup("Node" + i);

                    boolean alive = node.isNodeAlive();
                    boolean ping = node.ping();

                    System.out.printf("Node%d - Alive: %b, Ping: %b%n", i, alive, ping);
                } catch (Exception e) {
                    System.out.println("Node" + i + " - Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void printMenu() {
        System.out.println("\n📋 Main Menu");
        System.out.println("1. Add File");
        System.out.println("2. Update File");
        System.out.println("3. Delete File");
        System.out.println("4. Request File");
        if (userRole == Role.MANAGER) {
            System.out.println("5. Register User");
            System.out.println("6. List All Users");
        }
        System.out.println((userRole == Role.MANAGER ? "7" : "5") + ". Exit");
        System.out.print("Choice: ");
    }
    private static void printMenuWithFileStatus(String department, String fileName) {
        try {
            String lockStatus = coordinator.getFileLockStatus(department + "/" + fileName);
            System.out.println("\n🔒 File Status: " + lockStatus);
        } catch (Exception e) {
            System.out.println("\n⚠️ Could not check file status");
        }
        printMenu();
    }
}




