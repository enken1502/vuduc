package view;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class UserDAO {
    private static final String DB_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=ChatDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "minh";
    private static final String DB_PASS = "12345678";

    private Connection getConnection() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public User getUserProfile(String username) {
        String sql = "SELECT UserID, Username, Email, [Status] FROM Users WHERE LOWER(TRIM(Username)) = LOWER(TRIM(?))";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("UserID"),
                        rs.getString("Username"),
                        rs.getString("Email"),
                        rs.getString("Status")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Cập nhật Username và Password theo UserID
    public boolean updateUserProfile(int userId, String newUsername, String newPassword) {
        String sql;
        boolean updatePass = (newPassword != null && !newPassword.trim().isEmpty());

        if (updatePass) {
            sql = "UPDATE Users SET Username = ?, [Password] = ? WHERE UserID = ?";
        } else {
            sql = "UPDATE Users SET Username = ? WHERE UserID = ?";
        }

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername.trim());
            if (updatePass) {
                ps.setString(2, newPassword.trim());
                ps.setInt(3, userId);
            } else {
                ps.setInt(2, userId);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}