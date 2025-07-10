package model;

import dao.CredentialDAO;
import gui.HomePage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LogInPage extends JFrame implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(LogInPage.class.getName());

    private JPanel panel;
    private JLabel label, pwLabel, success;
    private JTextField userText;
    private JPasswordField pwText;
    private JButton button;

    public LogInPage() {
        initializeUI();
        this.setVisible(true);
    }

    private void initializeUI() {
        setTitle("MotorPH Log In");
        setSize(350, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        panel.setLayout(null);
        add(panel);

        label = new JLabel("User Name:");
        label.setBounds(10, 20, 80, 25);
        panel.add(label);

        userText = new JTextField(20);
        userText.setBounds(100, 20, 165, 25);
        panel.add(userText);

        pwLabel = new JLabel("Password:");
        pwLabel.setBounds(10, 60, 80, 25);
        panel.add(pwLabel);

        pwText = new JPasswordField();
        pwText.setBounds(100, 60, 165, 25);
        panel.add(pwText);

        button = new JButton("Login");
        button.setBounds(135, 90, 80, 25);
        button.addActionListener(this);
        panel.add(button);

        success = new JLabel("");
        success.setBounds(10, 120, 300, 25);
        panel.add(success);
    }


    private boolean authenticateUser(String username, String password) throws SQLException {
        CredentialDAO credentialDAO = new CredentialDAO();
        return credentialDAO.authenticateUser(username, password);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String enteredUsername = userText.getText();
            String enteredPassword = new String(pwText.getPassword());

            if (authenticateUser(enteredUsername, enteredPassword)) {
                // If authentication is successful, create the appropriate Employee instance
                // Pass the username to createEmployeeInstance
                Employee employee = Employee.createEmployeeInstance(enteredUsername);
                if (employee != null) {
                    HomePage home = new HomePage(employee);
                    employee.setHomePage(home);
                    home.setVisible(true);
                    this.dispose();
                } else {
                    success.setText("Error: Employee details or access type not found.");
                    success.setBounds(92, 120, 300, 25);
                }
            } else {
                success.setText("Incorrect Login Credentials");
                success.setBounds(92, 120, 300, 25);
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error during login", ex);
            success.setText("Database error. Please try again later.");
            success.setBounds(92, 120, 300, 25);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, "Configuration error during employee instance creation", ex);
            success.setText("Application error. Contact support.");
            success.setBounds(92, 120, 300, 25);
        }
    }
}
