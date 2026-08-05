package app;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainServer {
    private static final int PORT = 9999;
    
    private static final String DB_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=ChatDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "minh";
    private static final String DB_PASS = "12345678";

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> otpStorage = new HashMap<>();

    private static final String LOG_FILE = "server_activity.log";

    public static void main(String[] args) {
        System.out.println("=== SERVER SOCKET TCP ===");
        
        if (testDatabaseConnection()) {
            new Thread(MainServer::handleServerConsoleCommands).start();
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server đang chạy và lắng nghe tại cổng: " + PORT);
                System.out.println("💡 Nhập 'help' để xem các lệnh điều khiển Server.");
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (Exception e) {
                System.err.println("Lỗi Server khởi động thất bại: " + e.getMessage());
            }
        } else {
            System.err.println("Dừng Server do không thể kết nối Database!");
        }
    }

    private static boolean testDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            return conn != null;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối DB: " + e.getMessage());
            return false;
        }
    }

   

    // =========================================================================
    // HỆ THỐNG GHI LOG VÀ TIỆN ÍCH SERVER
    // =========================================================================
    public static synchronized void logActivity(String category, String detail) {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = String.format("[%s] [%s] %s", timeStamp, category.toUpperCase(), detail);
        
        System.out.println("📌 " + logEntry);

        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(logEntry + "\n");
        } catch (IOException e) {
            System.err.println("❌ Lỗi ghi log file: " + e.getMessage());
        }
    }
    // =========================================================================
    // XỬ LÝ LỆNH ADMIN TỪ MÀN HÌNH CONSOLE SERVER
    // =========================================================================
    private static void handleServerConsoleCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String commandLine = scanner.nextLine().trim();
            if (commandLine.isEmpty()) continue;

            String[] parts = commandLine.split(" ", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";

            switch (cmd) {
                case "help":
                    System.out.println("\n--- DANH SÁCH LỆNH ADMIN SERVER ---");
                    System.out.println("1. stats             : Xem thống kê tổng số user và danh sách online");
                    System.out.println("2. announce <text>   : Gửi tin nhắn hệ thống tới tất cả Client");
                    System.out.println("3. block <username (ID:)>  : Khóa tài khoản và ngắt kết nối người dùng ngay lập tức");
                    System.out.println("4. unblock <username (ID:)>: Mở khóa tài khoản người dùng");
                    System.out.println("-----------------------------------\n");
                    break;

                case "stats":
                    showServerStats();
                    break;

                case "announce":
                    if (arg.isEmpty()) {
                        System.out.println("⚠️ Cú pháp: announce <nội dung>");
                    } else {
                        broadcastSystemAnnouncement(arg);
                        System.out.println("📢 Đã gửi thông báo hệ thống!");
                    }
                    break;

                case "block":
                    if (arg.isEmpty()) {
                        System.out.println("⚠️ Cú pháp: block <username>");
                    } else {
                        blockUser(arg);
                    }
                    break;

                case "unblock":
                    if (arg.isEmpty()) {
                        System.out.println("⚠️ Cú pháp: unblock <username>");
                    } else {
                        unblockUser(arg);
                    }
                    break;

                default:
                    System.out.println("❌ Lệnh không hợp lệ. Nhập 'help' để xem hướng dẫn.");
                    break;
            }
        }
    }

    private static void showServerStats() {
        int totalUsers = 0;
        String sql = "SELECT COUNT(*) AS Total FROM Users";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                totalUsers = rs.getInt("Total");
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi thống kê DB: " + e.getMessage());
        }

        System.out.println("\n=== THỐNG KÊ HỆ THỐNG ===");
        System.out.println("📊 Tổng số tài khoản trong hệ thống: " + totalUsers);
        System.out.println("🟢 Số client đang online: " + getOnlineCount());
        System.out.print("👤 Danh sách User Online: ");
        
        List<String> onlineNames = new ArrayList<>();
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.username != null) {
                    onlineNames.add(client.username);
                }
            }
        }
        System.out.println(onlineNames.isEmpty() ? "Không có ai" : String.join(", ", onlineNames));
        System.out.println("===========================\n");
    }
    private static boolean isUserActive(String username) {
    if (username == null) return false;
    String sql = "SELECT [Status] FROM Users WHERE Username = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("Status") == 1;
        }
    } catch (Exception e) {
        System.err.println("Lỗi kiểm tra status user: " + e.getMessage());
    }
    return false;
}

    private static int getOnlineCount() {
        int count = 0;
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.username != null) count++;
            }
        }
        return count;
    }

   private static void broadcastSystemAnnouncement(String text) {
    // Định dạng gói tin riêng gửi về Client
    String msg = "ANNOUNCEMENT;" + text;
    synchronized (clients) {
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }
    logActivity("ANNOUNCEMENT", "Gửi thông báo: " + text);
}

   private static void blockUser(String input) {
    if (input == null || input.trim().isEmpty()) return;
    input = input.trim();

    int userId = -1;
    String targetUsername = "";

    // Bóc tách ID nếu truyền vào dạng "Username (ID: 5)"
    if (input.contains("(ID:") && input.endsWith(")")) {
        try {
            int idStart = input.indexOf("(ID:") + 4;
            int idEnd = input.indexOf(")", idStart);
            userId = Integer.parseInt(input.substring(idStart, idEnd).trim());
            targetUsername = input.substring(0, input.indexOf("(ID:")).trim();
        } catch (Exception e) {
            System.out.println("⚠️ Định dạng ID không hợp lệ!");
            return;
        }
    } else if (input.matches("\\d+")) { // Nhập mỗi số ID (VD: block 5)
        userId = Integer.parseInt(input);
    } else { // Nhập mỗi Username
        targetUsername = input;
    }

    // SQL Cập nhật và SQL Tìm thông tin user
    String updateSql = (userId != -1) 
            ? "UPDATE Users SET [Status] = 0 WHERE UserID = ?" 
            : "UPDATE Users SET [Status] = 0 WHERE Username = ?";
    
    String findUserSql = (userId != -1)
            ? "SELECT UserID, Username FROM Users WHERE UserID = ?"
            : "SELECT UserID, Username FROM Users WHERE Username = ?";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
        // Tìm thông tin chính xác của User
        try (PreparedStatement psFind = conn.prepareStatement(findUserSql)) {
            if (userId != -1) psFind.setInt(1, userId);
            else psFind.setString(1, targetUsername);

            try (ResultSet rs = psFind.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("UserID");
                    targetUsername = rs.getString("Username");
                } else {
                    System.out.println("⚠️ Không tìm thấy user: " + input);
                    return;
                }
            }
        }

        // Thực hiện Block trong DB
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            if (userId != -1) ps.setInt(1, userId);
            else ps.setString(1, targetUsername);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("🔒 Đã khóa tài khoản: " + targetUsername + " (ID: " + userId + ")");
                logActivity("BLOCK_USER", "Khóa tài khoản: " + targetUsername + " (ID: " + userId + ")");

                // Kích người dùng ra khỏi Server ngay lập tức
                synchronized (clients) {
                    for (ClientHandler client : clients) {
                        // So sánh theo Username hoặc theo ID (nếu ClientHandler có lưu userId)
                        if (client.username != null && client.username.equalsIgnoreCase(targetUsername)) {
                            client.sendMessage("FORCE_LOGOUT;Tài khoản của bạn đã bị KHÓA bởi Admin!");

                            new Thread(() -> {
                                try { Thread.sleep(200); } catch (Exception ignored) {}
                                client.kickClient();
                            }).start();
                            break;
                        }
                    }
                }
            } else {
                System.out.println("⚠️ Không tìm thấy user: " + input);
            }
        }
    } catch (Exception e) {
        System.err.println("❌ Lỗi khóa tài khoản: " + e.getMessage());
    }
}

    private static void unblockUser(String input) {
    if (input == null || input.trim().isEmpty()) return;
    input = input.trim();

    int userId = -1;
    String targetUsername = "";

    // Bóc tách ID nếu truyền vào dạng "Username (ID: 5)"
    if (input.contains("(ID:") && input.endsWith(")")) {
        try {
            int idStart = input.indexOf("(ID:") + 4;
            int idEnd = input.indexOf(")", idStart);
            userId = Integer.parseInt(input.substring(idStart, idEnd).trim());
            targetUsername = input.substring(0, input.indexOf("(ID:")).trim();
        } catch (Exception e) {
            System.out.println("⚠️ Định dạng ID không hợp lệ!");
            return;
        }
    } else if (input.matches("\\d+")) {
        userId = Integer.parseInt(input);
    } else {
        targetUsername = input;
    }

    String updateSql = (userId != -1) 
            ? "UPDATE Users SET [Status] = 1 WHERE UserID = ?" 
            : "UPDATE Users SET [Status] = 1 WHERE Username = ?";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = conn.prepareStatement(updateSql)) {

        if (userId != -1) ps.setInt(1, userId);
        else ps.setString(1, targetUsername);

        int rows = ps.executeUpdate();
        if (rows > 0) {
            String displayInfo = (userId != -1) ? (targetUsername + " (ID: " + userId + ")") : input;
            System.out.println("🔓 Đã mở khóa tài khoản: " + displayInfo);
            logActivity("UNBLOCK_USER", "Admin đã mở khóa tài khoản: " + displayInfo);
        } else {
            System.out.println("⚠️ Không tìm thấy người dùng: " + input);
        }
    } catch (Exception e) {
        System.err.println("❌ Lỗi mở khóa tài khoản: " + e.getMessage());
    }
}

    // Gửi tin nhắn real-time tới một User cụ thể đang online
    public static synchronized void sendToUser(String targetUsername, String message) {
        for (ClientHandler client : clients) {
            if (client.username != null && client.username.equalsIgnoreCase(targetUsername.trim())) {
                client.sendMessage(message);
                break;
            }
        }
    }
   

    // Gửi tin nhắn real-time tới tất cả thành viên trong một nhóm đang online
    public static synchronized void broadcastToGroup(String groupName, String message, String senderUsername) {
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

        for (ClientHandler client : clients) {
            if (client.username != null && !client.username.equalsIgnoreCase(senderUsername)) {
                for (String member : members) {
                    if (client.username.equalsIgnoreCase(member)) {
                        client.sendMessage(message);
                        break;
                    }
                }
            }
        }
    }

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
        public void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
                out.flush();
            }
        }

        public void kickClient() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {}
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

