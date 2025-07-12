package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import model.Employee;
import model.Admin;
import model.Manager;
import model.RegularEmployee;
import model.LogInPage;
import utilities.UtilMethods;

public class HomePage extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(HomePage.class.getName());

    private Employee currentUser;

//    private JButton addUpdateDeleteBtn;
//    private JButton viewAllSalaryBtn;
//    private JButton viewLeaveBtn;
//    private JButton viewEmployeeDetailsBtn;
//    private JButton viewSalaryBtn;
//    private JButton requestLeaveBtn;
//    private JButton viewPersonalDetailsBtn;

//    private JPanel buttonPanel;
//    private JPanel centerPanel;

    public HomePage(Employee user) {
        this.currentUser = user;
        initComponents();
        initializeUI();
        configureButtons();
        showDate();
        this.setLocationRelativeTo(null);
    }

    public void showDate() {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
        Date d = new Date();
        date.setText(s.format(d));
    }

    public JButton getAddUpdateDeleteBtn() {
        return addUpdateDeleteBtn;
    }

    public JButton getViewAllSalaryBtn() {
        return viewAllSalaryBtn;
    }

    public JButton getViewLeaveBtn() {
        return viewLeaveBtn;
    }

    public JButton getViewEmployeeDetailsBtn() {
        return viewEmployeeDetailsBtn;
    }

    public JButton getViewSalaryBtn() {
        return viewSalaryBtn;
    }

    public JButton getRequestLeaveBtn() {
        return requestLeaveBtn;
    }

    public JButton getViewPersonalDetailsBtn() {
        return viewPersonalDetailsBtn;
    }

    public JLabel getLabel1() {
        return currentUserLabel;
    }

    public Employee getCurrentUser() {
        return currentUser;
    }

    private void initializeUI() {
        // Initialize buttons
        UtilMethods.styleButton(addUpdateDeleteBtn);
        UtilMethods.styleButton(viewAllSalaryBtn);
        UtilMethods.styleButton(viewLeaveBtn);
        UtilMethods.styleButton(viewEmployeeDetailsBtn);
        UtilMethods.styleButton(viewPersonalDetailsBtn);
        UtilMethods.styleButton(viewSalaryBtn);
        UtilMethods.styleButton(requestLeaveBtn);
        
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutButton.setBackground(new Color(100, 100, 100)); // Darker gray
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createRaisedBevelBorder());
        logoutButton.setPreferredSize(new Dimension(120, 30));

        //Button action listeners
        //Admin
        addUpdateDeleteBtn.addActionListener(e -> {
            if (currentUser instanceof Admin admin) {
                admin.viewEmployeeDetails();
                this.dispose();
            }
        });

        //Admin & Manager
        viewAllSalaryBtn.addActionListener(e -> {
            if (currentUser instanceof Admin admin) {
                admin.viewSalary();
            } else if (currentUser instanceof Manager manager) {
                manager.viewSalary();
            }
            this.dispose();
        });

        //Admin & Manager
        viewLeaveBtn.addActionListener(e -> {
            if (currentUser instanceof Admin admin) {
                admin.processLeaveRequest();
            } else if (currentUser instanceof Manager manager) {
                manager.processLeaveRequest();
            }
            this.dispose();
        });

        //Manager
        viewEmployeeDetailsBtn.addActionListener(e -> {
            if (currentUser instanceof Manager manager) {
                manager.viewEmployeeDetails();
                this.dispose();
            }
        });

        //Regular Employee
        requestLeaveBtn.addActionListener(e -> {
            if (currentUser instanceof RegularEmployee regular) {
                regular.processLeaveRequest();
                this.dispose();
            }
        });

        //Regular Employee
        viewPersonalDetailsBtn.addActionListener(e -> {
            if (currentUser instanceof RegularEmployee regular) {
                regular.viewEmployeeDetails();
                this.dispose();
            }
        });

        //Regular Employee
        viewSalaryBtn.addActionListener(e -> {
            if (currentUser instanceof RegularEmployee regular) {
                regular.viewSalary();
                this.dispose();
            }
        });

