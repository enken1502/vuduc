package view;

import javax.swing.*;
import java.awt.*;

public class MainChatForm extends JFrame {

    public MainChatForm() {

        setTitle("Chat App");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Danh sách bạn bè
        String[] friendsData = {
                "Nguyễn Văn A (Online)",
                "Trần Văn B (Offline)",
                "Nhóm Java",
                "Nhóm CNTT"
        };

        JList<String> friendList = new JList<>(friendsData);

        // Khu vực chat
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);

        // Ô nhập tin nhắn
        JTextField txtMessage = new JTextField();

        JButton btnSend = new JButton("Gửi");
        JButton btnFile = new JButton("File");
        JButton btnSticker = new JButton("Sticker");

        JPanel bottomPanel = new JPanel(new BorderLayout());

        bottomPanel.add(txtMessage, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(btnFile);
        buttonPanel.add(btnSticker);
        buttonPanel.add(btnSend);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(new JScrollPane(friendList), BorderLayout.WEST);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Gửi tin nhắn
        btnSend.addActionListener(e -> {
            String msg = txtMessage.getText();

            if (!msg.isEmpty()) {
                chatArea.append("Tôi: " + msg + "\n");
                txtMessage.setText("");
            }
        });

        // MENU
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Menu");

        JMenuItem profile = new JMenuItem("Thông tin cá nhân");
        JMenuItem friends = new JMenuItem("Bạn bè");
        JMenuItem createGroup = new JMenuItem("Tạo nhóm");
        JMenuItem joinGroup = new JMenuItem("Tham gia nhóm");
        JMenuItem logout = new JMenuItem("Đăng xuất");

        profile.addActionListener(e -> new ProfileForm());

        friends.addActionListener(e -> new FriendForm());

        createGroup.addActionListener(e -> new CreateGroupForm());

        joinGroup.addActionListener(e -> new JoinGroupForm());

        logout.addActionListener(e -> {
            new LoginForm();
            dispose();
        });

        menu.add(profile);
        menu.add(friends);
        menu.add(createGroup);
        menu.add(joinGroup);
        menu.addSeparator();
        menu.add(logout);

        menuBar.add(menu);

        setJMenuBar(menuBar);

        setVisible(true);
    }
}