package view;

public class User {
    private int userId;
    private String username;
    private String email;
    private String status;

    public User(int userId, String username, String email, String status) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.status = status;
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
}