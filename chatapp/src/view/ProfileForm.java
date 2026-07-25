package view;

import java.awt.*;
import javax.swing.*;

public class ProfileForm extends JFrame {
    private JTextField txtUserId, txtUsername, txtEmail, txtStatus;
    private JButton btnEdit;
    private UserDAO userDAO = new UserDAO();
    private User currentUser;

    public ProfileForm(String username) {
        setTitle("Thông tin cá nhân - " + username);
        setSize(380, 280);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 8, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUserId = createReadOnlyField();
        txtUsername = createReadOnlyField();
        txtEmail = createReadOnlyField();
        txtStatus = createReadOnlyField();

        formPanel.add(new JLabel("Mã tài khoản (UserID):"));
        formPanel.add(txtUserId);

        formPanel.add(new JLabel("Tên đăng nhập:"));
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Email liên hệ:"));
        formPanel.add(txtEmail);

        formPanel.add(new JLabel("Trạng thái:"));
        formPanel.add(txtStatus);

        add(formPanel, BorderLayout.CENTER);

        // Nút Thay đổi thông tin bên dưới
        btnEdit = new JButton("Thay đổi thông tin");
        btnEdit.setPreferredSize(new Dimension(160, 35));
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnEdit);
        add(btnPanel, BorderLayout.SOUTH);

        // Nạp dữ liệu
        loadData(username);

        // Sự kiện bấm nút Thay đổi thông tin
        btnEdit.addActionListener(e -> {
    if (currentUser != null) {
        // 1. Khởi tạo EditProfileDialog
        EditProfileDialog dialog = new EditProfileDialog(ProfileForm.this, currentUser);
        
        // 🌟 DÒNG QUAN TRỌNG NHẤT: Bắt buộc phải có setVisible(true) thì Dialog mới hiện lên màn hình!
        dialog.setVisible(true); 
    } else {
        JOptionPane.showMessageDialog(ProfileForm.this, "Không có dữ liệu người dùng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
});

        setVisible(true);
    }

    private JTextField createReadOnlyField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setBackground(new Color(240, 240, 240));
        return tf;
    }

    private void loadData(String username) {
        currentUser = userDAO.getUserProfile(username);
        if (currentUser != null) {
            txtUserId.setText(String.valueOf(currentUser.getUserId()));
            txtUsername.setText(currentUser.getUsername());
            txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            txtStatus.setText(currentUser.getStatus() != null ? currentUser.getStatus() : "Online");
        }
    }
}