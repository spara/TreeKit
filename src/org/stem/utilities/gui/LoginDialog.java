package org.stem.utilities.gui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

public class LoginDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -4502294083702682450L;

	JLabel labelName;
    JLabel labelPass;
    JTextField textName;
    JPasswordField passField;
    JButton okButton;
    JButton cancelButton;
    JDialog dialog;

    public LoginDialog() {
        JPanel panelOne = new JPanel();
        labelName = new JLabel("Name");
        textName = new JTextField(15);
        //textName.setText("stemliveshere");
        panelOne.add(labelName);
        panelOne.add(textName);

        JPanel panelTwo = new JPanel();
        labelPass = new JLabel("Password");
        passField = new JPasswordField(15);
        //passField.setText("thewholeteam");
        panelTwo.add(labelPass);
        panelTwo.add(passField);

        JPanel panelThree = new JPanel();
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        panelThree.add(okButton);
        panelThree.add(cancelButton);

        dialog = new JDialog();
        dialog.setResizable(false);
        dialog.getContentPane().add(panelOne);
        dialog.getContentPane().add(panelTwo);
        dialog.getContentPane().add(panelThree);
        dialog.setTitle("Login in to Google Docs");
        dialog.getContentPane().setLayout(new FlowLayout());
        dialog.setSize(350, 150);
        dialog.setLocationRelativeTo(null); // place in center of screen
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            dialog.dispose();
        } else if (e.getSource() == cancelButton) {
            System.exit(0);
        }
    }

    public String getUserName() {
        return textName.getText();
    }

    public String getPassword() {
        return String.valueOf(passField.getPassword());
    }
}
