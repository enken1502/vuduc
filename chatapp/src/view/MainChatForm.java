package view;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;

public class MainChatForm extends JFrame {
    private JList<String> jlFriendsSidebar;
    private DefaultListModel<String> friendModel;
    
    private JList<String> jlGroupsSidebar;
    private DefaultListModel<String> groupModel;
    
    private JTextPane chatPane;
    private JTextField txtMessage;
    private JButton btnSend;
    
    private JButton btnSticker;
    private JButton btnSendFile; 
    
    private JMenuItem itemLogout; 
    
    private String myUsername;
    private String currentTarget = ""; 
    private boolean isGroupChat = false; 
    
    private volatile boolean isListening = true; 
    private FriendDAO friendDAO = new FriendDAO();

    public MainChatForm(String myUsername) {
        this.myUsername = myUsername;

        setTitle("Chat App - Tài khoản: " + myUsername);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // =========================================================================
        // 1. THANH MENU CHỨC NĂNG
        // =========================================================================
        JMenuBar menuBar = new JMenuBar();
        JMenu menuChucNang = new JMenu("Chức năng");
        JMenuItem itemFriend = new JMenuItem("Quản lý kết bạn");
        JMenuItem itemCreateGroup = new JMenuItem("Tạo nhóm chat mới");
        JMenuItem menuItemProfile = new JMenuItem("Thông tin cá nhân");
        itemLogout = new JMenuItem("Đăng xuất"); 

        menuChucNang.add(menuItemProfile);
        menuChucNang.add(itemFriend);
        menuChucNang.add(itemCreateGroup);
        menuChucNang.add(new JSeparator()); 
        menuChucNang.add(itemLogout);       
        menuBar.add(menuChucNang);
        setJMenuBar(menuBar);

        // =========================================================================
        // 2. SIDEBAR BÊN TRÁI (Bạn bè & Nhóm)
        // =========================================================================
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        leftPanel.setPreferredSize(new Dimension(240, 0));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel friendPanel = new JPanel(new BorderLayout());
        friendPanel.setBorder(BorderFactory.createTitledBorder("Bạn bè thân thiết"));
        friendModel = new DefaultListModel<>();
        jlFriendsSidebar = new JList<>(friendModel);
        jlFriendsSidebar.setFont(new Font("Arial", Font.PLAIN, 14));
        friendPanel.add(new JScrollPane(jlFriendsSidebar), BorderLayout.CENTER);
        leftPanel.add(friendPanel);

        JPanel groupPanel = new JPanel(new BorderLayout());
        groupPanel.setBorder(BorderFactory.createTitledBorder("Nhóm trò chuyện"));
        groupModel = new DefaultListModel<>();
        jlGroupsSidebar = new JList<>(groupModel);
        jlGroupsSidebar.setFont(new Font("Arial", Font.PLAIN, 14));
        groupPanel.add(new JScrollPane(jlGroupsSidebar), BorderLayout.CENTER);
        leftPanel.add(groupPanel);

        add(leftPanel, BorderLayout.WEST);

        // =========================================================================
        // 3. KHUNG CHAT BÊN PHẢI (JTextPane hiển thị nội dung)
        // =========================================================================
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Arial", Font.PLAIN, 14));
        rightPanel.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // Khung điều khiển phía dưới (Ô nhập text, Nút gửi, Nút File, Nút Sticker)
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        txtMessage = new JTextField();
        txtMessage.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSend = new JButton("Gửi");
        
        // Thanh chức năng chứa icon tiện ích bên trái ô nhập liệu
        JPanel attachmentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnSticker = new JButton("😊"); 
        btnSticker.setToolTipText("Gửi nhãn dán / Sticker");
        
        btnSendFile = new JButton("📁"); 
        btnSendFile.setToolTipText("Gửi tệp tin / File");
        
        attachmentPanel.add(btnSticker);
        attachmentPanel.add(btnSendFile); 
        
        controlPanel.add(attachmentPanel, BorderLayout.WEST);
        controlPanel.add(txtMessage, BorderLayout.CENTER);
        controlPanel.add(btnSend, BorderLayout.EAST);
        
        rightPanel.add(controlPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.CENTER);

        // =========================================================================
        // 4. BỘ LẮNG NGHE SỰ KIỆN (ACTION LISTENERS)
        // =========================================================================
        itemFriend.addActionListener(e -> new FriendForm(myUsername, this));
        itemCreateGroup.addActionListener(e -> performCreateGroup());
        // Khi nhấn chọn mục "Thông tin cá nhân" trong Menu Chức năng
        menuItemProfile.addActionListener(e -> {
    new ProfileForm(myUsername); // Truyền Username hiện tại đang đăng nhập vào
});
        // Đăng xuất
        itemLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất không?", "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                isListening = false; 
                try {
                    ClientSocketManager.getInstance().sendRequest("LOGOUT;" + myUsername);
                    Thread.sleep(100); 
                    if (ClientSocketManager.getInstance().getSocket() != null) {
                        ClientSocketManager.getInstance().getSocket().close();
                    }
                } catch (Exception ex) {
                    System.err.println("➔ Lỗi khi gửi lệnh đăng xuất: " + ex.getMessage());
                }
                this.dispose();
                new LoginForm(); 
            }
        });

        // Chọn bạn chat cá nhân
        jlFriendsSidebar.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jlFriendsSidebar.getSelectedValue() != null) {
                jlGroupsSidebar.clearSelection(); 
                currentTarget = jlFriendsSidebar.getSelectedValue();
                isGroupChat = false;
                
                chatPane.setText(""); 
                appendSystemMessage("--- Đang chat cá nhân với: " + currentTarget + " ---");
                
                List<String[]> history = friendDAO.getChatHistory(myUsername, currentTarget);
                for (String[] msg : history) {
                    String sender = msg[0];
                    String content = msg[1];
                    if (!content.startsWith("__FRIEND_")) {
                        appendChatMessage(sender, content);
                    }
                }
            }
        });

        // Chọn chat nhóm
        jlGroupsSidebar.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jlGroupsSidebar.getSelectedValue() != null) {
                jlFriendsSidebar.clearSelection(); 
                currentTarget = jlGroupsSidebar.getSelectedValue();
                isGroupChat = true;
                
                chatPane.setText(""); 
                appendSystemMessage("--- Đang chat trong nhóm: " + currentTarget + " ---");
                
                List<String[]> history = friendDAO.getGroupChatHistory(currentTarget);
                for (String[] msg : history) {
                    String sender = msg[0];
                    String content = msg[1];
                    if (!content.startsWith("__FRIEND_")) {
                        appendChatMessage(sender, content);
                    }
                }
            }
        });

        btnSend.addActionListener(e -> performSendMessage());
        txtMessage.addActionListener(e -> performSendMessage());
        btnSticker.addActionListener(e -> showStickerDialog());
        btnSendFile.addActionListener(e -> performSendFile()); // Kích hoạt sự kiện gửi file

        loadFriendAndGroupData();
        activateListening();

        setVisible(true);
    }

    // =========================================================================
    // 5. CÁC PHƯƠNG THỨC XỬ LÝ HIỂN THỊ ĐẶC BIỆT TRÊN JTEXTPANE
    // =========================================================================
    
    private void appendSystemMessage(String text) {
        StyledDocument doc = chatPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setForeground(center, Color.GRAY);
        StyleConstants.setItalic(center, true);
        try {
            doc.insertString(doc.getLength(), text + "\n", center);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    // Hàm chèn trực tiếp thông báo hệ thống vào khung chat hiện tại
private void appendSystemAnnouncement(String text) {
    SwingUtilities.invokeLater(() -> {
        try {
            // Trường hợp 1: Nếu bạn xài JTextPane / StyledDocument
            if (chatPane != null) {
                javax.swing.text.StyledDocument doc = chatPane.getStyledDocument();
                javax.swing.text.SimpleAttributeSet style = new javax.swing.text.SimpleAttributeSet();
                javax.swing.text.StyleConstants.setForeground(style, java.awt.Color.RED); // Chữ màu đỏ
                javax.swing.text.StyleConstants.setBold(style, true); // In đậm

                doc.insertString(doc.getLength(), "\n📢 [THÔNG BÁO HỆ THỐNG]: " + text + "\n\n", style);
                chatPane.setCaretPosition(doc.getLength()); // Tự cuộn xuống dưới cùng
            }
        } catch (Exception ex) {
            // Trường hợp 2: Dự phòng nếu dùng appendChatMessage thông thường
            appendChatMessage("HỆ THỐNG", "[THÔNG BÁO]: " + text);
        }
    });
}
    public void loadGroupList() {
    // Gửi yêu cầu lên Server để tải lại danh sách nhóm
    try {
        ClientSocketManager.getInstance().sendRequest("GET_GROUPS");
    } catch (Exception e) {
        System.err.println("Lỗi khi tải danh sách nhóm: " + e.getMessage());
    }
}

    private void appendChatMessage(String sender, String content) {
    StyledDocument doc = chatPane.getStyledDocument();
    SimpleAttributeSet senderStyle = new SimpleAttributeSet();
    
    if (sender.equalsIgnoreCase(myUsername)) {
        StyleConstants.setForeground(senderStyle, new Color(0, 128, 0)); 
        StyleConstants.setBold(senderStyle, true);
    } else {
        StyleConstants.setForeground(senderStyle, Color.BLUE); 
        StyleConstants.setBold(senderStyle, true);
    }

    try {
        doc.insertString(doc.getLength(), sender + ": ", senderStyle);
        
        // 1. XỬ LÝ ĐỊNH DẠNG STICKER
        if (content.startsWith("[STICKER_") && content.endsWith("]")) {
            String stickerName = content.substring(9, content.length() - 1);
            insertSticker(stickerName); 
        } 
        // 2. XỬ LÝ ĐỊNH DẠNG FILE (CHUYỂN THÀNH NÚT DOWNLOAD)
        else if (content.startsWith("[FILE_") && content.contains("_DATA_") && content.endsWith("]")) {
            // Bóc tách lấy tên file và chuỗi dữ liệu Base64
            int dataIndex = content.indexOf("_DATA_");
            String fileName = content.substring(6, dataIndex);
            String base64Data = content.substring(dataIndex + 6, content.length() - 1);
            
            // Tạo một nút bấm tải file giao diện phẳng nhỏ gọn
            JButton btnDownload = new JButton("📁 Tải file: " + fileName);
            btnDownload.setFont(new Font("Arial", Font.PLAIN, 12));
            btnDownload.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnDownload.setBackground(new Color(240, 240, 240));
            
            // Bắt sự kiện khi người nhận click vào nút để lưu file về máy
            btnDownload.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName)); // Gợi ý sẵn tên file gốc
                
                int userSelection = fileChooser.showSaveDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    try {
                        // Giải mã chuỗi Base64 ngược lại thành mảng byte ban đầu
                        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
                        // Ghi mảng byte ra file vật lý trên máy người nhận
                        java.nio.file.Files.write(fileToSave.toPath(), fileBytes);
                        
                        JOptionPane.showMessageDialog(this, "Đã tải và lưu file thành công!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Lỗi khi lưu file: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });
            
            // Chèn nút bấm trực tiếp vào JTextPane ngay tại vị trí dòng chat
            chatPane.setCaretPosition(doc.getLength());
            chatPane.insertComponent(btnDownload);
            appendNewLine();
        } 
        // 3. TIN NHẮN VĂN BẢN THƯỜNG
        else {
            SimpleAttributeSet textStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(textStyle, Color.BLACK);
            doc.insertString(doc.getLength(), content + "\n", textStyle);
        }
    } catch (BadLocationException e) {
        e.printStackTrace();
    }
}

    // Kiểm tra định dạng ảnh kỹ càng bằng ImageIO trước khi chèn vào khung chat
    private void insertSticker(String stickerName) {
        String path = "src/assets/" + stickerName + ".png"; 
        File file = new File(path);
        
        try {
            if (file.exists()) {
                Image img = ImageIO.read(file);
                if (img != null) { // Nếu đúng định dạng ảnh PNG thật
                    ImageIcon icon = new ImageIcon(img.getScaledInstance(80, 80, Image.SCALE_SMOOTH));
                    chatPane.setCaretPosition(chatPane.getDocument().getLength());
                    chatPane.insertIcon(icon);
                    appendNewLine();
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi hiển thị sticker: " + e.getMessage());
        }
        
        // Trực quan hóa văn bản thay thế nếu file lỗi định dạng hệ thống không đọc được
        try {
            chatPane.getStyledDocument().insertString(chatPane.getStyledDocument().getLength(), "[Nhãn dán: " + stickerName + " (File lỗi định dạng)]\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendNewLine() {
        try {
            chatPane.getStyledDocument().insertString(chatPane.getStyledDocument().getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // 6. XỬ LÝ GỬI TIN NHẮN, STICKER VÀ FILE (ĐÃ HOÀN THIỆN LOGIC GỬI FILE)
    // =========================================================================
    
   private void performSendMessage() {
    String msg = txtMessage.getText().trim();
    if (msg.isEmpty()) return;

    if (currentTarget.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Vui lòng chọn đối tượng từ danh sách để chat!");
        return;
    }

    if (isGroupChat) {
        ClientSocketManager.getInstance().sendRequest("GROUP_CHAT;" + currentTarget + ";" + msg);
    } else {
        ClientSocketManager.getInstance().sendRequest("CHAT;" + currentTarget + ";" + msg);
    }
    
    // In tin nhắn lên màn hình của chính mình ngay lập tức
    appendChatMessage(myUsername, msg); 
    txtMessage.setText("");
}

    // Sử dụng cơ chế kiểm tra định dạng thông minh của ImageIO để bắt lỗi file ảnh WEBP giả danh
    private void showStickerDialog() {
        if (currentTarget.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đối tượng trước khi gửi sticker!");
            return;
        }

        JDialog dialog = new JDialog(this, "Chọn nhãn dán", true);
        dialog.setSize(340, 200);
        dialog.setLayout(new FlowLayout());
        dialog.setLocationRelativeTo(this);

        String[] stickers = {"like", "haha", "heart", "sad", "angry"};

        for (String st : stickers) {
            String imgPath = "src/assets/" + st + ".png";
            File f = new File(imgPath);
            JButton btn = new JButton();
            
            try {
                if (f.exists()) {
                    Image img = ImageIO.read(f);
                    if (img != null) { // File chuẩn PNG
                        ImageIcon icon = new ImageIcon(img.getScaledInstance(50, 50, Image.SCALE_SMOOTH));
                        btn.setIcon(icon);
                    } else { // File WEBP cố tình đổi tên đuôi thành .png
                        btn.setText(st);
                        btn.setFont(new Font("Arial", Font.BOLD, 11));
                        btn.setForeground(Color.RED); // Hiện chữ đỏ báo hiệu
                    }
                } else {
                    btn.setText(st);
                }
            } catch (Exception ex) {
                btn.setText(st);
            }
            
            btn.setPreferredSize(new Dimension(65, 65));
            
            btn.addActionListener(e -> {
                String stickerMsg = "[STICKER_" + st + "]";
                if (isGroupChat) {
                    ClientSocketManager.getInstance().sendRequest("GROUP_CHAT;" + currentTarget + ";" + stickerMsg);
                } else {
                    ClientSocketManager.getInstance().sendRequest("CHAT;" + currentTarget + ";" + stickerMsg);
                }
                appendChatMessage(myUsername, stickerMsg); 
                dialog.dispose(); 
            });
            dialog.add(btn);
        }
        dialog.setVisible(true);
    }

    // 🌟 ĐOẠN XỬ LÝ ĐỌC FILE - MÃ HÓA BASE64 VÀ GỬI QUA SOCKET CHUẨN ĐÃ ĐƯỢC HOÀN THIỆN
    private void performSendFile() {
        if (currentTarget.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đối tượng trước khi gửi tệp!");
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Đọc toàn bộ byte dữ liệu của file chọn
                byte[] fileBytes = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                
                // Mã hóa mảng byte vừa đọc sang chuỗi ký tự Base64 truyền tải qua mạng internet
                String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
                
                // Đóng gói chuỗi tin nhắn gửi đi đúng cú pháp hệ thống
                String fileMsg = "[FILE_" + selectedFile.getName() + "_DATA_" + base64Data + "]";
                
                if (isGroupChat) {
                    ClientSocketManager.getInstance().sendRequest("GROUP_CHAT;" + currentTarget + ";" + fileMsg);
                } else {
                    ClientSocketManager.getInstance().sendRequest("CHAT;" + currentTarget + ";" + fileMsg);
                }
                
                // Hiển thị trạng thái gửi file lên khung JTextPane local
                JOptionPane.showMessageDialog(this, "Gửi tệp tin thành công!");
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Có lỗi xảy ra khi mã hóa gửi tệp: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public void loadFriendAndGroupData() {
        friendModel.clear();
        for (String f : friendDAO.getAcceptedFriends(myUsername)) {
            friendModel.addElement(f);
        }

        groupModel.clear();
        for (String g : friendDAO.getMyGroups(myUsername)) {
            groupModel.addElement(g);
        }
    }

    private void performCreateGroup() {
        String groupName = JOptionPane.showInputDialog(this, "Nhập tên nhóm trò chuyện cần tạo:");
        if (groupName == null || groupName.trim().isEmpty()) return;
        groupName = groupName.trim();

        String memberInput = JOptionPane.showInputDialog(this, "Nhập Username các thành viên (cách nhau bởi dấu phẩy):");
        if (memberInput == null) return;

        List<String> chosenMembers = new ArrayList<>();
        for (String name : memberInput.split(",")) {
            if (!name.trim().isEmpty()) {
                chosenMembers.add(name.trim());
            }
        }

        if (friendDAO.createGroup(groupName, myUsername, chosenMembers)) {
            JOptionPane.showMessageDialog(this, "Tạo nhóm [" + groupName + "] thành công!");
            loadFriendAndGroupData(); 
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi tạo nhóm! Hãy kiểm tra lại.");
        }
    }

    // =========================================================================
    // 7. LUỒNG LIÊN TỤC LẮNG NGHE ĐỌC TIN NHẮN TỪ SERVER GỬI VỀ
    // =========================================================================
    public void activateListening() {
        isListening = true; 
        
        new Thread(() -> {
            try {
                while (isListening) {
                    String response = ClientSocketManager.getInstance().receiveResponse();
                    if (response != null) {
                        String[] data = response.split(";");
                        String cmd = data[0];
                        
                        if (cmd.equals("RECEIVE_MSG")) {
    String sender = data[1];
    String content = data[2];
    
    // Nếu tin nhắn là từ chính mình GỬI ĐI hoặc từ bạn chat đang mở -> In lên màn hình
    if (sender.equalsIgnoreCase(myUsername) || (!isGroupChat && sender.equalsIgnoreCase(currentTarget))) {
        appendChatMessage(sender, content);
    } else {
        appendChatMessage(sender, "[Tin nhắn riêng]: " + content);
    }
}
                        else if (cmd.equals("SEND_MSG_FAILED")) {
                        String errorMsg = (data.length > 1) ? data[1] : "Tài khoản không tồn tại";
                        
                        // Dùng SwingUtilities.invokeLater để hiển thị Popup mượt mà không làm đơ UI
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, errorMsg, "Thông báo", JOptionPane.WARNING_MESSAGE);
                        });
                    }
                        // Trong hàm activateListening() của MainChatForm.java:
                        else if (cmd.equals("ANNOUNCEMENT")) {
                        String annText = data.length > 1 ? data[1] : "";      
                            appendSystemAnnouncement(annText);
                        }
                        // Trong hàm activateListening() của MainChatForm.java:
else if (cmd.equals("FORCE_LOGOUT")) {
    String reason = (data.length > 1) ? data[1] : "Tài khoản của bạn đã bị khóa!";
    
    // 1. Tắt cờ lắng nghe ngay lập tức
    isListening = false; 
    
    // 2. Đóng kết nối socket hiện tại để xả luồng cũ
    
    SwingUtilities.invokeLater(() -> {
        JOptionPane.showMessageDialog(this, reason, "Thông báo hệ thống", JOptionPane.ERROR_MESSAGE);
        this.dispose(); // Tắt MainChatForm
        new LoginForm(); // Mở LoginForm
    });
    break; // Thoát vòng lặp while của Thread
}
                        else if (cmd.equals("RECEIVE_GROUP_MSG")) {
                            String groupName = data[1];
                            String sender = data[2];
                            String content = data[3];
                            
                            if (isGroupChat && groupName.equalsIgnoreCase(currentTarget)) {
                                if (!sender.equalsIgnoreCase(myUsername)) {
                                    appendChatMessage(sender, content);
                                }
                            } else {
                                appendChatMessage(sender, "[Nhóm " + groupName + "]: " + content);
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception ex) {
                System.out.println("➔ Luồng nghe tin nhắn đã dừng.");
            }
        }).start();
    }
}