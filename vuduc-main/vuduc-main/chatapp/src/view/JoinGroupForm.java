package view;

import javax.swing.*;

public class JoinGroupForm extends JFrame {
    public JoinGroupForm() {
        setTitle("Tham gia nhóm");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(null);

        JLabel lbCode = new JLabel("Mã nhóm:");
        lbCode.setBounds(20, 40, 100, 30);

        JTextField txtCode = new JTextField();
        txtCode.setBounds(120, 40, 200, 30);

        JButton btnJoin = new JButton("Tham gia");
        btnJoin.setBounds(120, 100, 120, 35);

        btnJoin.addActionListener(e -> {
            String code = txtCode.getText().trim();
            if(!code.isEmpty()){
                JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu xin vào nhóm: " + code);
                dispose();
            }
        });

        panel.add(lbCode);
        panel.add(txtCode);
        panel.add(btnJoin);
        add(panel);
        setVisible(true);
    }
}