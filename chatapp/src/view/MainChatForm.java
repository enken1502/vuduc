package view;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class MainChatForm extends JFrame {
    private JList<String> jlFriendsSidebar;
    private DefaultListModel<String> friendModel;
    
    private JList<String> jlGroupsSidebar;
    private DefaultListModel<String> groupModel;
    
    private JTextArea chatArea;
    private JTextField txtMessage;
    private JButton btnSend;
    
    private String myUsername;
    private String currentTarget = ""; 
    private boolean isGroupChat = false; 
    
    private FriendDAO friendDAO = new FriendDAO();

    public MainChatForm(String myUsername) {
        this.myUsername = myUsername;

        setTitle("Chat App - Tài khoản: " + myUsername);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu bar chức năng
        JMenuBar menuBar = new JMenuBar();
        JMenu menuChucNang = new JMenu("Chức năng");
        JMenuItem itemFriend = new JMenuItem("Quản lý kết bạn");
        JMenuItem itemCreateGroup = new JMenuItem("Tạo nhóm chat mới");

        menuChucNang.add(itemFriend);
        menuChucNang.add(itemCreateGroup);
        menuBar.add(menuChucNang);
        setJMenuBar(menuBar);

        itemFriend.addActionListener(e -> new FriendForm(myUsername));
        itemCreateGroup.addActionListener(e -> performCreateGroup());

        // SIDEBAR BÊN TRÁI (ZALO STYLE)
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        leftPanel.setPreferredSize(new Dimension(240, 0));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Nửa trên: Bạn bè
        JPanel friendPanel = new JPanel(new BorderLayout());
        friendPanel.setBorder(BorderFactory.createTitledBorder("Bạn bè thân thiết"));
        friendModel = new DefaultListModel<>();
        jlFriendsSidebar = new JList<>(friendModel);
        jlFriendsSidebar.setFont(new Font("Arial", Font.PLAIN, 14));
        friendPanel.add(new JScrollPane(jlFriendsSidebar), BorderLayout.CENTER);
        leftPanel.add(friendPanel);

        // Nửa dưới: Nhóm
        JPanel groupPanel = new JPanel(new BorderLayout());
        groupPanel.setBorder(BorderFactory.createTitledBorder("Nhóm trò chuyện"));
        groupModel = new DefaultListModel<>();
        jlGroupsSidebar = new JList<>(groupModel);
        jlGroupsSidebar.setFont(new Font("Arial", Font.PLAIN, 14));
        groupPanel.add(new JScrollPane(jlGroupsSidebar), BorderLayout.CENTER);
        leftPanel.add(groupPanel);

        add(leftPanel, BorderLayout.WEST);

        // KHUNG CHAT BÊN PHẢI
        JPanel rightPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        txtMessage = new JTextField();
        txtMessage.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSend = new JButton("Gửi");
        
        inputPanel.add(txtMessage, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);

        // =========================================================================
        // SỰ KIỆN CLICK CHỌN ĐỂ TỰ ĐỘNG TẢI LỊCH SỬ TIN NHẮN TỪ DB LÊN
        // =========================================================================
        
       // Khi chọn chat cá nhân với một người bạn
// Khi chọn chat cá nhân với một người bạn
jlFriendsSidebar.addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting() && jlFriendsSidebar.getSelectedValue() != null) {
        jlGroupsSidebar.clearSelection(); 
        currentTarget = jlFriendsSidebar.getSelectedValue();
        isGroupChat = false;
        
        chatArea.setText("--- Đang chat cá nhân với: " + currentTarget + " ---\n");
        
        List<String[]> history = friendDAO.getChatHistory(myUsername, currentTarget);
        for (String[] msg : history) {
            String sender = msg[0];
            String content = msg[1];
            
            // THÊM ĐIỀU KIỆN LỌC Ở ĐÂY:
            if (!content.startsWith("__FRIEND_")) {
                chatArea.append(sender + ": " + content + "\n");
            }
        }
    }
});

