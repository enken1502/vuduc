package view;

import javax.swing.*;

public class RegisterForm extends JFrame {
    private JTextField txtName, txtEmail, txtOTP;
    private JPasswordField txtPass; // Thêm ô nhập mật khẩu
    private JButton btnOTP, btnRegister;

    public RegisterForm() {
        setTitle("Đăng ký tài khoản");
        setSize(450, 450); // Tăng chiều cao để vừa ô mật khẩu
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        // 1. Ô Nhập Họ tên
        JLabel lbName = new JLabel("Họ tên:");
        lbName.setBounds(30, 30, 100, 30);
        txtName = new JTextField();
        txtName.setBounds(150, 30, 220, 30);

        // 2. Ô Nhập Email
        JLabel lbEmail = new JLabel("Email:");
        lbEmail.setBounds(30, 80, 100, 30);
        txtEmail = new JTextField();
        txtEmail.setBounds(150, 80, 220, 30);

        // 3. Ô Nhập Mật khẩu (Mới thêm)
        JLabel lbPass = new JLabel("Mật khẩu:");
        lbPass.setBounds(30, 130, 100, 30);
        txtPass = new JPasswordField();
        txtPass.setBounds(150, 130, 220, 30);

        // 4. Ô Nhập OTP
        JLabel lbOTP = new JLabel("Mã OTP:");
        lbOTP.setBounds(30, 180, 100, 30);
        txtOTP = new JTextField();
        txtOTP.setBounds(150, 180, 220, 30);

        // 5. Nút Gửi OTP
        btnOTP = new JButton("Gửi OTP");
        btnOTP.setBounds(150, 240, 100, 30);

        // 6. Nút Đăng ký
        btnRegister = new JButton("Đăng ký");
        btnRegister.setBounds(270, 240, 100, 30);

        // Thêm các thành phần vào giao diện
        panel.add(lbName); panel.add(txtName);
        panel.add(lbEmail); panel.add(txtEmail);
        panel.add(lbPass);  panel.add(txtPass);
        panel.add(lbOTP);   panel.add(txtOTP);
        panel.add(btnOTP);  panel.add(btnRegister);
        add(panel);

        // --- XỬ LÝ SỰ KIỆN NÚT BẤM VỚI SERVER ---

        // Hành động 1: Bấm nút Gửi OTP
        btnOTP.addActionListener(e -> {
            String email = txtEmail.getText().trim();
            String gmailRegex = "^[A-Za-z0-9._%+-]+@gmail\\.com$";

            if (!email.matches(gmailRegex)) {
                JOptionPane.showMessageDialog(this, "Email không đúng định dạng Gmail!\nVí dụ: abc@gmail.com", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Gửi lệnh yêu cầu Server tạo và gửi OTP: SEND_OTP;<email>
            view.ClientSocketManager.getInstance().sendRequest("SEND_OTP;" + email);
            JOptionPane.showMessageDialog(this, "Đang gửi mã OTP đến Gmail của bạn, vui lòng kiểm tra!");
        });

        // Hành động 2: Bấm nút Đăng ký
        btnRegister.addActionListener(e -> {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPass.getPassword()).trim();
            String otp = txtOTP.getText().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || otp.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tất cả các trường thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Gửi toàn bộ thông tin lên Server xác thực OTP và nạp vào DB
            // Cú pháp: REGISTER;<name>;<email>;<password>;<otp>
            view.ClientSocketManager.getInstance().sendRequest("REGISTER;" + name + ";" + email + ";" + password + ";" + otp);

            // Nhận kết quả từ Server trả về
            String response = view.ClientSocketManager.getInstance().receiveResponse();
            if ("REGISTER_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công tài khoản!");
                new LoginForm();
                dispose();
            } else if ("INVALID_OTP".equals(response)) {
                JOptionPane.showMessageDialog(this, "Mã OTP nhập vào không chính xác hoặc đã hết hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Đăng ký thất bại! Email có thể đã được sử dụng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        setVisible(true);
    }
}