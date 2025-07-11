package gui;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import dao.LeaveRequestDAO;
import java.util.Date;
import model.LeaveRequest;
import data.DBConnection;
import java.sql.Connection;
import utilities.UtilMethods;

public class Remarks extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(Remarks.class.getName());

    private final String empNo;
    private final Date date;
    private final ViewRequest viewRequest;
    private LeaveRequestDAO leaveRequestDAO;

    public Remarks(String empNo, Date date, ViewRequest viewRequest) {
        initComponents();
        UtilMethods.styleButton(submit);
        this.empNo = empNo;
        this.date = date;
        this.viewRequest = viewRequest;
        this.leaveRequestDAO = new LeaveRequestDAO();
        this.setLocationRelativeTo(null);
    }

    private void updateRequestRemarks(String empNo, Date requestDate, String newStatus, String newRemarks, String approvedByEmployeeId) {
        Connection conn = null; 
        try {
            conn = DBConnection.getTransactionalConnection(); 

            boolean success = leaveRequestDAO.updateLeaveRequestStatusAndRemarks(conn, empNo, requestDate, newStatus, newRemarks, approvedByEmployeeId); 

            if (success) {
                conn.commit();
                JOptionPane.showMessageDialog(this, "Remarks updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
                if (viewRequest != null) {
                    viewRequest.readData(null, null); 
                }
            } else {
                conn.rollback(); 
                JOptionPane.showMessageDialog(this, "Failed to update remarks. Request not found or database error.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            
            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.log(Level.INFO, "Transaction rolled back due to SQL error in Remarks update.");
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction in Remarks update: " + rollbackEx.getMessage(), rollbackEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Database error updating request remarks for employee: " + empNo + " on date: " + requestDate, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
           
            if (conn != null) {
                try {
                    conn.close();
                    LOGGER.log(Level.INFO, "Database connection closed in Remarks update.");
                } catch (SQLException closeEx) {
                    LOGGER.log(Level.SEVERE, "Error closing database connection in Remarks update: " + closeEx.getMessage(), closeEx);
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        remarksField = new javax.swing.JTextField();
        submit = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        remarksField.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        submit.setText("Submit");
        submit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(44, Short.MAX_VALUE)
                .addComponent(submit)
                .addContainerGap(44, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(remarksField)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(remarksField, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(submit)
                .addContainerGap())
        );

        remarksField.getAccessibleContext().setAccessibleDescription("Remarks");

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void submitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitActionPerformed
        String newRemarks = remarksField.getText().trim();
        if (newRemarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Remarks cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String newStatus = "Rejected"; 


        String approvedByEmployeeId = "";
        if (viewRequest != null && viewRequest.getCurrentUser() != null) {
            approvedByEmployeeId = viewRequest.getCurrentUser().getEmployeeNo();
        } else {
            LOGGER.log(Level.WARNING, "Current user not found for Remarks. ApprovedByEmployeeID will be empty.");
            
        }

        updateRequestRemarks(empNo, date, newStatus, newRemarks, approvedByEmployeeId);
    }//GEN-LAST:event_submitActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField remarksField;
    private javax.swing.JButton submit;
    // End of variables declaration//GEN-END:variables
}
