package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;

public class FriendRequestForm extends JFrame {
    private JList<String> requestList;
    private DefaultListModel<String> listModel;
    
    // Khai báo chuẩn xác đối tượng xử lý Database
    private view.FriendDAO friendDAO = new view.FriendDAO();
    
    private String myUsername;
    private FriendForm friendFormInstance; 

    public FriendRequestForm(String myUsername, FriendForm friendFormInstance) {
        this.myUsername = myUsername;
        this.friendFormInstance = friendFormInstance;

        // --- CẤU HÌNH GIAO DIỆN ---
        setTitle("Lời mời kết bạn đang chờ");
        setSize(400, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        requestList = new JList<>(listModel);
        requestList.setFont(new Font("Arial", Font.PLAIN, 14));
        
        add(new JScrollPane(requestList), BorderLayout.CENTER);

        // Thanh điều khiển nút bấm phía dưới
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton btnAccept = new JButton("Chấp nhận");
        JButton btnReject = new JButton("Từ chối");
        
        buttonPanel.add(btnAccept);
        buttonPanel.add(btnReject);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- XỬ LÝ SỰ KIỆN NÚT CHẤP NHẬN ---
        btnAccept.addActionListener(e -> {
            String selectedSender = requestList.getSelectedValue();
            if (selectedSender == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một người trong danh sách để duyệt!");
                return;
            }

            // 1. Cập nhật trạng thái ACCEPTED trong SQL máy mình
            boolean ok = friendDAO.acceptFriend(selectedSender, myUsername);
            if (ok) {
                // 2. Mượn đường Server bắn tin nhắn ẩn báo cho người kia biết để họ reload JList
                ClientSocketManager.getInstance().sendRequest("CHAT;" + selectedSender + ";__FRIEND_ACCEPT__");

                JOptionPane.showMessageDialog(this, "Chúc mừng! Hai bạn đã trở thành bạn bè.");
                
                // 3. Làm mới lại danh sách chờ duyệt hiện tại
                loadRequests();
                
                // 4. Nếu Form danh sách bạn bè chính thức đang mở, ép nó reload luôn dữ liệu mới
                if (friendFormInstance != null && friendFormInstance.isShowing()) {
                    friendFormInstance.loadFriendList();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi xử lý Database cập nhật bạn bè!");
            }
        });

        // --- XỬ LÝ SỰ KIỆN NÚT TỪ CHỐI ---
        btnReject.addActionListener(e -> {
            String selectedSender = requestList.getSelectedValue();
            if (selectedSender == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một người trong danh sách!");
                return;
            }

            // Xóa lời mời kết bạn (PENDING) khỏi DB
            boolean ok = friendDAO.rejectOrDeleteFriend(selectedSender, myUsername);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã từ chối lời mời kết bạn của [" + selectedSender + "]");
                loadRequests(); // Nạp lại danh sách
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa lời mời kết bạn khỏi DB!");
            }
        });

        // Tải danh sách dữ liệu thực tế lên màn hình
        loadRequests();

        setVisible(true);
    }

    /**
     * Hàm lấy dữ liệu PENDING từ Database để nạp trực tiếp vào JList
     */
    public void loadRequests() {
        // Đảm bảo xóa sạch dữ liệu cũ hiển thị sai lệch trên giao diện trước khi nạp
        listModel.clear();
        
        try {
            // Gọi hàm từ FriendDAO chuẩn
            List<String> pendingUsers = friendDAO.getPendingRequests(myUsername);
            
            if (pendingUsers != null && !pendingUsers.isEmpty()) {
                for (String user : pendingUsers) {
                    listModel.addElement(user); // Thêm từng người gửi vào danh sách hiển thị
                }
                setTitle("Lời mời kết bạn (" + pendingUsers.size() + " đang chờ)");
            } else {
                setTitle("Lời mời kết bạn (Trống)");
            }
        } catch (Exception ex) {
            System.err.println("Lỗi nạp danh sách chờ kết bạn: " + ex.getMessage());
        }
    }
}