// =========================================================
// 1. XỬ LÝ LỆNH GỬI OTP (Có kiểm tra Email tồn tại)
// =========================================================
if (action.equals("SEND_OTP")) {
    String email = tokens[1].trim();

    // B1: Kiểm tra Email đã tồn tại trong DB chưa
    boolean isExist = false;
    String checkEmailSql = "SELECT UserID FROM Users WHERE Email = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = conn.prepareStatement(checkEmailSql)) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) isExist = true;
        }
    } catch (Exception ex) {
        System.err.println("Lỗi check Email: " + ex.getMessage());
    }

    // Nếu ĐĂNG KÝ mà Email ĐÃ TỒN TẠI -> Báo lỗi ngay
    if (isExist) {
        out.println("SEND_OTP_FAILED;Email này đã được sử dụng cho tài khoản khác!");
        continue;
    }

    // B2: Tạo OTP ngẫu nhiên & Lưu vào Map otpStorage của Server
    String generatedOTP = view.GmailSender.generateOTP();
    otpStorage.put(email, generatedOTP); // Lưu OTP theo Key là Email
    
    System.out.println("🔑 [SERVER OTP DEBUG] OTP cho " + email + " là: " + generatedOTP);

    // B3: Thực sự gọi GmailSender để bắn Mail
    boolean sendSuccess = view.GmailSender.sendOTP(email, generatedOTP);

    if (sendSuccess) {
        out.println("OTP_SENT_SUCCESS");
    } else {
        out.println("SEND_OTP_FAILED;Không thể gửi email OTP. Vui lòng thử lại!");
    }
}
                    
                    else if (action.equals("REGISTER")) {
    // Cú pháp từ Client: REGISTER;<name>;<email>;<password>;<otp>
    String name = tokens[1].trim();
    String email = tokens[2].trim();
    String pass = tokens[3].trim();
    String userOtp = tokens[4].trim();

    // B1: Kiểm tra OTP nhập vào có đúng với OTP đã lưu trong otpStorage không
    String realOtp = otpStorage.get(email);

    if (realOtp == null || !realOtp.equals(userOtp)) {
        out.println("INVALID_OTP"); // Trả về đúng chuỗi RegisterForm đang chờ
        continue;
    }

    // B2: OTP đúng -> Tiến hành Lưu vào Cơ sở dữ liệu
    String sql = "INSERT INTO Users (Username, [Password], Email, [Status]) VALUES (?, ?, ?, 1)";
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, name);
        ps.setString(2, pass);
        ps.setString(3, email);
        ps.executeUpdate();

        // Đăng ký xong thì xóa OTP đi để không tái sử dụng
        otpStorage.remove(email);
        
        out.println("REGISTER_SUCCESS");
    } catch (Exception ex) {
        out.println("REGISTER_FAILED;" + ex.getMessage());
    }
}
//Login
                    else if (action.equals("LOGIN")) {
    String user = tokens[1].trim();
    String pass = tokens[2].trim();
    
    // Tra cứu UserID và Status
    String sql = "SELECT UserID, [Status] FROM Users WHERE Username = ? AND [Password] = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user);
        ps.setString(2, pass);
        
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int status = rs.getInt("Status");
                if (status == 0) {
                    // 🌟 Nếu bị khóa, trả về thông báo cụ thể
                    out.println("LOGIN_FAILED;Tài khoản này đã bị khóa!");
                } else {
                    this.currentUserId = rs.getInt("UserID");
                    this.username = user;
                    out.println("LOGIN_SUCCESS;" + this.currentUserId);
                }
            } else {
                out.println("LOGIN_FAILED;Tài khoản hoặc mật khẩu không chính xác!");
            }
        }
    } catch (Exception ex) {
        out.println("LOGIN_FAILED;Lỗi kết nối cơ sở dữ liệu!");
    }
}

                   // 4. XỬ LÝ CHAT CÁ NHÂN: CHAT;tên_người_nhận;nội_dung_tin_nhắn
