package view;

import javax.swing.*;

public class RegisterForm extends JFrame {

    public RegisterForm() {

        setTitle("Đăng ký");
        setSize(450,400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lbName = new JLabel("Họ tên:");
        lbName.setBounds(30,30,100,30);

        JTextField txtName = new JTextField();
        txtName.setBounds(150,30,220,30);

        JLabel lbEmail = new JLabel("Email:");
        lbEmail.setBounds(30,80,100,30);

        JTextField txtEmail = new JTextField();
        txtEmail.setBounds(150,80,220,30);

        JLabel lbOTP = new JLabel("OTP:");
        lbOTP.setBounds(30,130,100,30);

        JTextField txtOTP = new JTextField();
        txtOTP.setBounds(150,130,220,30);

        JButton btnOTP = new JButton("Gửi OTP");
        btnOTP.setBounds(150,180,100,30);

       JButton btnRegister = new JButton("Đăng ký");
btnRegister.setBounds(270,180,100,30);

btnRegister.addActionListener(e -> {

    String email = txtEmail.getText().trim();

    // Kiểm tra định dạng Gmail
    String gmailRegex = "^[A-Za-z0-9._%+-]+@gmail\\.com$";

    if (!email.matches(gmailRegex)) {
        JOptionPane.showMessageDialog(
                this,
                "Email không đúng định dạng Gmail!\nVí dụ: abc@gmail.com",
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        return;
    }

    JOptionPane.showMessageDialog(
            this,
            "Đăng ký thành công!",
            "Thông báo",
            JOptionPane.INFORMATION_MESSAGE);
});

        panel.add(lbName);
        panel.add(txtName);
        panel.add(lbEmail);
        panel.add(txtEmail);
        panel.add(lbOTP);
        panel.add(txtOTP);
        panel.add(btnOTP);
        panel.add(btnRegister);

        add(panel);

        setVisible(true);
    }
}