//        // Set button size
//        Dimension buttonSize = new Dimension(200, 50);
//        addUpdateDeleteBtn.setPreferredSize(buttonSize);
//        viewAllSalaryBtn.setPreferredSize(buttonSize);
//        viewLeaveBtn.setPreferredSize(buttonSize);
//        viewEmployeeDetailsBtn.setPreferredSize(buttonSize);
//        viewPersonalDetailsBtn.setPreferredSize(buttonSize);
//        viewSalaryBtn.setPreferredSize(buttonSize);
//        requestLeaveBtn.setPreferredSize(buttonSize);
//
        // Panel with GridLayout (1 column, multiple rows)
        //buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        // Wrapper panel for centering
        //centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 200));
        //centerPanel.add(buttonPanel);

        // Set frame layout
        //setLayout(new BorderLayout());
        //add(centerPanel, BorderLayout.CENTER);

        //setSize(400, 400);
       //setLocationRelativeTo(null);
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void configureButtons() {
        buttonPanel.removeAll();
        currentUser.accessPermissions(this); //Let Employee subclass decide buttons
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    public void addButton(JButton button) {
        buttonPanel.add(button);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        date = new javax.swing.JLabel();
        currentUserLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        logoutButton = new javax.swing.JButton();
        buttonPanel = new javax.swing.JPanel();
        requestLeaveBtn = new javax.swing.JButton();
        viewSalaryBtn = new javax.swing.JButton();
        viewPersonalDetailsBtn = new javax.swing.JButton();
        viewEmployeeDetailsBtn = new javax.swing.JButton();
        addUpdateDeleteBtn = new javax.swing.JButton();
        viewAllSalaryBtn = new javax.swing.JButton();
        viewLeaveBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MotorPH System");
        setBounds(new java.awt.Rectangle(0, 0, 0, 0));
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        setLocation(new java.awt.Point(0, 0));
        setMinimumSize(new java.awt.Dimension(700, 540));
        setSize(new java.awt.Dimension(0, 0));

        jPanel1.setBackground(new java.awt.Color(0, 102, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(150, 100));
        jPanel1.setLayout(null);

        date.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        date.setText("jLabel1");
        jPanel1.add(date);
        date.setBounds(10, 10, 100, 16);

        currentUserLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        currentUserLabel.setText("Employee ID: " + currentUser.getEmployeeNo());
        jPanel1.add(currentUserLabel);
        currentUserLabel.setBounds(10, 70, 210, 16);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorph.png"))); // NOI18N
        jPanel1.add(jLabel1);
        jLabel1.setBounds(-30, -60, 2280, 170);

        logoutButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        logoutButton.setText("Log Out");
        logoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutButtonActionPerformed(evt);
            }
        });

        buttonPanel.setLayout(new java.awt.GridLayout(0, 1, 10, 10));

        requestLeaveBtn.setBackground(new java.awt.Color(50, 150, 250));
        requestLeaveBtn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        requestLeaveBtn.setForeground(new java.awt.Color(255, 255, 255));
        requestLeaveBtn.setText("Request Leave");
        requestLeaveBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(requestLeaveBtn);

        viewSalaryBtn.setText("View Salary");
        viewSalaryBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(viewSalaryBtn);

        viewPersonalDetailsBtn.setText("View Personal Details");
        viewPersonalDetailsBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(viewPersonalDetailsBtn);

        viewEmployeeDetailsBtn.setText("View Employee Details");
        viewEmployeeDetailsBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(viewEmployeeDetailsBtn);

        addUpdateDeleteBtn.setText("Manage Employees");
        addUpdateDeleteBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(addUpdateDeleteBtn);

        viewAllSalaryBtn.setText("View All Salaries");
        viewAllSalaryBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(viewAllSalaryBtn);

        viewLeaveBtn.setText("View Leave Requests");
        viewLeaveBtn.setPreferredSize(new java.awt.Dimension(200, 50));
        buttonPanel.add(viewLeaveBtn);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(258, Short.MAX_VALUE)
                .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 162, Short.MAX_VALUE)
                .addComponent(logoutButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logoutButton)
                    .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void logoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutButtonActionPerformed

        LogInPage login = new LogInPage();
        login.setVisible(true); 
        this.dispose();
    }//GEN-LAST:event_logoutButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addUpdateDeleteBtn;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JLabel currentUserLabel;
    private javax.swing.JLabel date;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton logoutButton;
    private javax.swing.JButton requestLeaveBtn;
    private javax.swing.JButton viewAllSalaryBtn;
    private javax.swing.JButton viewEmployeeDetailsBtn;
    private javax.swing.JButton viewLeaveBtn;
    private javax.swing.JButton viewPersonalDetailsBtn;
    private javax.swing.JButton viewSalaryBtn;
    // End of variables declaration//GEN-END:variables
}
