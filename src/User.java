import java.io.Serializable;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
public class User implements Serializable {
    private final String username;
    private String password;
    private final Role role;
    private String token;
    private String department;
    private final String hashedPassword;
    private final byte[] salt;
    public User(String username, String password, Role role,String department) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.department = department;
        this.salt = generateSalt();
        this.hashedPassword = hashPassword(password, salt);
    }
    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
    private static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
    // استبدال طريقة التحقق من كلمة المرور
    public boolean verifyPassword(String password) {
        return this.hashedPassword.equals(hashPassword(password, this.salt));
    }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getDepartment() {return department;}

    @Override
    public String toString() {
        return "User: " + username + " | Role: " + role +
                (department != null ? " | Department: " + department : "");
    }
}
   enum Role {
    MANAGER,  // يتحكم بكل الأقسام
    EMPLOYEE ; // يتحكم فقط بقسمه
    public boolean canManageUsers() {
        return this == MANAGER;
    }
}