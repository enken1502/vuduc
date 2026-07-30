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
    
private static final String SERVER_IP = "127.0.0.1"; 
private static final int SERVER_PORT = 9999;

    private ClientSocketManager() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            System.out.println("Đã kết nối tới Server thành công qua Socket!");
        } catch (Exception e) {
            System.err.println("Không thể kết nối tới Server: " + e.getMessage());
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
            System.err.println("Lỗi đọc dữ liệu từ Socket: " + e.getMessage());
        }
        return null;
    }
    
    public Socket getSocket() {
    return this.socket; // Trả về biến Socket đang kết nối của Client
}

public void reconnect() {
    try {
        // Đóng socket cũ nếu nó chưa đóng hoàn toàn
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        // Khởi tạo lại kết nối mới sử dụng SERVER_IP và SERVER_PORT có sẵn của ông
        this.socket = new java.net.Socket(SERVER_IP, SERVER_PORT);
        this.out = new java.io.PrintWriter(socket.getOutputStream(), true);
        this.in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), "UTF-8"));
        
        System.out.println("➔ [SOCKET]: Đã kết nối lại thành công!");
    } catch (Exception e) {
        System.err.println("❌ Lỗi reconnect: " + e.getMessage());
    }
}
}