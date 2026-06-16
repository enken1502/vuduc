package app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class MainServer {
    private static final int PORT = 9999;

    // 🛢️ CẤU HÌNH KẾT NỐI: Dùng tài khoản minh / mật khẩu 1234567 qua cổng tĩnh 1433
    private static final String DB_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=ChatDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "minh";
    private static final String DB_PASS = "1234567";

    public static void main(String[] args) {
        System.out.println("=== SERVER SOCKET TCP - KẾT NỐI QUA TÀI KHOẢN MINH (SQL AUTH) ===");
        
        System.out.println("-> Đang thử đăng nhập vào SQL Server qua tài khoản '" + DB_USER + "'...");
        if (testDatabaseConnection()) {
            System.out.println("\n=======================================================");
            System.out.println("🎉 🎉 KẾT NỐI DATABASE THÀNH CÔNG RỒI BẠN ƠI! 🎉 🎉");
            System.out.println("=======================================================");
        } else {
            System.out.println("\n❌ Cảnh báo: Kết nối thất bại! Hãy chắc chắn tài khoản 'minh' đã được kích hoạt trong SSMS.");
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("\n=== 🚀 SERVER ĐÃ SẴN SÀNG, ĐANG CHỜ CLIENT KẾT NỐI (PORT " + PORT + ") ===");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📬 Có Client kết nối qua Socket: " + clientSocket.getInetAddress());
                
                // Tạo luồng riêng (Thread) để xử lý dữ liệu chat từ mỗi Client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi chạy Server Socket: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("SERVER_CONNECTED");

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("📥 Server nhận được chuỗi: " + request);
                
                // Gọi hàm lưu nội dung tin nhắn trực tiếp vào SQL Server
                saveMessageToDatabase(request);
                
                out.println("SERVER_RECEIVED_OK");
            }
        } catch (Exception e) {
            System.out.println("🔌 Một Client đã ngắt kết nối.");
        }
    }

    // 🛢️ 1. HÀM KIỂM TRA ĐƯỜNG TRUYỀN (ĐÃ TRUYỀN ĐỦ 3 THAM SỐ)
    private static boolean testDatabaseConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // Bắt buộc truyền đủ URL, USER, PASS khi dùng tài khoản SQL
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Chi tiết lỗi kết nối ngầm: " + e.getMessage());
            return false;
        }
    }

    // 🛢️ 2. HÀM GHI TIN NHẮN VÀO DATABASE (ĐÃ TRUYỀN ĐỦ 3 THAM SỐ)
   // 🛢️ HÀM GHI TIN NHẮN VÀO DATABASE ĐÃ ĐỒNG BỘ CÁC CỘT THEO HÌNH BẠN GỬI
    private static void saveMessageToDatabase(String content) {
        // Khớp hoàn toàn 7 cột theo cấu trúc bảng thực tế của bạn
        String sql = "INSERT INTO Messages (SenderID, ReceiverID, GroupID, [Content], [Type], Timestamp) " +
                     "VALUES (1, NULL, NULL, ?, 'Text', GETDATE())";
                     
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, content); // Nạp nội dung tin nhắn chat vào dấu chấm hỏi (?)
                
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("💾 [SQL SERVER]: Đã chèn dữ liệu thành công vào bảng 7 cột!");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [LỖI LƯU DATABASE]: " + e.getMessage());
        }
    }
}