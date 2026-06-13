package view;

import javax.swing.*;
import java.awt.*;

public class MainChatForm extends JFrame {

    public MainChatForm() {

        setTitle("Chat App");
        setSize(1000,600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Danh sách bạn bè
        String[] friends = {
                "Nguyễn Văn A (Online)",
                "Trần Văn B (Offline)",
                "Nhóm Java",
                "Nhóm CNTT"
        };

        JList<String> friendList = new JList<>(friends);

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

        btnSend.addActionListener(e -> {
            String msg = txtMessage.getText();

            if(!msg.isEmpty()) {
                chatArea.append("Tôi: " + msg + "\n");
                txtMessage.setText("");
            }
        });
        JMenuBar menuBar = new JMenuBar();

JMenu menu = new JMenu("Tài khoản");

JMenuItem profileItem = new JMenuItem("Thông tin cá nhân");
JMenuItem friendItem = new JMenuItem("Bạn bè");
JMenuItem logoutItem = new JMenuItem("Đăng xuất");

menu.add(profileItem);
menu.add(friendItem);
menu.addSeparator();
menu.add(logoutItem);

menuBar.add(menu);

setJMenuBar(menuBar);

        setVisible(true);
    }
}