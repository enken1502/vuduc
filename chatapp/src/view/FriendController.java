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
        }
    }

    private void handleIncomingRequest(String sender) {
        friendDAO.insertFriendRequest(sender, myUsername);

        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(null,
                    "Người dùng [" + sender + "] muốn kết bạn với bạn. Bạn có đồng ý không?",
                    "Lời mời kết bạn mới", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                friendDAO.acceptFriend(sender, myUsername);
                ClientSocketManager.getInstance().sendRequest("CHAT_PRIVATE;" + sender + ";__FRIEND_ACCEPT__");

                if (friendFormInstance != null && friendFormInstance.isShowing()) {
                    friendFormInstance.loadFriendList();
                }
                JOptionPane.showMessageDialog(null, "Hai bạn đã trở thành bạn bè thành công!");
            }
        });
    }

    private void handleRequestAccepted(String sender) {
        friendDAO.acceptFriend(myUsername, sender);

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                    "[" + sender + "] đã đồng ý lời mời kết bạn của bạn!", 
                    "Kết bạn thành công 🎉", 
                    JOptionPane.INFORMATION_MESSAGE);

            if (friendFormInstance != null && friendFormInstance.isShowing()) {
                friendFormInstance.loadFriendList();
            }
        });
    }
}