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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class MainServer {
    private static final int PORT = 9999;

    // 🛢️ CẤU HÌNH KẾT NỐI DATABASE SQL SERVER
    private static final String DB_URL = "jdbc:sqlserver://10.20.1.69:1433;databaseName=ChatDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "minh";
    private static final String DB_PASS = "1234567";

    // 💾 Bộ nhớ đệm lưu trữ OTP tạm thời ngay trên RAM của Server
    private static final Map<String, String> otpStorage = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("=== SERVER SOCKET TCP - FULL FIX LOGIC VÀ PHÒNG CHỐNG SPAM MAIL ===");
        
        if (testDatabaseConnection()) {
            System.out.println("\n=================================");
            System.out.println(" ✅ KẾT NỐI DATABASE THÀNH CÔNG");
            System.out.println("=================================");
        } else {
            System.out.println("\n ❌ Cảnh báo: Kết nối database thất bại!");
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("\n=== SERVER ĐÃ SẴN SÀNG, ĐANG CHỜ CLIENT KẾT NỐI (PORT " + PORT + ") ===");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có Client kết nối qua Socket: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi chạy Server Socket: " + e.getMessage());
        }
    }

    private static boolean testDatabaseConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // LUỒNG XỬ LÝ ĐA LUỒNG CHO TỪNG KHÁCH HÀNG KẾT NỐI
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private int currentUserId = -1; // Lưu ID người dùng sau khi Đăng nhập/Đăng ký để phục vụ chat

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println("SERVER_CONNECTED");

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Server nhận được chuỗi: " + request);
                    
                    String[] tokens = request.split(";");
                    String action = tokens[0];

                    // 🛠️ 1. XỬ LÝ LỆNH: SEND_OTP
                    if (action.equals("SEND_OTP")) {
                        String emailRegister = tokens[1];

                        boolean isEmailExist = false;
                        String checkSql = "SELECT 1 FROM Users WHERE Email = ?";
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(checkSql)) {
                            ps.setString(1, emailRegister);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    isEmailExist = true;
                                }
                            }
                        }

                        if (isEmailExist) {
                            out.println("EMAIL_ALREADY_USED");
                            System.out.println("⚠️ [OTP]: Từ chối cấp mã vì Email [" + emailRegister + "] đã tồn tại.");
                        } else {
                            String otpCode = view.GmailSender.generateOTP();
                            boolean sendSuccess = view.GmailSender.sendOTP(emailRegister, otpCode);
                            
                            if (sendSuccess) {
                                otpStorage.put(emailRegister, otpCode);
                                out.println("OTP_SENT_SUCCESS");
                            } else {
                                out.println("OTP_SEND_FAILED");
                            }
                        }
                    }
                    
                    // 🛠️ 2. XỬ LÝ LỆNH: REGISTER
                    else if (action.equals("REGISTER")) {
                        String username = tokens[1];
                        String email = tokens[2];
                        String password = tokens[3];
                        String userOTP = tokens[4];

                        String systemOTP = otpStorage.get(email);

                        if (systemOTP != null && systemOTP.equals(userOTP)) {
                            // Khớp OTP -> Thực hiện INSERT đồng thời yêu cầu trả về ID vừa sinh tự động (RETURN_GENERATED_KEYS)
                            String insertSql = "INSERT INTO Users (Username, [Password], Email, [Status]) VALUES (?, ?, ?, 1)";
                            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                                 PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                                ps.setString(1, username);
                                ps.setString(2, password);
                                ps.setString(3, email);
                                
                                int rows = ps.executeUpdate();
                                if (rows > 0) {
                                    // Lấy UserID thực tế vừa sinh dưới SQL Server lên để gán vào bộ nhớ tạm luồng chat
                                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                                        if (generatedKeys.next()) {
                                            this.currentUserId = generatedKeys.getInt(1); 
                                        }
                                    }
                                    out.println("REGISTER_SUCCESS");
                                    System.out.println("🎉 [REGISTRATION]: Tài khoản '" + username + "' (ID: " + this.currentUserId + ") khởi tạo thành công!");
                                    otpStorage.remove(email);
                                } else {
                                    out.println("REGISTER_FAILED");
                                }
                            }
                        } else {
                            out.println("WRONG_OTP");
                            System.out.println("❌ [REGISTRATION]: Nhập sai mã OTP cho tài khoản " + username);
                        }
                    }

                    // 🛠️ 3. XỬ LÝ LỆNH CHAT: KHẮC PHỤC LỖI FOREIGN KEY CONSTRAINT CHAT LÚC NÃY
                    else if (action.equals("CHAT")) {
                        String msgContent = tokens[1];

                        // Nếu chưa có tài khoản hoặc chưa login mà đã cố phát tin nhắn chat
                        if (this.currentUserId == -1) {
                            // Để test nhanh luồng gửi, gán tạm thời một ID tồn tại hợp lệ dưới DB nếu bạn chưa làm Form Login
                            this.currentUserId = 1; 
                        }

                        // Thực hiện câu lệnh INSERT an toàn vào bảng Messages
                        String insertMsgSql = "INSERT INTO Messages (SenderID, Content, [Type], [Timestamp]) VALUES (?, ?, 'TEXT', GETDATE())";
                        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                             PreparedStatement ps = conn.prepareStatement(insertMsgSql)) {
                            ps.setInt(1, this.currentUserId);
                            ps.setString(2, msgContent);
                            
                            ps.executeUpdate();
                            System.out.println("💾 [DATABASE]: Đã lưu tin nhắn từ UserID " + this.currentUserId + " vào bảng Messages.");
                        } catch (Exception ex) {
                            System.err.println("❌ [LỖI LƯU DATABASE]: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                // Ngắt luồng an toàn
            }
        }
    }
}