package view;

import java.awt.*;
import javax.swing.*;

public class MainChatForm extends JFrame {
    private JTextArea chatArea;

    public MainChatForm() {
        setTitle("Chat App");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        String[] friendsData = {"Nguyễn Văn A (Online)", "Trần Văn B (Offline)", "Nhóm Java", "Nhóm CNTT"};
        JList<String> friendList = new JList<>(friendsData);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JTextField txtMessage = new JTextField();
        JButton btnSend = new JButton("Gửi");
        JButton btnFile = new JButton("File");
        JButton btnSticker = new JButton("Sticker");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(txtMessage, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnFile); buttonPanel.add(btnSticker); buttonPanel.add(btnSend);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(new JScrollPane(friendList), BorderLayout.WEST);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Hành động gửi tin nhắn chat
        btnSend.addActionListener(e -> {
            String msg = txtMessage.getText().trim();
            if (!msg.isEmpty()) {
                ClientSocketManager.getInstance().sendRequest("CHAT;" + msg);
                chatArea.append("Tôi: " + msg + "\n");
                txtMessage.setText("");
            }
        });

        // MENU SYSTEM
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");

        JMenuItem profile = new JMenuItem("Thông tin cá nhân");
        JMenuItem friends = new JMenuItem("Bạn bè");
        JMenuItem createGroup = new JMenuItem("Tạo nhóm");
        JMenuItem joinGroup = new JMenuItem("Tham gia nhóm");
        JMenuItem logout = new JMenuItem("Đăng xuất");

        profile.addActionListener(e -> {
            new ProfileForm(ClientSocketManager.getInstance().getUsername());
            dispose();
        });
        friends.addActionListener(e -> new FriendForm());
        createGroup.addActionListener(e -> new CreateGroupForm());
        joinGroup.addActionListener(e -> new JoinGroupForm());
        logout.addActionListener(e -> {
            new LoginForm();
            dispose();
        });

        menu.add(profile); menu.add(friends); menu.add(createGroup); menu.add(joinGroup);
        menu.addSeparator(); menu.add(logout);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        setVisible(true);

        // Kích hoạt luồng chạy ngầm nhận tin nhắn Real-time từ Server
        startListening();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    String response = ClientSocketManager.getInstance().receiveResponse();
                    if (response != null) {
                        String[] data = response.split(";");
                        if (data[0].equals("RECEIVE_MSG")) {
                            String sender = data[1];
                            String content = data[2];
                            chatArea.append(sender + ": " + content + "\n");
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Mất kết nối luồng nhận tin chat.");
            }
        }).start();
    }
}