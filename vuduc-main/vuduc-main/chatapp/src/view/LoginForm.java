package view;

import java.awt.Font;
import javax.swing.*;

public class LoginForm extends JFrame {
    private JTextField txtEmail;
    private JPasswordField txtPass;

    public LoginForm() {
        setTitle("Đăng nhập");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(null);

        JLabel title = new JLabel("ĐĂNG NHẬP CHAT APP");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setBounds(90, 20, 250, 30);

        JLabel lbEmail = new JLabel("Email:");
        lbEmail.setBounds(40, 80, 80, 30);
        txtEmail = new JTextField();
        txtEmail.setBounds(130, 80, 200, 30);

        JLabel lbPass = new JLabel("Mật khẩu:");
        lbPass.setBounds(40, 130, 80, 30);
        txtPass = new JPasswordField();
        txtPass.setBounds(130, 130, 200, 30);

        JButton btnLogin = new JButton("Đăng nhập");
        btnLogin.setBounds(60, 190, 120, 35);

        JButton btnReg = new JButton("Đăng ký");
        btnReg.setBounds(200, 190, 120, 35);

        btnLogin.addActionListener(e -> loginXuly());
        btnReg.addActionListener(e -> new RegisterForm());

        panel.add(title); panel.add(lbEmail); panel.add(txtEmail);
        panel.add(lbPass); panel.add(txtPass); panel.add(btnLogin); panel.add(btnReg);
        add(panel);
        setVisible(true);
    }

    private void loginXuly() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPass.getPassword()).trim();

        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đủ tài khoản mật khẩu!");
            return;
        }

        // Gửi lệnh đăng nhập lên Server
        ClientSocketManager.getInstance().sendRequest("LOGIN;" + email + ";" + pass);
        String response = ClientSocketManager.getInstance().receiveResponse();

        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
            String[] data = response.split(";");
            
            // Đồng bộ nạp dữ liệu Profile vừa đăng nhập vào Singleton Client
            ClientSocketManager.getInstance().setUsername(data[1]);
            ClientSocketManager.getInstance().setFullName(data[2]);
            ClientSocketManager.getInstance().setEmail(data[3]);
            ClientSocketManager.getInstance().setPhone(data[4]);

            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");
            
            // Chuyển hướng hiển thị thẳng vào trang cá nhân vừa đăng nhập
            new ProfileForm(data[1]);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new LoginForm();
    }
}