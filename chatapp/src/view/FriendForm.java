package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;

public class FriendForm extends JFrame {
    private JList<String> jlFriends;
    private DefaultListModel<String> listModel;
    private FriendDAO friendDAO = new FriendDAO();
    private String myUsername;
    private MainChatForm mainChatForm;

    public FriendForm(String myUsername, MainChatForm mainChatForm) {
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
        JButton btnDeleteFriend = new JButton("Xóa kết bạn"); // Nút mới

        bottomPanel.add(btnAddFriend);
        bottomPanel.add(btnViewRequests);
        bottomPanel.add(btnDeleteFriend); // Thêm nút xóa vào panel
        add(bottomPanel, BorderLayout.SOUTH);

        // 1. SỰ KIỆN NÚT THÊM BẠN (GỬI LỜI MỜI ĐI)
        btnAddFriend.addActionListener(e -> {
            String keyword = JOptionPane.showInputDialog(this, "Nhập tên tài khoản muốn tìm kiếm:");
            if (keyword == null || keyword.trim().isEmpty()) return;
            
            keyword = keyword.trim();
            
            // Gọi hàm từ DAO để lấy danh sách tất cả người dùng trùng tên kèm ID
            List<String> foundUsers = friendDAO.searchUsers(keyword);
            
            if (foundUsers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy người dùng nào khớp với từ khóa!");
                return;
            }
            
            // Chuyển List kết quả thành mảng String để hiển thị lên Dropdown (JComboBox)
            String[] options = foundUsers.toArray(new String[0]);
            
            // Hiển thị hộp thoại Dropdown cho người dùng chọn chính xác tài khoản cần kết bạn
            String selectedOption = (String) JOptionPane.showInputDialog(
                    this,
                    "Tìm thấy các tài khoản sau, vui lòng chọn người muốn kết bạn:",
                    "Kết quả tìm kiếm",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            
            // Nếu người dùng chọn một tài khoản và nhấn OK
            if (selectedOption != null) {
                // Tách lấy chính xác Username đứng trước chuỗi " (ID:" để gửi lệnh kết bạn
                String targetUser = selectedOption.split(" \\(ID:")[0].trim();
                
                if (targetUser.equalsIgnoreCase(myUsername)) {
                    JOptionPane.showMessageDialog(this, "Bạn không thể tự gửi kết bạn với chính mình!");
                    return;
                }

                // Lưu trạng thái PENDING vào database máy mình trước
                boolean isInserted = friendDAO.insertFriendRequest(myUsername, targetUser);
                if (isInserted) {
                    // Gửi tín hiệu mạng đi
                ClientSocketManager.getInstance().sendRequest("CHAT;" + targetUser + ";__FRIEND_REQUEST__");                    JOptionPane.showMessageDialog(this, "Đã gửi lời mời kết bạn đến [" + selectedOption + "]!");
                } else {
                    JOptionPane.showMessageDialog(this, "Lỗi! Bạn đã gửi lời mời hoặc đã là bạn bè với tài khoản này.");
                }
            }
        });
        btnDeleteFriend.addActionListener(e -> {
            String selectedFriend = jlFriends.getSelectedValue();
            if (selectedFriend == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một người bạn từ danh sách để hủy kết bạn!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Bạn có chắc chắn muốn hủy kết bạn với [" + selectedFriend + "] không?", 
                    "Xác nhận hủy kết bạn", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                if (friendDAO.rejectOrDeleteFriend(myUsername, selectedFriend)) {
                    ClientSocketManager.getInstance().sendRequest("CHAT;" + selectedFriend + ";__FRIEND_DELETE__");
                    JOptionPane.showMessageDialog(this, "Đã hủy kết bạn thành công!");
                    loadFriendList(); 
                } else {
                    JOptionPane.showMessageDialog(this, "Hủy kết bạn thất bại! Vui lòng thử lại.");
                }
            }
            if (mainChatForm != null) {
            mainChatForm.loadFriendAndGroupData(); // Hoặc tên hàm nạp danh sách bạn bè của MainChatForm
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