// Khi chọn chat trong một Nhóm
jlGroupsSidebar.addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting() && jlGroupsSidebar.getSelectedValue() != null) {
        jlFriendsSidebar.clearSelection(); 
        currentTarget = jlGroupsSidebar.getSelectedValue();
        isGroupChat = true;
        
        chatArea.setText("--- Đang chat trong nhóm: " + currentTarget + " ---\n");
        
        List<String[]> history = friendDAO.getGroupChatHistory(currentTarget);
        for (String[] msg : history) {
            String sender = msg[0];
            String content = msg[1];
            
            // THÊM ĐIỀU KIỆN LỌC Ở ĐÂY:
            if (!content.startsWith("__FRIEND_")) {
                chatArea.append(sender + ": " + content + "\n");
            }
        }
    }
});

        btnSend.addActionListener(e -> performSendMessage());
        txtMessage.addActionListener(e -> performSendMessage());

        loadFriendAndGroupData();
        activateListening();

        setVisible(true);
    }

    public void loadFriendAndGroupData() {
        friendModel.clear();
        for (String f : friendDAO.getAcceptedFriends(myUsername)) friendModel.addElement(f);

        groupModel.clear();
        for (String g : friendDAO.getMyGroups(myUsername)) groupModel.addElement(g);
    }

   private void performSendMessage() {
    String msg = txtMessage.getText().trim();
    if (msg.isEmpty()) return;

    if (currentTarget.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Vui lòng chọn đối tượng từ danh sách để chat!");
        return;
    }

    if (isGroupChat) {
        ClientSocketManager.getInstance().sendRequest("GROUP_CHAT;" + currentTarget + ";" + msg);
        // THAY ĐỔI: Hiện tên của mình thay vì "Tôi (Nhóm...)"
        chatArea.append(myUsername + ": " + msg + "\n");
    } else {
        ClientSocketManager.getInstance().sendRequest("CHAT;" + currentTarget + ";" + msg);
        // THAY ĐỔI: Chỉ hiện tên của mình thay vì "Tôi -> currentTarget"
        chatArea.append(myUsername + ": " + msg + "\n");
    }
    txtMessage.setText("");
}

    private void performCreateGroup() {
        String groupName = JOptionPane.showInputDialog(this, "Nhập tên nhóm trò chuyện cần tạo:");
        if (groupName == null || groupName.trim().isEmpty()) return;
        groupName = groupName.trim();

        String memberInput = JOptionPane.showInputDialog(this, "Nhập Username các thành viên muốn thêm (cách nhau bởi dấu phẩy):");
        if (memberInput == null) return;

        List<String> chosenMembers = new ArrayList<>();
        for (String name : memberInput.split(",")) {
            if (!name.trim().isEmpty()) chosenMembers.add(name.trim());
        }

        if (friendDAO.createGroup(groupName, myUsername, chosenMembers)) {
            JOptionPane.showMessageDialog(this, "Tạo nhóm [" + groupName + "] thành công!");
            loadFriendAndGroupData(); 
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi tạo nhóm! Kiểm tra lại tên thành viên.");
        }
    }

    public void activateListening() {
        new Thread(() -> {
            try {
                while (true) {
                    String response = ClientSocketManager.getInstance().receiveResponse();
                    if (response != null) {
                        String[] data = response.split(";");
                        String cmd = data[0];
                        
                        if (cmd.equals("RECEIVE_MSG")) {
                            String sender = data[1];
                            String content = data[2];
                            if (!isGroupChat && sender.equalsIgnoreCase(currentTarget)) {
                                chatArea.append(sender + ": " + content + "\n");
                            } else {
                                chatArea.append("[" + sender + " nhắn riêng]: " + content + "\n");
                            }
                        } 
                        else if (cmd.equals("RECEIVE_GROUP_MSG")) {
                            String groupName = data[1];
                            String sender = data[2];
                            String content = data[3];
                            
                            if (isGroupChat && groupName.equalsIgnoreCase(currentTarget)) {
                                if (!sender.equalsIgnoreCase(myUsername)) {
                                    chatArea.append(sender + ": " + content + "\n");
                                }
                            } else {
                                chatArea.append("[Nhóm " + groupName + "] " + sender + ": " + content + "\n");
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("❌ Lỗi luồng đọc Client: " + ex.getMessage());
            }
        }).start();
    }
}