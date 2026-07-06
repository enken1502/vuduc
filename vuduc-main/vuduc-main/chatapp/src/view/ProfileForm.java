package view;

import java.awt.Font;
import javax.swing.*;

public class ProfileForm extends JFrame {
    private JTextField txtFullName, txtEmail, txtPhone;
    private JLabel lbUsername;
    private String username;

    public ProfileForm(String username) {
        this.username = username;

        setTitle("Thông tin cá nhân");
        setSize(450, 430);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(null);

        JLabel title = new JLabel("THÔNG TIN CÁ NHÂN");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBounds(100, 10, 250, 30);

        JLabel avatar = new JLabel("👤", SwingConstants.CENTER);
        avatar.setFont(new Font("Arial", Font.PLAIN, 40));
        avatar.setBounds(170, 45, 80, 60);

        JLabel lbUser = new JLabel("Tên đăng nhập:");
        lbUser.setBounds(30, 120, 120, 30);
        lbUsername = new JLabel(username);
        lbUsername.setBounds(170, 120, 200, 30);

        JLabel lbName = new JLabel("Họ tên:");
        lbName.setBounds(30, 160, 120, 30);
        txtFullName = new JTextField(ClientSocketManager.getInstance().getFullName());
        txtFullName.setBounds(170, 160, 220, 30);

        JLabel lbEmail = new JLabel("Email:");
        lbEmail.setBounds(30, 200, 120, 30);
        txtEmail = new JTextField(ClientSocketManager.getInstance().getEmail());
        txtEmail.setBounds(170, 200, 220, 30);
        txtEmail.setEditable(false); // Email dùng làm ID định danh không được sửa

        JLabel lbPhone = new JLabel("SĐT:");
        lbPhone.setBounds(30, 240, 120, 30);
        txtPhone = new JTextField(ClientSocketManager.getInstance().getPhone());
        txtPhone.setBounds(170, 240, 220, 30);

        JButton btnUpdate = new JButton("Cập nhật");
        btnUpdate.setBounds(30, 320, 110, 35);

        JButton btnChat = new JButton("Vào Chat");
        btnChat.setBounds(160, 320, 110, 35);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setBounds(290, 320, 110, 35);

        // --- XỬ LÝ SỰ KIỆN ---
        btnUpdate.addActionListener(e -> {
            String newName = txtFullName.getText().trim();
            String newPhone = txtPhone.getText().trim();

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Họ tên không được bỏ trống!");
                return;
            }

            ClientSocketManager.getInstance().sendRequest("UPDATE_PROFILE;" + username + ";" + newName + ";" + txtEmail.getText() + ";" + newPhone);
            ClientSocketManager.getInstance().setFullName(newName);
            ClientSocketManager.getInstance().setPhone(newPhone);
            JOptionPane.showMessageDialog(this, "Cập nhật thành công thông tin!");
        });

        btnChat.addActionListener(e -> {
            new MainChatForm(); // Mở duy nhất một màn hình chính
            dispose();
        });

        btnLogout.addActionListener(e -> {
            new LoginForm();
            dispose();
        });

        panel.add(title); panel.add(avatar); panel.add(lbUser); panel.add(lbUsername);
        panel.add(lbName); panel.add(txtFullName); panel.add(lbEmail); panel.add(txtEmail);
        panel.add(lbPhone); panel.add(txtPhone); panel.add(btnUpdate); panel.add(btnChat); panel.add(btnLogout);
        add(panel);
        setVisible(true);
    }
}