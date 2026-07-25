package view;

import javax.swing.*;

public class CreateGroupForm extends JFrame {

    public CreateGroupForm() {

        setTitle("Tạo nhóm");
        setSize(400,250);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lbName = new JLabel("Tên nhóm:");
        lbName.setBounds(20,40,100,30);

        JTextField txtName = new JTextField();
        txtName.setBounds(120,40,200,30);

        JButton btnCreate = new JButton("Tạo nhóm");
        btnCreate.setBounds(120,100,120,35);

        panel.add(lbName);
        panel.add(txtName);
        panel.add(btnCreate);

        add(panel);
        setVisible(true);
    }
}