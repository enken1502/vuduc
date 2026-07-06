package view;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocketManager {
    private static ClientSocketManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 9999;

    // Lưu trữ Session người dùng đăng nhập
    private String username;
    private int userId;
    private String fullName;
    private String email;
    private String phone;

    private ClientSocketManager() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            System.out.println("Đã kết nối tới Server!");
        } catch (Exception e) {
            System.err.println("Không kết nối được Server!");
        }
    }

    public static ClientSocketManager getInstance() {
        if (instance == null || instance.socket == null || instance.socket.isClosed()) {
            instance = new ClientSocketManager();
        }
        return instance;
    }

    public void sendRequest(String request) {
        if (out != null) {
            out.println(request);
        }
    }

    public String receiveResponse() {
        try {
            if (in != null) {
                return in.readLine();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đọc phản hồi: " + e.getMessage());
        }
        return null;
    }

    // Getters và Setters hệ thống
    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getUserId() { return userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getFullName() { return fullName; }
    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPhone() { return phone; }
}