package view;

import javax.swing.*;

public class FriendForm extends JFrame {

    public FriendForm() {

        setTitle("Danh sách bạn bè");
        setSize(400,500);
        setLocationRelativeTo(null);

        String[] friends = {
            "Nguyễn Văn A",
            "Trần Văn B",
            "Lê Văn C"
        };

        JList<String> list = new JList<>(friends);

        add(new JScrollPane(list));

        setVisible(true);
        JButton btnAddFriend = new JButton("Thêm bạn");

btnAddFriend.addActionListener(e -> {
    String name = JOptionPane.showInputDialog(
            "Nhập tên người dùng:");

    if(name != null && !name.trim().isEmpty()) {
        JOptionPane.showMessageDialog(
                null,
                "Đã gửi lời mời kết bạn tới: " + name);
    }
});
    }
}