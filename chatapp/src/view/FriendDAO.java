package view;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FriendDAO {
    // 🛢️ CẤU HÌNH DATABASE CHUẨN CỦA NHÓM
    private static final String DB_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=ChatDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "minh";
    private static final String DB_PASS = "12345678";

    private Connection getConnection() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // --- QUẢN LÝ BẠN BÈ ---

    public boolean insertFriendRequest(String sender, String receiver) {
        String sql = "INSERT INTO Friends (sender_username, receiver_username, [status]) VALUES (?, ?, 'PENDING')";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false; 
        }
    }

    public boolean acceptFriend(String sender, String receiver) {
        String sql = "UPDATE Friends SET [status] = 'ACCEPTED' WHERE sender_username = ? AND receiver_username = ? AND [status] = 'PENDING'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    public List<String> searchUsers(String keyword) {
    List<String> list = new ArrayList<>();
    // Câu lệnh SQL tìm kiếm tài khoản gần đúng theo Username
    String sql = "SELECT UserID, Username FROM Users WHERE Username LIKE ? ORDER BY Username ASC";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, "%" + keyword.trim() + "%");
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("UserID");
                String username = rs.getString("Username");
                // Định dạng chuỗi hiển thị kèm ID phía sau để nhận biết
                list.add(username + " (ID: " + id + ")");
            }
        }
    } catch (Exception e) {
        System.err.println("❌ Lỗi searchUsers: " + e.getMessage());
    }
    return list;
}

    public List<String> getAcceptedFriends(String username) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT CASE WHEN sender_username = ? THEN receiver_username ELSE sender_username END AS friend " +
                     "FROM Friends WHERE (sender_username = ? OR receiver_username = ?) AND [status] = 'ACCEPTED'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ps.setString(3, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("friend"));
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public List<String> getPendingRequests(String myUsername) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT TRIM(sender_username) AS sender FROM Friends WHERE LOWER(TRIM(receiver_username)) = LOWER(TRIM(?)) AND LOWER(TRIM([status])) = 'pending'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, myUsername.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("sender"));
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public boolean rejectOrDeleteFriend(String sender, String receiver) {
        String sql = "DELETE FROM Friends WHERE (sender_username = ? AND receiver_username = ?) OR (sender_username = ? AND receiver_username = ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender); ps.setString(2, receiver);
            ps.setString(3, receiver); ps.setString(4, sender);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // --- QUẢN LÝ NHÓM CHAT (ĐỒNG BỘ GROUPCHATS) ---

    public boolean createGroup(String groupName, String creatorUsername, List<String> memberUsernames) {
        String insertGroupSql = "INSERT INTO GroupChats (GroupName, CreatedBy) VALUES (?, (SELECT UserID FROM Users WHERE Username = ?))";
        String insertMemberSql = "INSERT INTO GroupMembers (GroupID, UserID) VALUES (?, (SELECT UserID FROM Users WHERE Username = ?))";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            int groupId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertGroupSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, groupName);
                ps.setString(2, creatorUsername);
                if (ps.executeUpdate() == 0) { conn.rollback(); return false; }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) groupId = rs.getInt(1);
                }
            }
            if (groupId != -1) {
                try (PreparedStatement ps = conn.prepareStatement(insertMemberSql)) {
                    ps.setInt(1, groupId); ps.setString(2, creatorUsername);
                    ps.executeUpdate();
                }
                for (String member : memberUsernames) {
                    try (PreparedStatement ps = conn.prepareStatement(insertMemberSql)) {
                        ps.setInt(1, groupId); ps.setString(2, member.trim());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi createGroup: " + e.getMessage());
        }
        return false;
    }

    public List<String> getMyGroups(String username) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT g.GroupName FROM GroupChats g JOIN GroupMembers gm ON g.GroupID = gm.GroupID WHERE gm.UserID = (SELECT UserID FROM Users WHERE Username = ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("GroupName"));
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
    
 
public boolean leaveGroup(String username, String groupName) {
    // 1. Lấy UserID từ Username
    String getUserIdSql = "SELECT UserID FROM Users WHERE Username = ?";
    // 2. Lấy GroupID từ GroupName trong bảng GroupChats (Đã sửa tên bảng)
    String getGroupIdSql = "SELECT GroupID FROM GroupChats WHERE GroupName = ?";
    // 3. Xóa dòng khỏi bảng GroupMembers
    String deleteMemberSql = "DELETE FROM GroupMembers WHERE GroupID = ? AND UserID = ?";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
        int userId = -1;
        int groupId = -1;

        // Lấy UserID
        try (PreparedStatement ps = conn.prepareStatement(getUserIdSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) userId = rs.getInt("UserID");
            }
        }

        // Lấy GroupID
        try (PreparedStatement ps = conn.prepareStatement(getGroupIdSql)) {
            ps.setString(1, groupName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) groupId = rs.getInt("GroupID");
            }
        }

        if (userId == -1 || groupId == -1) {
            System.err.println("Lỗi: Không tìm thấy UserID hoặc GroupID hợp lệ!");
            return false;
        }

        // Thực hiện xóa khỏi nhóm
        try (PreparedStatement ps = conn.prepareStatement(deleteMemberSql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }

    } catch (Exception e) {
        System.err.println("Lỗi leaveGroup: " + e.getMessage());
        e.printStackTrace();
    }
    return false;
}

