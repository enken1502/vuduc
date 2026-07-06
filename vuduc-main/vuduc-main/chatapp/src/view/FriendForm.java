package view;

import javax.swing.*;

public class FriendForm extends JFrame {
    public FriendForm() {
        setTitle("Danh sách bạn bè");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(null);

        String[] friends = {"Nguyễn Văn A", "Trần Văn B", "Lê Văn C"};
        JList<String> list = new JList<>(friends);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBounds(20, 20, 340, 360);

        JButton btnAddFriend = new JButton("Thêm bạn");
        btnAddFriend.setBounds(130, 400, 120, 35);

        btnAddFriend.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Nhập tên người dùng:");
            if (name != null && !name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Đã gửi lời mời kết bạn tới: " + name);
            }
        });

        panel.add(scroll);
        panel.add(btnAddFriend);
        add(panel);
        setVisible(true);
    }
}