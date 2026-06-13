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

        btnLogin.addActionListener(e -> {
            new MainChatForm();
            dispose();
        });

        JButton btnRegister = new JButton("Đăng ký");
        btnRegister.setBounds(200, 170, 120, 35);

        btnRegister.addActionListener(e -> {
            new RegisterForm();
            dispose();
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