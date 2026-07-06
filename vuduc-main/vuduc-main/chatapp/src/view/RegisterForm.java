package view;

import javax.swing.*;

public class RegisterForm extends JFrame {
    private JTextField txtName, txtEmail, txtOTP;
    private JPasswordField txtPass;
    private JButton btnOTP, btnRegister;

    public RegisterForm() {
        setTitle("Đăng ký tài khoản");
        setSize(450, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(null);

        JLabel lbName = new JLabel("Họ tên:");
        lbName.setBounds(30, 30, 100, 30);
        txtName = new JTextField();
        txtName.setBounds(150, 30, 220, 30);

        JLabel lbEmail = new JLabel("Email:");
        lbEmail.setBounds(30, 80, 100, 30);
        txtEmail = new JTextField();
        txtEmail.setBounds(150, 80, 220, 30);

        JLabel lbPass = new JLabel("Mật khẩu:");
        lbPass.setBounds(30, 130, 100, 30);
        txtPass = new JPasswordField();
        txtPass.setBounds(150, 130, 220, 30);

        JLabel lbOTP = new JLabel("Mã OTP:");
        lbOTP.setBounds(30, 180, 100, 30);
        txtOTP = new JTextField();
        txtOTP.setBounds(150, 180, 220, 30);

        btnOTP = new JButton("Gửi OTP");
        btnOTP.setBounds(150, 240, 100, 30);

        btnRegister = new JButton("Đăng ký");
        btnRegister.setBounds(270, 240, 100, 30);

        btnOTP.addActionListener(e -> {
            String email = txtEmail.getText().trim();
            if (!email.matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
                JOptionPane.showMessageDialog(this, "Yêu cầu nhập đúng cấu trúc Gmail!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ClientSocketManager.getInstance().sendRequest("SEND_OTP;" + email);
            JOptionPane.showMessageDialog(this, "Yêu cầu tạo mã đã gửi, vui lòng kiểm tra Gmail.");
        });

        btnRegister.addActionListener(e -> {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPass.getPassword()).trim();
            String otp = txtOTP.getText().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || otp.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ các thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ClientSocketManager.getInstance().sendRequest("REGISTER;" + name + ";" + email + ";" + password + ";" + otp);
            String response = ClientSocketManager.getInstance().receiveResponse();

            if ("REGISTER_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công tài khoản!");
                dispose();
            } else if ("INVALID_OTP".equals(response)) {
                JOptionPane.showMessageDialog(this, "Mã OTP không chính xác!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Tài khoản Email này đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(lbName); panel.add(txtName); panel.add(lbEmail); panel.add(txtEmail);
        panel.add(lbPass); panel.add(txtPass); panel.add(lbOTP); panel.add(txtOTP);
        panel.add(btnOTP); panel.add(btnRegister);
        add(panel);
        setVisible(true);
    }
}