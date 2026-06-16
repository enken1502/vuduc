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
    
    // Thay đổi IP máy chạy Server và Port cho đúng cấu hình của nhóm bạn
    private final String SERVER_IP = "localhost"; 
    private final int SERVER_PORT = 9999;

    private ClientSocketManager() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối tới Server thành công qua Socket!");
        } catch (Exception e) {
            System.err.println("Không thể kết nối tới Server: " + e.getMessage());
        }
    }

    // Sử dụng cơ chế Singleton để tất cả các Form dùng chung 1 kết nối duy nhất
    public static ClientSocketManager getInstance() {
        if (instance == null || instance.socket == null || instance.socket.isClosed()) {
            instance = new ClientSocketManager();
        }
        return instance;
    }

    // Hàm gửi tin nhắn/lệnh lên Server
    public void sendRequest(String request) {
        if (out != null) {
            out.println(request);
        }
    }

    // Hàm nhận phản hồi từ Server
    public String receiveResponse() {
        try {
            if (in != null) {
                return in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
