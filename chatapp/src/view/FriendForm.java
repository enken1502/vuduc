package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;

public class FriendForm extends JFrame {
    private JList<String> jlFriends;
    private DefaultListModel<String> listModel;
    private FriendDAO friendDAO = new FriendDAO();
    private String myUsername;

    public FriendForm(String myUsername) {
        this.myUsername = myUsername;

        // --- CẤU HÌNH GIAO DIỆN CHÍNH ---
        setTitle("Danh sách bạn bè - " + myUsername);
        setSize(380, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Danh sách hiển thị bạn bè chính thức
        listModel = new DefaultListModel<>();
        jlFriends = new JList<>(listModel);
        jlFriends.setFont(new Font("Arial", Font.PLAIN, 14));
        add(new JScrollPane(jlFriends), BorderLayout.CENTER);

        // --- THANH ĐIỀU KHIỂN NÚT BẤM PHÍA DƯỚI ---
        // Chia làm 2 cột bằng GridLayout để 2 nút nằm ngang hàng cân đối
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        JButton btnAddFriend = new JButton("Thêm bạn");
        JButton btnViewRequests = new JButton("Lời mời kết bạn"); // NÚT BẤM XEM DANH SÁCH CHỜ DUYỆT

        bottomPanel.add(btnAddFriend);
        bottomPanel.add(btnViewRequests);
        add(bottomPanel, BorderLayout.SOUTH);

        // 1. SỰ KIỆN NÚT THÊM BẠN (GỬI LỜI MỜI ĐI)
        btnAddFriend.addActionListener(e -> {
            String targetUser = JOptionPane.showInputDialog(this, "Nhập tên tài khoản muốn kết bạn:");
            if (targetUser == null || targetUser.trim().isEmpty()) return;
            
            targetUser = targetUser.trim();
            if (targetUser.equalsIgnoreCase(myUsername)) {
                JOptionPane.showMessageDialog(this, "Bạn không thể tự gửi kết bạn với chính mình!");
                return;
            }

            // Lưu trạng thái PENDING vào database máy mình trước
            boolean isInserted = friendDAO.insertFriendRequest(myUsername, targetUser);
            if (isInserted) {
                // Mượn đường gửi tín hiệu ẩn qua Server bằng lệnh CHAT gốc của nhóm ông
                ClientSocketManager.getInstance().sendRequest("CHAT;" + targetUser + ";__FRIEND_REQUEST__");
                JOptionPane.showMessageDialog(this, "Đã gửi lời mời kết bạn đến [" + targetUser + "]!");
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi! Tài khoản không tồn tại hoặc lời mời đã tồn tại.");
            }
        });

        // 2. SỰ KIỆN NÚT "LỜI MỜI KẾT BẠN" (MỞ NƠI NHẤN DUYỆT DANH SÁCH)
        btnViewRequests.addActionListener(e -> {
            // Nhấn vào đây sẽ bật giao diện FriendRequestForm lên, truyền 'this' để tự động nạp lại khi Accept
            new FriendRequestForm(myUsername, this);
        });

        // Tự động tải danh sách bạn bè chính thức lên JList khi vừa mở Form
        loadFriendList();
        
        setVisible(true);
    }

    /**
     * Hàm nạp dữ liệu bạn bè chính thức (ACCEPTED) từ DB lên giao diện
     */
    public void loadFriendList() {
        listModel.clear();
        List<String> friends = friendDAO.getAcceptedFriends(myUsername);
        if (friends != null) {
            for (String friend : friends) {
                listModel.addElement(friend);
            }
        }
    }
}