/**
 * Thêm thành viên vào nhóm trò chuyện
 */
public boolean addMemberToGroup(String groupName, String newUsername) {
    String getGroupIdSql = "SELECT GroupID FROM GroupChats WHERE GroupName = ?";
    String getUserIdSql = "SELECT UserID FROM Users WHERE Username = ? AND [Status] = 1";
    String insertMemberSql = "INSERT INTO GroupMembers (GroupID, UserID) " +
                             "SELECT ?, ? WHERE NOT EXISTS (" +
                             "    SELECT 1 FROM GroupMembers WHERE GroupID = ? AND UserID = ?" +
                             ")";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
        int groupId = -1;
        int userId = -1;

        try (PreparedStatement ps = conn.prepareStatement(getGroupIdSql)) {
            ps.setString(1, groupName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) groupId = rs.getInt("GroupID");
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(getUserIdSql)) {
            ps.setString(1, newUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) userId = rs.getInt("UserID");
            }
        }

        if (groupId == -1 || userId == -1) {
            return false;
        }

        try (PreparedStatement ps = conn.prepareStatement(insertMemberSql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.setInt(3, groupId);
            ps.setInt(4, userId);
            
            return ps.executeUpdate() > 0;
        }

    } catch (Exception e) {
        System.err.println("Lỗi addMemberToGroup: " + e.getMessage());
        e.printStackTrace();
    }
    return false;
}
/**
 * Tìm danh sách User theo từ khóa Username mà CHƯA có trong nhóm
 * Trả về danh sách chuỗi định dạng: "Username (ID: UserID)"
 */
public List<String> searchUsersNotInGroup(String keyword, String groupName) {
    List<String> userList = new ArrayList<>();
    String sql = "SELECT UserID, Username FROM Users " +
                 "WHERE Username LIKE ? AND [Status] = 1 " +
                 "  AND UserID NOT IN (" +
                 "      SELECT GM.UserID " +
                 "      FROM GroupMembers GM " +
                 "      JOIN GroupChats GC ON GM.GroupID = GC.GroupID " +
                 "      WHERE GC.GroupName = ?" +
                 "  )";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, "%" + keyword + "%"); // Tìm gần đúng theo từ khóa
        ps.setString(2, groupName);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("UserID");
                String uname = rs.getString("Username");
                userList.add(uname + " (ID: " + id + ")");
            }
        }
    } catch (Exception e) {
        System.err.println("Lỗi searchUsersNotInGroup: " + e.getMessage());
        e.printStackTrace();
    }
    return userList;
}

    // --- LẤY LỊCH SỬ TIN NHẮN TỪ DATABASE ---

    public List<String[]> getChatHistory(String myUsername, String friendUsername) {
        List<String[]> history = new ArrayList<>();
        String sql = "SELECT (SELECT Username FROM Users WHERE UserID = m.SenderID) AS Sender, [Content] FROM Messages m " +
                     "WHERE GroupID IS NULL AND (" +
                     "  (SenderID = (SELECT UserID FROM Users WHERE Username = ?) AND ReceiverID = (SELECT UserID FROM Users WHERE Username = ?)) " +
                     "  OR " +
                     "  (SenderID = (SELECT UserID FROM Users WHERE Username = ?) AND ReceiverID = (SELECT UserID FROM Users WHERE Username = ?))" +
                     ") ORDER BY [Timestamp] ASC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, myUsername); ps.setString(2, friendUsername);
            ps.setString(3, friendUsername); ps.setString(4, myUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new String[]{rs.getString("Sender"), rs.getString("Content")});
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi load chat cá nhân: " + e.getMessage());
        }
        return history;
    }

    public List<String[]> getGroupChatHistory(String groupName) {
        List<String[]> history = new ArrayList<>();
        String sql = "SELECT (SELECT Username FROM Users WHERE UserID = m.SenderID) AS Sender, [Content] FROM Messages m " +
                     "WHERE GroupID = (SELECT GroupID FROM GroupChats WHERE GroupName = ?) ORDER BY [Timestamp] ASC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new String[]{rs.getString("Sender"), rs.getString("Content")});
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi load chat nhóm: " + e.getMessage());
        }
        return history;
    }
}