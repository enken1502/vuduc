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

    // Đảm bảo kết nối Socket luôn mới và sạch sẽ trước khi gửi
    ClientSocketManager.getInstance().sendRequest("LOGIN;" + username + ";" + password);
    
    String response = ClientSocketManager.getInstance().receiveResponse();
    
    // Nếu vô tình lần đầu bị lag ra null, cho nó thử đọc lại thêm 1 lần nữa ngay lập tức cứu vãn
    if (response == null) {
        response = ClientSocketManager.getInstance().receiveResponse();
    }
    
    if (response != null && response.trim().toUpperCase().contains("LOGIN_SUCCESS")) {
        MainChatForm mainChat = new MainChatForm(username);
        mainChat.activateListening();
        dispose();
    } else {
        JOptionPane.showMessageDialog(this, "Tài khoản hoặc mật khẩu không chính xác! (Hoặc lỗi kết nối)", "Lỗi", JOptionPane.ERROR_MESSAGE);
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