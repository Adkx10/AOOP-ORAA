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
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import utilities.UtilMethods;

public class LogInPage extends JFrame implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(LogInPage.class.getName());

    private BackgroundPanel backgroundPanel;
    private JLabel usernameLabel, passwordLabel, messageLabel;
    private JTextField userText;
    private JPasswordField pwText;
    private JButton loginButton;

    private static final String BACKGROUND_IMAGE_PATH = "aesthetics.png";

    public LogInPage() {
        initializeUI();
        this.setVisible(true);
    }

    private void initializeUI() {
        setTitle("MotorPH Log In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        
        backgroundPanel = new BackgroundPanel(BACKGROUND_IMAGE_PATH);
        backgroundPanel.setLayout(new GridBagLayout());
        add(backgroundPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // --- Username Label ---
        usernameLabel = new JLabel("User Name:");
        styleLabel(usernameLabel);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST; // Align to the right
        backgroundPanel.add(usernameLabel, gbc);

        // --- Username Text Field ---
        userText = new JTextField(20);
        styleTextField(userText);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST; // Align to the left
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontal space
        backgroundPanel.add(userText, gbc);

        // --- Password Label ---
        passwordLabel = new JLabel("Password:");
        styleLabel(passwordLabel);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        backgroundPanel.add(passwordLabel, gbc);

        // --- Password Text Field ---
        pwText = new JPasswordField(20);
        styleTextField(pwText);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(pwText, gbc);

        // --- Login Button ---
        loginButton = new JButton("Login");
        UtilMethods.styleButton(loginButton);
        loginButton.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Span two columns
        gbc.anchor = GridBagConstraints.CENTER; // Center the button
        backgroundPanel.add(loginButton, gbc);

        // --- Message Label (for success/error) ---
        messageLabel = new JLabel("");
        styleMessageLabel(messageLabel);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        backgroundPanel.add(messageLabel, gbc);

        pack(); // Pack components to their preferred sizes
    }

    // Helper method to style JLabels
    private void styleLabel(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(Color.BLACK); // Dark text for contrast
        // Optional: Add a semi-transparent background if text is hard to read on image
        label.setOpaque(true);
        label.setBackground(new Color(255, 255, 255, 180)); // White with 70% opacity
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    // Helper method to style JTextFields and JPasswordFields
    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 150), 1), // Gray border
                BorderFactory.createEmptyBorder(5, 8, 5, 8) // Inner padding
        ));
        textField.setPreferredSize(new Dimension(200, 30)); // Set preferred size
    }

    // Helper method to style the message label
    private void styleMessageLabel(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(Color.RED); // Default to red for error messages
        label.setHorizontalAlignment(JLabel.CENTER); // Center text
        // Optional: Add a semi-transparent background if text is hard to read on image
        label.setOpaque(true);
        label.setBackground(new Color(255, 255, 255, 180));
    }

    // Custom JPanel to draw the background image
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel(String imagePath) {
            try {
                // Load the image from the specified path
                ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource(imagePath));
                if (icon.getImageLoadStatus() == java.awt.MediaTracker.ERRORED) {
                    LOGGER.log(Level.SEVERE, "Failed to load background image: " + imagePath);
                    // Fallback to a solid color if image fails to load
                    setBackground(new Color(230, 230, 230));
                } else {
                    this.backgroundImage = icon.getImage();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading background image: " + imagePath, e);
                setBackground(new Color(230, 230, 230));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
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
                Employee employee = Employee.createEmployeeInstance(enteredUsername);
                if (employee != null) {
                    HomePage home = new HomePage(employee);
                    employee.setHomePage(home);
                    home.setVisible(true);
                    this.dispose();
                } else {
                    messageLabel.setForeground(Color.ORANGE); // Use orange for warnings
                    messageLabel.setText("Error: Employee details or access type not found.");
                }
            } else {
                messageLabel.setForeground(Color.RED); // Use red for incorrect credentials
                messageLabel.setText("Incorrect Login Credentials");
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error during login", ex);
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Database error. Please try again later.");
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, "Configuration error during employee instance creation", ex);
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Application error. Contact support.");
        }
    }
}