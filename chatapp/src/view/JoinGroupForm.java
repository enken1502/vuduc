package view;

import javax.swing.*;

public class JoinGroupForm extends JFrame {

    public JoinGroupForm() {

        setTitle("Tham gia nhóm");
        setSize(400,250);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel lbCode = new JLabel("Mã nhóm:");
        lbCode.setBounds(20,40,100,30);

        JTextField txtCode = new JTextField();
        txtCode.setBounds(120,40,200,30);

        JButton btnJoin = new JButton("Tham gia");
        btnJoin.setBounds(120,100,120,35);

        panel.add(lbCode);
        panel.add(txtCode);
        panel.add(btnJoin);

        add(panel);
        setVisible(true);
    }
}