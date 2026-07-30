package view;

import java.awt.*;
import javax.swing.*;

public class EditProfileDialog extends JDialog {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnSave;

    public EditProfileDialog(Window parent, User user) {
        super(parent, "Thay đổi thông tin tài khoản", ModalityType.APPLICATION_MODAL);
        setSize(360, 230);
        setLocationRelativeTo(parent); // Hiển thị giữa màn hình cha
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 12));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsername = new JTextField(user.getUsername());
        txtPassword = new JPasswordField();

        formPanel.add(new JLabel("Tên đăng nhập mới:"));
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Mật khẩu mới:"));
        formPanel.add(txtPassword);

        add(formPanel, BorderLayout.CENTER);

        btnSave = new JButton("Xác nhận thay đổi");
        btnSave.setPreferredSize(new Dimension(150, 32));
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);

        // Sự kiện khi bấm Lưu
        btnSave.addActionListener(e -> {
            String newUsername = txtUsername.getText().trim();
            String newPassword = new String(txtPassword.getPassword()).trim();

            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Gửi gói tin lên MainServer
            String passParam = newPassword.isEmpty() ? "KEEP_OLD_PASS" : newPassword;
            String packet = "UPDATE_PROFILE;" + user.getUserId() + ";" + newUsername + ";" + passParam;
            
            // Gửi qua ClientSocketManager
            ClientSocketManager.getInstance().sendRequest(packet);

            JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu đổi thông tin đến Server!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
    }
}