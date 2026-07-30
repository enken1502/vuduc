package view;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class JoinGroupDialog extends JDialog {
    private JTextField txtSearch;
    private JButton btnSearch, btnJoin;
    private JList<String> listResults;
    private DefaultListModel<String> listModel;

    public JoinGroupDialog(Window parent) {
        super(parent, "Tìm kiếm & Tham gia nhóm", ModalityType.APPLICATION_MODAL);
        setSize(400, 320);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(8, 8));

        // --- Panel Tìm kiếm ở trên ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        txtSearch = new JTextField();
        btnSearch = new JButton("Tìm kiếm");

        topPanel.add(new JLabel("Tên/ID Nhóm: "), BorderLayout.WEST);
        topPanel.add(txtSearch, BorderLayout.CENTER);
        topPanel.add(btnSearch, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- Danh sách kết quả ở giữa ---
        listModel = new DefaultListModel<>();
        listResults = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(listResults);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Kết quả tìm kiếm"));
        add(scrollPane, BorderLayout.CENTER);

        // --- Nút Tham gia ở dưới ---
        btnJoin = new JButton("Tham gia nhóm");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnJoin);
        add(bottomPanel, BorderLayout.SOUTH);

        // ================= ACTION LISTENERS =================

        // 1. NÚT TÌM KIẾM (Đọc từ DB giống hệt AddFriend)
        btnSearch.addActionListener(e -> {
            String keyword = txtSearch.getText().trim();
            if (keyword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập Tên hoặc Mã ID nhóm!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            listModel.clear();
            List<String> groups = searchGroupsFromDB(keyword);

            if (groups.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy nhóm nào khớp với từ khóa!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                for (String g : groups) {
                    listModel.addElement(g);
                }
            }
        });

        // 2. NÚT THAM GIA NHÓM
        btnJoin.addActionListener(e -> {
            String selectedValue = listResults.getSelectedValue();
            if (selectedValue == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhóm từ danh sách!");
                return;
            }

            try {
                // Tách ID từ chuỗi "Tên Nhóm (ID: 10)"
                int idStart = selectedValue.lastIndexOf("(ID: ") + 5;
                int idEnd = selectedValue.lastIndexOf(")");
                int groupId = Integer.parseInt(selectedValue.substring(idStart, idEnd));

                // Gửi request lên Server (Chỉ gửi 1 chiều, không gọi receiveResponse gây đơ UI)
                ClientSocketManager.getInstance().sendRequest("JOIN_GROUP;" + groupId);

                JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu tham gia nhóm!");

                // Reload lại danh sách nhóm ở màn hình chính MainChatForm
                Window owner = getOwner();
                if (owner instanceof MainChatForm) {
                    ((MainChatForm) owner).loadGroupList();
                }

                dispose(); // Đóng Dialog
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xử lý dữ liệu nhóm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Hàm truy vấn danh sách nhóm trực tiếp từ Database
    private List<String> searchGroupsFromDB(String keyword) {
        List<String> result = new ArrayList<>();
        // Thay thông số DB_URL, DB_USER, DB_PASS cho đúng cấu hình của ông
        String url = "jdbc:sqlserver://localhost:1433;databaseName=ChatDB;encrypt=false;trustServerCertificate=true;";
        String user = "sa";
        String pass = "12345678"; // Đổi mật khẩu DB của ông ở đây

        String sql = "SELECT GroupID, GroupName FROM GroupChats WHERE GroupName LIKE ? OR CAST(GroupID AS VARCHAR) = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, keyword);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("GroupName") + " (ID: " + rs.getInt("GroupID") + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}