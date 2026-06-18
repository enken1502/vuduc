package view;

import java.awt.*;
import javax.swing.*;

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
            String msg = txtMessage.getText().trim();

            if (!msg.isEmpty()) {
                // 1. Gửi chuỗi lên Server qua Socket chung
                view.ClientSocketManager.getInstance().sendRequest("CHAT;" + msg);
                
                // 2. Hiển thị lên màn hình chat của mình
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

        // -----------------------------------------------------------------
        // LUỒNG NGHE TIN NHẮN REAL-TIME TỪ SERVER ĐẨY VỀ
        // -----------------------------------------------------------------
        new Thread(() -> {
            try {
                while (true) {
                    // Liên tục đứng đợi đọc tin nhắn từ Server gửi xuống
                    String response = view.ClientSocketManager.getInstance().receiveResponse();
                    
                    if (response != null) {
                        // Giả sử Server gửi tin nhắn tới theo cấu trúc: RECEIVE_MSG;<Người_gửi>;<Nội_dung>
                        String[] data = response.split(";");
                        
                        if (data[0].equals("RECEIVE_MSG")) {
                            String sender = data[1];
                            String content = data[2];
                            
                            // Đẩy tin nhắn vừa nhận lên khung hiển thị chatArea
                            chatArea.append(sender + ": " + content + "\n");
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Lỗi luồng nhận tin nhắn: " + ex.getMessage());
            }
        }).start(); // Kích hoạt Thread chạy ngầm song song với giao diện
    }
}