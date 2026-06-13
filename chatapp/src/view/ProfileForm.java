package view;

import javax.swing.*;

public class ProfileForm extends JFrame {

    public ProfileForm() {

        setTitle("Thông tin cá nhân");
        setSize(400,350);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lbName = new JLabel("Họ tên:");
        lbName.setBounds(30,30,100,30);

        JTextField txtName = new JTextField();
        txtName.setBounds(130,30,200,30);

        JLabel lbEmail = new JLabel("Email:");
        lbEmail.setBounds(30,80,100,30);

        JTextField txtEmail = new JTextField();
        txtEmail.setBounds(130,80,200,30);

        JLabel lbPhone = new JLabel("SĐT:");
        lbPhone.setBounds(30,130,100,30);

        JTextField txtPhone = new JTextField();
        txtPhone.setBounds(130,130,200,30);

        JButton btnUpdate = new JButton("Cập nhật");
        btnUpdate.setBounds(130,200,120,35);

        panel.add(lbName);
        panel.add(txtName);
        panel.add(lbEmail);
        panel.add(txtEmail);
        panel.add(lbPhone);
        panel.add(txtPhone);
        panel.add(btnUpdate);

        add(panel);
        setVisible(true);
    }
}