else if (action.equals("CHAT")) {
    if (!isUserActive(this.username)) {
        out.println("RECEIVE_MSG;HỆ THỐNG;Tài khoản của bạn đã bị KHÓA, không thể gửi tin nhắn!");
        continue;
    }
    
    String targetUser = tokens[1].trim();
    String msgContent = tokens[2].trim();
    if (this.currentUserId == -1) this.currentUserId = 1;

    int receiverId = -1;
    String findUserSql = "SELECT UserID FROM Users WHERE Username = ? AND [Status] = 1";
    String insertMsgSql = "INSERT INTO Messages (SenderID, ReceiverID, GroupID, [Content], [Type], [Timestamp]) VALUES (?, ?, NULL, ?, 'TEXT', GETDATE())";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
        try (PreparedStatement psFind = conn.prepareStatement(findUserSql)) {
            psFind.setString(1, targetUser);
            try (ResultSet rs = psFind.executeQuery()) {
                if (rs.next()) {
                    receiverId = rs.getInt("UserID");
                }
            }
        }

        // Nếu người nhận bị block hoặc không tồn tại -> Trả về lỗi và DỪNG LẠI
        if (receiverId == -1) {
            out.println("SEND_MSG_FAILED;Tài khoản không tồn tại");
            continue;
        }

        // Lưu vào DB
        try (PreparedStatement psInsert = conn.prepareStatement(insertMsgSql)) {
            psInsert.setInt(1, this.currentUserId);
            psInsert.setInt(2, receiverId);
            psInsert.setString(3, msgContent);
            psInsert.executeUpdate();
        }

if (!targetUser.equalsIgnoreCase(this.username)) {
    MainServer.sendToUser(targetUser, "RECEIVE_MSG;" + this.username + ";" + msgContent);
}
        // 🌟 GỬI LẠI CHO CHÍNH NGƯỜI GỬI (để Client người gửi tự in tin nhắn lên màn hình)
out.println("RECEIVE_MSG;" + this.username + ";" + msgContent);

    } catch (Exception ex) {
        System.err.println("Lỗi xử lý lệnh CHAT: " + ex.getMessage());
    }
}
                    // 5. XỬ LÝ CHAT NHÓM: GROUP_CHAT;tên_nhóm_nhận;nội_dung_tin_nhắn
                    else if (action.equals("GROUP_CHAT")) {
                        String targetGroup = tokens[1].trim();
                        String msgContent = tokens[2].trim();
                        if (!isUserActive(this.username)) {
        out.println("RECEIVE_MSG;HỆ THỐNG;Tài khoản của bạn đã bị KHÓA, không thể gửi tin nhắn!");
        continue;
    }
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
                    // =========================================================
                    // 6. XỬ LÝ LẤY THÔNG TIN CÁ NHÂN (PROFILE)
                    // =========================================================
                    else if (action.equals("GET_PROFILE")) {
                        int userId = Integer.parseInt(tokens[1].trim());
                        String sql = "SELECT UserID, Username, Email, [Status] FROM Users WHERE UserID = ?";
                        
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, userId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    String uId = String.valueOf(rs.getInt("UserID"));
                                    String uName = rs.getString("Username");
                                    String uEmail = rs.getString("Email");
                                    String uStatus = rs.getInt("Status") == 1 ? "Hoạt động" : "Bị khóa";
                                    
                                    // Gửi chuỗi dữ liệu profile về cho Client
                                    out.println("PROFILE_DATA;" + uId + ";" + uName + ";" + uEmail + ";" + uStatus);
                                    out.flush(); // <--- ÉP ĐẨY DỮ LIỆU SANG CLIENT NGAY LẬP TỨC
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Lỗi GET_PROFILE: " + ex.getMessage());
                        }
                    }

                    // =========================================================
                    // 7. XỬ LÝ LẤY DANH SÁCH BẠN BÈ
                    // =========================================================
                    else if (action.equals("GET_FRIENDS")) {
                        String currentUserName = tokens[1].trim();
                        StringBuilder friendsList = new StringBuilder("FRIEND_LIST");
                        
                        String sql = "SELECT receiver_username FROM Friends WHERE sender_username = ? AND status = 'ACCEPTED' " +
                                     "UNION " +
                                     "SELECT sender_username FROM Friends WHERE receiver_username = ? AND status = 'ACCEPTED'";
                        
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, currentUserName);
                            ps.setString(2, currentUserName);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    friendsList.append(";").append(rs.getString(1));
                                }
                            }
                            out.println(friendsList.toString());
                            out.flush(); // <--- ÉP ĐẨY DANH SÁCH BẠN BÈ SANG CLIENT
                        } catch (Exception ex) {
                            System.err.println("Lỗi GET_FRIENDS: " + ex.getMessage());
                        }
                    }

                    // =========================================================
                    // 8. XỬ LÝ LẤY DANH SÁCH NHÓM
                    // =========================================================
                    else if (action.equals("GET_GROUPS")) {
                        int userId = Integer.parseInt(tokens[1].trim());
                        StringBuilder groupsList = new StringBuilder("GROUP_LIST");
                        
                        String sql = "SELECT g.GroupName FROM GroupChats g " +
                                     "JOIN GroupMembers gm ON g.GroupID = gm.GroupID " +
                                     "WHERE gm.UserID = ?";
                                     
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, userId);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    groupsList.append(";").append(rs.getString("GroupName"));
                                }
                            }
                            out.println(groupsList.toString());
                            out.flush(); // <--- ÉP ĐẨY DANH SÁCH NHÓM SANG CLIENT
                        } catch (Exception ex) {
                            System.err.println("Lỗi GET_GROUPS: " + ex.getMessage());
                        }
                    }
                    //Xử lý update profile
                        else if (action.equals("UPDATE_PROFILE")) {
                        int userId = Integer.parseInt(tokens[1].trim());
                        String newUsername = tokens[2].trim();
                        String newPassword = tokens[3].trim();

                        boolean hasNewPassword = !newPassword.equals("KEEP_OLD_PASS") && !newPassword.isEmpty();
                        String sql;

                        if (hasNewPassword) {
                            sql = "UPDATE Users SET Username = ?, [Password] = ? WHERE UserID = ?";
                        } else {
                            sql = "UPDATE Users SET Username = ? WHERE UserID = ?";
                        }

                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            
                            ps.setString(1, newUsername);
                            if (hasNewPassword) {
                                ps.setString(2, newPassword);
                                ps.setInt(3, userId);
                            } else {
                                ps.setInt(2, userId);
                            }

                            int rows = ps.executeUpdate();
                            if (rows > 0) {
                                this.username = newUsername; // Cập nhật lại username mới cho Session Socket online hiện tại
                                out.println("UPDATE_PROFILE_SUCCESS;Cập nhật thông tin thành công!");
                                System.out.println("⚙️ [UPDATE_PROFILE]: Đã đổi thông tin cho UserID: " + userId + " -> Username mới: " + newUsername);
                            } else {
                                out.println("UPDATE_PROFILE_FAILED;Không tìm thấy người dùng!");
                            }
                        } catch (Exception ex) {
                            System.err.println("❌ Lỗi UPDATE_PROFILE: " + ex.getMessage());
                            out.println("UPDATE_PROFILE_FAILED;Tên đăng nhập đã tồn tại hoặc bị lỗi!");
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