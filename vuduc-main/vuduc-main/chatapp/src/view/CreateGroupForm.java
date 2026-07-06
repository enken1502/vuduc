package view;

import java.awt.Font;
import javax.swing.*;

public class CreateGroupForm extends JFrame {
    private JTextField txtGroupName;
    private JTextArea txtDescription;
    private JLabel lbOwner;

    public CreateGroupForm() {
        setTitle("Tạo nhóm chat");
        setSize(450, 380);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(null);

        JLabel title = new JLabel("TẠO NHÓM CHAT");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBounds(120, 15, 220, 30);

        JLabel lbGroupName = new JLabel("Tên nhóm:");
        lbGroupName.setBounds(30, 70, 100, 30);
        txtGroupName = new JTextField();
        txtGroupName.setBounds(140, 70, 250, 30);

        JLabel lbDesc = new JLabel("Mô tả:");
        lbDesc.setBounds(30, 120, 100, 30);
        txtDescription = new JTextArea();
        JScrollPane scroll = new JScrollPane(txtDescription);
        scroll.setBounds(140, 120, 250, 80);

        JLabel lbCreate = new JLabel("Người tạo:");
        lbCreate.setBounds(30, 220, 100, 30);

        lbOwner = new JLabel(ClientSocketManager.getInstance().getUsername());
        lbOwner.setBounds(140, 220, 250, 30);

        JButton btnCreate = new JButton("Tạo nhóm");
        btnCreate.setBounds(70, 280, 130, 35);

        JButton btnCancel = new JButton("Đóng");
        btnCancel.setBounds(240, 280, 130, 35);

        btnCreate.addActionListener(e -> {
            String groupName = txtGroupName.getText().trim();
            String description = txtDescription.getText().trim();

            if (groupName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm!");
                return;
            }

            ClientSocketManager.getInstance().sendRequest("CREATE_GROUP;" + groupName + ";" + description);
            JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu tạo nhóm!");
            dispose();
        });

        btnCancel.addActionListener(e -> dispose());

        panel.add(title); panel.add(lbGroupName); panel.add(txtGroupName);
        panel.add(lbDesc); panel.add(scroll); panel.add(lbCreate); panel.add(lbOwner);
        panel.add(btnCreate); panel.add(btnCancel);
        add(panel);
        setVisible(true);
    }
}