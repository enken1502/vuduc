package view;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class FriendController {
    private FriendDAO friendDAO = new FriendDAO();
    private String myUsername;
    private FriendForm friendFormInstance;

    public FriendController(String myUsername, FriendForm friendFormInstance) {
        this.myUsername = myUsername;
        this.friendFormInstance = friendFormInstance;
    }

    public void setFriendFormInstance(FriendForm friendFormInstance) {
        this.friendFormInstance = friendFormInstance;
    }

    public void processFriendSignal(String signalType, String sender) {
        if (signalType.equals("__FRIEND_REQUEST__")) {
            handleIncomingRequest(sender);
        } else if (signalType.equals("__FRIEND_ACCEPT__")) {
            handleRequestAccepted(sender);
        } else if (signalType.equals("__FRIEND_DELETE__")) { // Tiếp nhận tín hiệu hủy kết bạn từ mạng
            handleIncomingDelete(sender);
        }
    }

    private void handleIncomingRequest(String sender) {
        friendDAO.insertFriendRequest(sender, myUsername); //[cite: 1]

        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(null,
                    "Người dùng [" + sender + "] muốn kết bạn với bạn. Bạn có đồng ý không?",
                    "Lời mời kết bạn mới", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE); //[cite: 1]

            if (choice == JOptionPane.YES_OPTION) {
                friendDAO.acceptFriend(sender, myUsername); //[cite: 1]
                ClientSocketManager.getInstance().sendRequest("CHAT_PRIVATE;" + sender + ";__FRIEND_ACCEPT__"); //[cite: 1]

                if (friendFormInstance != null && friendFormInstance.isShowing()) {
                    friendFormInstance.loadFriendList(); //[cite: 1]
                }
                JOptionPane.showMessageDialog(null, "Hai bạn đã trở thành bạn bè thành công!"); //[cite: 1]
            }
        });
    }

    private void handleRequestAccepted(String sender) {
        friendDAO.acceptFriend(myUsername, sender); //[cite: 1]

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                    "[" + sender + "] đã đồng ý lời mời kết bạn của bạn!", 
                    "Kết bạn thành công 🎉", 
                    JOptionPane.INFORMATION_MESSAGE); //[cite: 1]

            if (friendFormInstance != null && friendFormInstance.isShowing()) {
                friendFormInstance.loadFriendList(); //[cite: 1]
            }
        });
    }

    private void handleIncomingDelete(String sender) {
        // Thực hiện xóa mối quan hệ bạn bè trong Database cục bộ khi nhận tín hiệu từ Server
        friendDAO.rejectOrDeleteFriend(sender, myUsername);

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                    "Người dùng [" + sender + "] đã hủy kết bạn với bạn.", 
                    "Thông báo hệ thống", 
                    JOptionPane.INFORMATION_MESSAGE);

            // Nếu giao diện danh sách bạn bè đang mở thì tự động reload làm mới dữ liệu
            if (friendFormInstance != null && friendFormInstance.isShowing()) {
                friendFormInstance.loadFriendList();
            }
        });
    }
}