package view;

import javax.swing.*;

public class LoginForm extends JFrame {

    public LoginForm() {
        setTitle("Đăng nhập");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lbUser = new JLabel("Tên đăng nhập:");
        lbUser.setBounds(30, 40, 120, 30);
        JTextField txtUser = new JTextField();
        txtUser.setBounds(150, 40, 180, 30);

        JLabel lbPass = new JLabel("Mật khẩu:");
        lbPass.setBounds(30, 90, 120, 30);
        JPasswordField txtPass = new JPasswordField();
        txtPass.setBounds(150, 90, 180, 30);

        JButton btnLogin = new JButton("Đăng nhập");
        btnLogin.setBounds(50, 170, 120, 35);

        // SỬA LẠI SỰ KIỆN BTNLOGIN TRONG LOGINFORM.JAVA
       btnLogin.addActionListener(e -> {
    String username = txtUser.getText().trim();
    String password = new String(txtPass.getPassword()).trim();

    if (username.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Vui lòng nhập tài khoản và mật khẩu!");
        return;
    }

    try {
        // 1. Chỉ gọi reconnect nếu socket bị đóng (tránh kết nối thừa thãi gây lỗi)
        if (ClientSocketManager.getInstance().getSocket() == null || 
            ClientSocketManager.getInstance().getSocket().isClosed()) {
            ClientSocketManager.getInstance().reconnect();
        }

        // 2. GIỮ NGUYÊN lệnh gửi LOGIN gốc của nhóm ông
        ClientSocketManager.getInstance().sendRequest("LOGIN;" + username + ";" + password);
        
        // 3. Đọc phản hồi về
        String response = ClientSocketManager.getInstance().receiveResponse();
        System.out.println("➔ [CLIENT_DEBUG] Server phản hồi: " + response);
        
        if (response == null) {
            response = ClientSocketManager.getInstance().receiveResponse(); // Đọc lại phòng hờ lag
        }
        
        // 4. Kiểm tra phản hồi (vừa check chữ hoa/thường, vừa check cả từ khóa của nhóm ông)
        if (response != null) {
            String upperRes = response.trim().toUpperCase();
            if (upperRes.contains("LOGIN_SUCCESS") || 
                upperRes.contains("SUCCESS") || 
                upperRes.contains("THÀNH CÔNG") || 
                upperRes.contains("OK")) {
                
                // Đăng nhập thành công -> Chuyển trang
                MainChatForm mainChat = new MainChatForm(username);
                mainChat.activateListening();
                dispose(); 
                return;
            }
        }
        
        // Nếu không thỏa mãn điều kiện trên
        JOptionPane.showMessageDialog(this, "Tài khoản hoặc mật khẩu không chính xác!", "Lỗi", JOptionPane.ERROR_MESSAGE);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Lỗi kết nối Socket: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }
});

        JButton btnRegister = new JButton("Đăng ký");
        btnRegister.setBounds(200, 170, 120, 35);
        btnRegister.addActionListener(e -> {
            new RegisterForm();
        });

        panel.add(lbUser);
        panel.add(txtUser);
        panel.add(lbPass);
        panel.add(txtPass);
        panel.add(btnLogin);
        panel.add(btnRegister);

        add(panel);
        setVisible(true);
    }
}