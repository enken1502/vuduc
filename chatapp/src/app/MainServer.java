package app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainServer {
    private static final int PORT = 9999;
    
    // 🛢️ CẤU HÌNH KẾT NỐI DATABASE SQL SERVER CHUẨN CỦA NHÓM
    private static final String DB_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=ChatDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "minh";
    private static final String DB_PASS = "1234567";

    // 💾 Quản lý danh sách các luồng Client đang online để phục vụ Broadcast/Chat real-time
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> otpStorage = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("=== SERVER SOCKET TCP ===");
        
        if (testDatabaseConnection()) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server đang chạy và lắng nghe tại cổng: " + PORT);
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi Server khởi động thất bại: " + e.getMessage());
            }
        } else {
            System.err.println("❌ Dừng Server do không thể kết nối Database!");
        }
    }

    private static boolean testDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            return conn != null;
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối DB: " + e.getMessage());
            return false;
        }
    }

    // Gửi tin nhắn real-time tới một User cụ thể đang online
    public static synchronized void sendToUser(String targetUsername, String message) {
        for (ClientHandler client : clients) {
            if (client.username != null && client.username.equalsIgnoreCase(targetUsername.trim())) {
                client.out.println(message);
                break;
            }
        }
    }

    // Gửi tin nhắn real-time tới tất cả thành viên trong một nhóm đang online
    public static synchronized void broadcastToGroup(String groupName, String message, String senderUsername) {
        // Tìm danh sách Username thuộc nhóm này từ DB
        List<String> members = new ArrayList<>();
        String sql = "SELECT u.Username FROM Users u " +
                     "JOIN GroupMembers gm ON u.UserID = gm.UserID " +
                     "JOIN GroupChats g ON gm.GroupID = g.GroupID " +
                     "WHERE g.GroupName = ?";
                     
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("Username"));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy thành viên nhóm để broadcast: " + e.getMessage());
        }

        // Gửi tin nhắn cho những ai online nằm trong danh sách thành viên nhóm (Trừ người gửi)
        for (ClientHandler client : clients) {
            if (client.username != null && !client.username.equalsIgnoreCase(senderUsername)) {
                for (String member : members) {
                    if (client.username.equalsIgnoreCase(member)) {
                        client.out.println(message);
                        break;
                    }
                }
            }
        }
    }

    // Loại bỏ client ra khỏi danh sách online khi ngắt kết nối
    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // =========================================================================
    // LỚP XỬ LÝ LUỒNG CHO TỪNG CLIENT KẾT NỐI (CLIENT HANDLER)
    // =========================================================================
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int currentUserId = -1; // Lưu ID người dùng sau khi Login thành công
        private String username = null;  // Lưu Username online tương ứng

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String request;

                while ((request = in.readLine()) != null) {
                    System.out.println("📩 Nhận gói tin: " + request);
                    String[] tokens = request.split(";");
                    if (tokens.length == 0) continue;
                    
                    String action = tokens[0];

                    // 1. XỬ LÝ LỆNH GỬI OTP
                    if (action.equals("SEND_OTP")) {
                        String email = tokens[1].trim();
                        // Thực hiện hàm gửi mail OTP cũ của nhóm ông tại đây...
                        out.println("OTP_SENT_SUCCESS");
                    }
                    
                    // 2. XỬ LÝ LỆNH ĐĂNG KÝ (REGISTER)
                    else if (action.equals("REGISTER")) {
                        String user = tokens[1].trim();
                        String pass = tokens[2].trim();
                        String email = tokens[3].trim();
                        
                        String sql = "INSERT INTO Users (Username, [Password], Email, [Status]) VALUES (?, ?, ?, 1)";
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, user);
                            ps.setString(2, pass);
                            ps.setString(3, email);
                            ps.executeUpdate();
                            out.println("REGISTER_SUCCESS");
                        } catch (Exception ex) {
                            out.println("REGISTER_FAILED;" + ex.getMessage());
                        }
                    }

                    // 3. XỬ LÝ LỆNH ĐĂNG NHẬP (LOGIN)
                    else if (action.equals("LOGIN")) {
                        String user = tokens[1].trim();
                        String pass = tokens[2].trim();
                        
                        String sql = "SELECT UserID FROM Users WHERE Username = ? AND [Password] = ? AND [Status] = 1";
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, user);
                            ps.setString(2, pass);
                            
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    this.currentUserId = rs.getInt("UserID");
                                    this.username = user; // Lưu trạng thái định danh online
                                    out.println("LOGIN_SUCCESS");
                                    System.out.println("🔓 [LOGIN]: Tài khoản '" + user + "' (ID: " + this.currentUserId + ") đăng nhập THÀNH CÔNG!");
                                } else {
                                    out.println("LOGIN_FAILED");
                                }
                            }
                        } catch (Exception ex) {
                            out.println("LOGIN_FAILED");
                        }
                    }

                    // 4. XỬ LÝ CHAT CÁ NHÂN: CHAT;tên_người_nhận;nội_dung_tin_nhắn
                    else if (action.equals("CHAT")) {
                        String targetUser = tokens[1].trim();
                        String msgContent = tokens[2].trim(); // Nhận đúng nội dung text (Ví dụ: 123)
                        
                        if (this.currentUserId == -1) this.currentUserId = 1; // Dự phòng
                        
                        int receiverId = -1;
                        String findUserSql = "SELECT UserID FROM Users WHERE Username = ?";
                        String insertMsgSql = "INSERT INTO Messages (SenderID, ReceiverID, GroupID, [Content], [Type], [Timestamp]) VALUES (?, ?, NULL, ?, 'TEXT', GETDATE())";
                        
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                            // Tra cứu ID người nhận
                            try (PreparedStatement psFind = conn.prepareStatement(findUserSql)) {
                                psFind.setString(1, targetUser);
                                try (ResultSet rs = psFind.executeQuery()) {
                                    if (rs.next()) receiverId = rs.getInt("UserID");
                                }
                            }
                            // Lưu Database bảng Messages
                            try (PreparedStatement psInsert = conn.prepareStatement(insertMsgSql)) {
                                psInsert.setInt(1, this.currentUserId);
                                if (receiverId != -1) psInsert.setInt(2, receiverId);
                                else psInsert.setNull(2, java.sql.Types.INTEGER);
                                psInsert.setString(3, msgContent);
                                psInsert.executeUpdate();
                                System.out.println("DB: Đã lưu tin nhắn cá nhân gửi đến [" + targetUser + "]: " + msgContent);
                            }
                            
                            // Gửi tín hiệu real-time sang Client người nhận nếu họ đang online
                            MainServer.sendToUser(targetUser, "RECEIVE_MSG;" + this.username + ";" + msgContent);
                            
                        } catch (Exception ex) {
                            System.err.println("Lỗi xử lý lệnh CHAT: " + ex.getMessage());
                        }
                    }

                    // 5. XỬ LÝ CHAT NHÓM: GROUP_CHAT;tên_nhóm_nhận;nội_dung_tin_nhắn
                    else if (action.equals("GROUP_CHAT")) {
                        String targetGroup = tokens[1].trim();
                        String msgContent = tokens[2].trim();
                        
                        if (this.currentUserId == -1) this.currentUserId = 1;
                        
                        int groupId = -1;
                        String findGroupSql = "SELECT GroupID FROM GroupChats WHERE GroupName = ?";
                        String insertMsgSql = "INSERT INTO Messages (SenderID, ReceiverID, GroupID, [Content], [Type], [Timestamp]) VALUES (?, NULL, ?, ?, 'TEXT', GETDATE())";
                        
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                            // B1: Lấy GroupID từ bảng GroupChats
                            try (PreparedStatement psFind = conn.prepareStatement(findGroupSql)) {
                                psFind.setString(1, targetGroup);
                                try (ResultSet rs = psFind.executeQuery()) {
                                    if (rs.next()) groupId = rs.getInt("GroupID");
                                }
                            }
                            
                            // B2: Lưu vào bảng Messages gán mã GroupID tương ứng
                            if (groupId != -1) {
                                try (PreparedStatement psInsert = conn.prepareStatement(insertMsgSql)) {
                                    psInsert.setInt(1, this.currentUserId);
                                    psInsert.setInt(2, groupId);
                                    psInsert.setString(3, msgContent);
                                    psInsert.executeUpdate();
                                    System.out.println("DB: Đã lưu tin nhắn vào Nhóm [" + targetGroup + "]: " + msgContent);
                                }
                                
                                // B3: Broadcast real-time tới tất cả thành viên trong nhóm đang online
                                MainServer.broadcastToGroup(targetGroup, "RECEIVE_GROUP_MSG;" + targetGroup + ";" + this.username + ";" + msgContent, this.username);
                            }
                        } catch (Exception ex) {
                            System.err.println("Lỗi xử lý lệnh GROUP_CHAT: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("🔌 Client ngắt kết nối: " + (username != null ? username : "Ẩn danh"));
            } finally {
                MainServer.removeClient(this);
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }
}