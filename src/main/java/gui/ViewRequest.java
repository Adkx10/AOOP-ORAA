package gui;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JDateChooser;

import model.Employee;
import model.Admin;
import model.Manager;
import model.RegularEmployee;
import model.LeaveRequest;
import dao.LeaveRequestDAO;
import dao.EmployeeDAO;
import dao.LeaveTypeDAO;
import java.util.Calendar;
import data.DBConnection;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;
import javax.swing.BorderFactory;
import utilities.UtilMethods;

public class ViewRequest extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(ViewRequest.class.getName());

    private String empNo; // Current logged-in employee's ID (or search emp ID)
    private HomePage homePage;
    private Employee currentUser;
    private LeaveRequestDAO leaveRequestDAO;
    private EmployeeDAO employeeDAO; // Used to get current user's FN/LN for submission
    private LeaveTypeDAO leaveTypeDAO; // To get leave type IDs
    private List<LeaveRequest> displayedRequests = new ArrayList<>(); // To hold requests shown in the table

    public ViewRequest() {
        this(null);
    }

    public ViewRequest(HomePage homePage) {
        this.homePage = homePage;
        this.currentUser = (homePage != null) ? homePage.getCurrentUser() : null;
        this.empNo = (currentUser instanceof RegularEmployee) ? currentUser.getEmployeeNo() : null;

        try {
            this.leaveRequestDAO = new LeaveRequestDAO();
            this.employeeDAO = new EmployeeDAO();
            this.leaveTypeDAO = new LeaveTypeDAO();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal Error: Failed to initialize DAOs in ViewRequest constructor.", e);
            JOptionPane.showMessageDialog(this,
                    "Application startup error: Could not connect to database or initialize components.\n"
                    + "Please check database connection and logs for details.\n" + e.getMessage(),
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
        }

        initComponents();
        applyCustomStyles();
        if (processRequestedDate != null) {
            processRequestedDate.setDateFormatString("yyyy-MM-dd");
        }
        if (requestedDate != null) {
            requestedDate.setDateFormatString("yyyy-MM-dd");
        }

        // Populate leaveType JComboBox
        try {
            List<String> leaveNames = leaveTypeDAO.getAllLeaveTypeNames();
            leaveType.removeAllItems();
            for (String name : leaveNames) {
                leaveType.addItem(name);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error populating leave types.", ex);
            JOptionPane.showMessageDialog(this, "Error loading leave types: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        showDate();

        try {
            readData(this.empNo, null);
            readLeaveBalanceData(this.empNo);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error initializing ViewRequest GUI data load", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDate() {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
        Date d = new Date();
        jLabel4.setText(s.format(d));
    }

    public boolean readData(String empNoToFilter, Date filterRequestedDate) throws SQLException {
        boolean requestsFound = false;
        DefaultTableModel model = (DefaultTableModel) requestTable.getModel();
        model.setRowCount(0);
        requestTable.setAutoCreateRowSorter(true);

        List<LeaveRequest> requests;

        if (empNoToFilter != null && !empNoToFilter.isEmpty()) {
            requests = leaveRequestDAO.getLeaveRequestsByEmployeeNo(empNoToFilter);
        } else {
            requests = leaveRequestDAO.getAllLeaveRequests();
        }

        List<LeaveRequest> filteredRequests = new ArrayList<>();
        SimpleDateFormat displaySdf = new SimpleDateFormat("yyyy-MM-dd");

        // Filter by requested date if provided
        if (filterRequestedDate != null) {

            Date normalizedFilterDate = normalizeDateToMidnight(filterRequestedDate);

            for (LeaveRequest req : requests) {
                Date reqRequestedDate = req.getRequestedDate();

                Date normalizedReqDate = (reqRequestedDate != null) ? normalizeDateToMidnight(reqRequestedDate) : null;

                if (normalizedReqDate != null && normalizedReqDate.equals(normalizedFilterDate)) {
                    filteredRequests.add(req);
                }
            }
        } else {
            filteredRequests = requests;
        }

        for (LeaveRequest req : filteredRequests) {
            boolean addRow = false;
            if (currentUser instanceof Admin || currentUser instanceof Manager) {
                addRow = true;
            } else if (currentUser instanceof RegularEmployee) {
                if (req.getEmployeeId().equals(currentUser.getEmployeeNo())) {
                    addRow = true;
                }
            }

            if (addRow) {
                String displayRequestedDate = (req.getRequestedDate() != null) ? displaySdf.format(req.getRequestedDate()) : "";
                String displaySubmissionDate = (req.getSubmissionDate() != null) ? displaySdf.format(req.getSubmissionDate()) : "";

                model.addRow(new Object[]{
                    req.getEmployeeId(), req.getLastName(), req.getFirstName(),
                    req.getReason(),
                    req.getLeaveTypeName(),
                    displayRequestedDate,
                    req.getStatus(),
                    req.getRemarks(),
                    displaySubmissionDate
                });
                requestsFound = true;
            }
        }
        model.fireTableDataChanged();
        return requestsFound;
    }

    public void updateRequest(Connection conn, String empNo, Date requestedDate, String newStatus, String newRemarks) throws SQLException {
        String approvedById = currentUser.getEmployeeNo();

        Date normalizedRequestedDate = normalizeDateToMidnight(requestedDate);

        boolean success = leaveRequestDAO.updateLeaveRequestStatusAndRemarks(conn, empNo, normalizedRequestedDate, newStatus, newRemarks, approvedById); //

        if (!success) {
            throw new SQLException("Failed to update request status. Request not found or database error.");
        }

        if ("Approved".equalsIgnoreCase(newStatus)) {
            List<LeaveRequest> requests = leaveRequestDAO.getLeaveRequestsByEmployeeNo(empNo);
            LeaveRequest approvedReq = null;
            for (LeaveRequest req : requests) {
                Date reqNormalizedDate = (req.getRequestedDate() != null) ? normalizeDateToMidnight(req.getRequestedDate()) : null;
                if (reqNormalizedDate != null && reqNormalizedDate.equals(normalizedRequestedDate)) {
                    approvedReq = req;
                    break;
                }
            }

            if (approvedReq != null) {
                long diffDays = 1;

                // Deduct leave balance
                boolean deductionSuccess = leaveRequestDAO.deductLeaveBalance(
                        conn,
                        approvedReq.getEmployeeId(),
                        approvedReq.getLeaveTypeID(),
                        (double) diffDays
                );
                if (!deductionSuccess) {
                    throw new SQLException("Failed to deduct leave balance for employee " + empNo);
                }
            } else {
                throw new SQLException("Approved request details not found for balance deduction.");
            }
        }
    }

    private void readLeaveBalanceData(String empNoToFilter) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) leaveBalanceTable.getModel();
        model.setRowCount(0);

        if (empNoToFilter == null || empNoToFilter.isEmpty()) {
            return;
        }

        List<LeaveRequest> balances = leaveRequestDAO.getLeaveBalancesByEmployeeNo(empNoToFilter);

        for (LeaveRequest balance : balances) {
            model.addRow(new Object[]{
                balance.getLeaveTypeName(),
                balance.getBalanceDays()
            });
        }
    }

    private Date normalizeDateToMidnight(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public void leaveProcessor() {
        // Elements for Request Leave (hide)
        jLabel5.setVisible(false);
        leaveReason.setVisible(false);
        requestedDate.setVisible(false);
        jLabel6.setVisible(false);
        leaveType.setVisible(false);
        submitRequest.setVisible(false);
        refreshButton1.setVisible(false);
        deleteRequest.setVisible(false);

        // Elements for Process Leave (show)
        processRequestedDate.setVisible(true);
        empNoField.setVisible(true);
        jLabel1.setVisible(true);
        jLabel2.setText("Date:");
        viewRequest.setVisible(true);
        refreshButton.setVisible(true);
        approveRequest.setVisible(true);
        rejectRequest.setVisible(true);
        jScrollPane1.setVisible(true);
        requestTable.setVisible(true);
    }

    public void leaveRequestor() {
        // Elements for Process Leave (hide)
        jLabel1.setVisible(false);
        empNoField.setVisible(false);
        processRequestedDate.setVisible(false);
        jLabel2.setVisible(false);
        viewRequest.setVisible(false);
        refreshButton.setVisible(false);
        approveRequest.setVisible(false);
        rejectRequest.setVisible(false);

        // Elements for Request Leave (show)
        jLabel5.setVisible(true);
        leaveReason.setVisible(true);
        requestedDate.setVisible(true);
        jLabel6.setVisible(true);
        leaveType.setVisible(true);
        submitRequest.setVisible(true);
        refreshButton1.setVisible(true);
        jScrollPane1.setVisible(true);
        requestTable.setVisible(true);
        deleteRequest.setVisible(true);
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
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        empNoField = new javax.swing.JTextField();
        viewRequest = new javax.swing.JButton();
        approveRequest = new javax.swing.JButton();
        rejectRequest = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        refreshButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        backButton = new javax.swing.JButton();
        leaveReason = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        submitRequest = new javax.swing.JButton();
        leaveType = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        refreshButton1 = new javax.swing.JButton();
        processRequestedDate = new com.toedter.calendar.JDateChooser();
        requestedDate = new com.toedter.calendar.JDateChooser();
        deleteRequest = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        requestTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        leaveBalanceTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("View Leave Request");
        setMinimumSize(new java.awt.Dimension(500, 500));

        jPanel1.setBackground(new java.awt.Color(0, 102, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(700, 100));
        jPanel1.setLayout(null);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Employee ID: " + currentUser.getEmployeeNo());
        jPanel1.add(jLabel3);
        jLabel3.setBounds(6, 78, 220, 16);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setText("jLabel4");
        jPanel1.add(jLabel4);
        jLabel4.setBounds(10, 10, 140, 16);

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorph.png"))); // NOI18N
        jPanel1.add(jLabel7);
        jLabel7.setBounds(0, -110, 2370, 280);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        viewRequest.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        viewRequest.setText("View");
        viewRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewRequestActionPerformed(evt);
            }
        });

        approveRequest.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        approveRequest.setText("Approve");
        approveRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveRequestActionPerformed(evt);
            }
        });

        rejectRequest.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        rejectRequest.setText("Reject");
        rejectRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rejectRequestActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel1.setText("Enter Employee No. :");

        refreshButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setText("Date:");

        backButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        backButton.setText("< Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("Leave Reason:");

        submitRequest.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        submitRequest.setText("Submit");
        submitRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitRequestActionPerformed(evt);
            }
        });

        leaveType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Vacation Leave", "Leave of Absence", "Mental Wellness Leave" }));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Type of Leave:");

        refreshButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        refreshButton1.setText("Refresh");
        refreshButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButton1ActionPerformed(evt);
            }
        });

        processRequestedDate.setDateFormatString("yyyy-MM-dd");
        processRequestedDate.setPreferredSize(new java.awt.Dimension(91, 22));

        requestedDate.setDateFormatString("yyyy-MM-dd");
        requestedDate.setPreferredSize(new java.awt.Dimension(91, 22));

        deleteRequest.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        deleteRequest.setText("Delete");
        deleteRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteRequestActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(approveRequest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(viewRequest)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(refreshButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel6))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(7, 7, 7)
                                        .addComponent(leaveType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(16, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(refreshButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(deleteRequest, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(submitRequest, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(rejectRequest, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(processRequestedDate, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                                    .addComponent(empNoField, javax.swing.GroupLayout.Alignment.LEADING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(leaveReason, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(requestedDate, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(9, 9, 9)
                                        .addComponent(jLabel5))))
                            .addComponent(backButton, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {approveRequest, refreshButton, rejectRequest, viewRequest});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(empNoField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(leaveReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(requestedDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(processRequestedDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(viewRequest)
                    .addComponent(refreshButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(approveRequest)
                    .addComponent(leaveType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rejectRequest)
                    .addComponent(submitRequest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(refreshButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deleteRequest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 92, Short.MAX_VALUE)
                .addComponent(backButton)
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {refreshButton, viewRequest});

        requestedDate.getAccessibleContext().setAccessibleName("");
        requestedDate.getAccessibleContext().setAccessibleDescription("");

        requestTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Employee #", "Last Name", "First Name", "Reason", "Type", "Requested Date", "Status", "Remarks", "Submission Date"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true, false, true, true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        requestTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(requestTable);

        jTabbedPane1.addTab("Requests", jScrollPane1);

        leaveBalanceTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Leave Type", "Balance Days"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        leaveBalanceTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(leaveBalanceTable);

        jTabbedPane1.addTab("Leave Balance", jScrollPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1296, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void refreshButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButton1ActionPerformed
        try {
            String currentEmpNo = currentUser.getEmployeeNo();
            readData(currentEmpNo, null);
            readLeaveBalanceData(currentEmpNo);
            leaveReason.setText("");
            requestedDate.setDate(null);
            leaveType.setSelectedIndex(0);
//            if (currentUser instanceof RegularEmployee) {
//                readData(currentEmpNo, null);
//            } else {
//                readData(null, null);
//            }
//            leaveReason.setText("");
//            requestedDate.setDate(null);
//            leaveType.setSelectedIndex(0);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error refreshing request panel for Regular Employee", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Refresh Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_refreshButton1ActionPerformed

    private void submitRequestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitRequestActionPerformed
        Connection conn = null;
        String empNoValue = currentUser.getEmployeeNo();
        try {
            conn = DBConnection.getTransactionalConnection();

            String empLeaveReason = leaveReason.getText().trim();
            Date selectedRequestedDate = requestedDate.getDate();

            if (selectedRequestedDate == null) {
                JOptionPane.showMessageDialog(this, "Please select a date for the leave request.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String empLeaveType = (leaveType.getSelectedItem() != null) ? leaveType.getSelectedItem().toString() : "N/A";
            String empLeaveStatus = "Pending";
            String empLeaveRemarks = "";

            if (empLeaveReason.isEmpty() || empLeaveType.equals("N/A")) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields (Leave Reason, Type of Leave).", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Employee currentEmpDetails = employeeDAO.getEmployeeByEmployeeNo(empNoValue);
            if (currentEmpDetails == null) {
                JOptionPane.showMessageDialog(this, "Employee details not found in database. Cannot submit request.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String empLNValue = currentEmpDetails.getEmployeeLN();
            String empFNValue = currentEmpDetails.getEmployeeFN();

            LeaveRequest newRequest = new LeaveRequest(
                    empNoValue, empLNValue, empFNValue, empLeaveReason, empLeaveType,
                    selectedRequestedDate,
                    empLeaveStatus, empLeaveRemarks
            );

            boolean success = leaveRequestDAO.submitLeaveRequest(conn, newRequest);

            if (success) {
                conn.commit();
                JOptionPane.showMessageDialog(this, "Leave request submitted successfully.", "Leave Request", JOptionPane.INFORMATION_MESSAGE);
                leaveReason.setText("");
                requestedDate.setDate(null);
                leaveType.setSelectedIndex(0);
                readData(empNoValue, null);
            } else {
                conn.rollback(); // Rollback if DAO method returns false
                JOptionPane.showMessageDialog(this, "Failed to submit leave request. Please check logs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            // Rollback on any SQL error
            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.log(Level.INFO, "Transaction rolled back due to SQL error during leave submission.");
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction during leave submission: " + rollbackEx.getMessage(), rollbackEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Database error submitting leave request for employee: " + empNoValue, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    LOGGER.log(Level.INFO, "Database connection closed after leave submission.");
                } catch (SQLException closeEx) {
                    LOGGER.log(Level.SEVERE, "Error closing connection after leave submission: " + closeEx.getMessage(), closeEx);
                }
            }
        }
    }//GEN-LAST:event_submitRequestActionPerformed

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        homePage.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_backButtonActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        try {
            readData(null, null);
            readLeaveBalanceData(null);
            empNoField.setText(null);
            processRequestedDate.setDate(null);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error refreshing view panel", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Refresh Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void rejectRequestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rejectRequestActionPerformed
        String targetEmpNo = empNoField.getText().trim();
        Date selectedRequestedDate = processRequestedDate.getDate();

        if (targetEmpNo.isEmpty() || selectedRequestedDate == null) {
            JOptionPane.showMessageDialog(this, "Please enter both Employee # and Date to reject a request.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {

            Date normalizedSelectedDate = normalizeDateToMidnight(selectedRequestedDate);

            List<LeaveRequest> empRequests = leaveRequestDAO.getLeaveRequestsByEmployeeNo(targetEmpNo);
            LeaveRequest foundRequest = null;
            for (LeaveRequest req : empRequests) {
                // Find the exact request by EmployeeID and RequestedDate
                Date reqNormalizedDate = (req.getRequestedDate() != null) ? normalizeDateToMidnight(req.getRequestedDate()) : null;
                if (reqNormalizedDate != null && reqNormalizedDate.equals(normalizedSelectedDate)) {
                    foundRequest = req;
                    break;
                }
            }

            if (foundRequest == null) {
                JOptionPane.showMessageDialog(this, "Request not found for this Employee # and Date!", "Search Request", JOptionPane.WARNING_MESSAGE);
            } else {
                Remarks remarks = new Remarks(targetEmpNo, normalizedSelectedDate, this);
                remarks.setVisible(true);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error checking request for rejection: " + targetEmpNo + " on date: " + selectedRequestedDate, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_rejectRequestActionPerformed

    private void approveRequestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveRequestActionPerformed
        Connection conn = null;
        String targetEmpNo = empNoField.getText().trim();
        Date selectedRequestedDate = processRequestedDate.getDate();

        try {
            conn = DBConnection.getTransactionalConnection();

            if (targetEmpNo.isEmpty() || selectedRequestedDate == null) {
                JOptionPane.showMessageDialog(this, "Please enter both Employee # and Date to approve a request.", "Input Error", JOptionPane.WARNING_MESSAGE);
                conn.rollback();
                return;
            }

            Date normalizedSelectedDate = normalizeDateToMidnight(selectedRequestedDate);

            List<LeaveRequest> empRequests = leaveRequestDAO.getLeaveRequestsByEmployeeNo(targetEmpNo);
            LeaveRequest foundRequest = null;
            for (LeaveRequest req : empRequests) {
                Date reqNormalizedDate = (req.getRequestedDate() != null) ? normalizeDateToMidnight(req.getRequestedDate()) : null;
                if (reqNormalizedDate != null && reqNormalizedDate.equals(normalizedSelectedDate)) {
                    foundRequest = req;
                    break;
                }
            }

            if (foundRequest == null) {
                JOptionPane.showMessageDialog(this, "Request not found for this Employee # and Date!", "Search Request", JOptionPane.WARNING_MESSAGE);
                conn.rollback();
                return;
            } else {
                String newStatus = "Approved";
                String newRemarks = "Approved by " + currentUser.getEmployeeNo();

                updateRequest(conn, targetEmpNo, normalizedSelectedDate, newStatus, newRemarks);

                conn.commit();
                JOptionPane.showMessageDialog(this, "Request approved and leave balance updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                readData(null, null);
            }
        } catch (SQLException ex) {

            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.log(Level.INFO, "Transaction rolled back due to SQL error during leave approval.");
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction during leave approval: " + rollbackEx.getMessage(), rollbackEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Database error approving request for employee: " + targetEmpNo + " on date: " + selectedRequestedDate, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    LOGGER.log(Level.INFO, "Database connection closed after leave approval.");
                } catch (SQLException closeEx) {
                    LOGGER.log(Level.SEVERE, "Error closing connection after leave approval: " + closeEx.getMessage(), closeEx);
                }
            }
        }
    }//GEN-LAST:event_approveRequestActionPerformed

    private void viewRequestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewRequestActionPerformed
        String targetEmpNo = empNoField.getText().trim();
        Date filterRequestedDate = processRequestedDate.getDate();
        Date normalizedFilterDate = normalizeDateToMidnight(filterRequestedDate);

        if (targetEmpNo.isEmpty() && normalizedFilterDate == null) {
            JOptionPane.showMessageDialog(this, "Please enter an Employee # or select a Date to search.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String empToSearch = targetEmpNo.isEmpty() ? null : targetEmpNo;
            readData(empToSearch, normalizedFilterDate);
            readLeaveBalanceData(empToSearch);
            if (requestTable.getModel().getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No requests found matching your search criteria!", "Search Request", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error during view request search", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_viewRequestActionPerformed

    private void deleteRequestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteRequestActionPerformed
        Date dateToDelete = requestedDate.getDate();

        if (dateToDelete == null) {
            JOptionPane.showMessageDialog(this, "Please select the date of the request you wish to delete.", "No Date Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String employeeId = currentUser.getEmployeeNo();
        Date normalizedDate = normalizeDateToMidnight(dateToDelete);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the leave request for " + new SimpleDateFormat("yyyy-MM-dd").format(normalizedDate) + "?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = leaveRequestDAO.deleteLeaveRequest(employeeId, normalizedDate);

                if (success) {
                    JOptionPane.showMessageDialog(this, "Leave request deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    readData(employeeId, null);
                    requestedDate.setDate(null);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete the request.\nIt might not exist or has already been processed.", "Deletion Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Database error during leave request deletion.", ex);
                JOptionPane.showMessageDialog(this, "A database error occurred. Please try again.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_deleteRequestActionPerformed
    public Employee getCurrentUser() { // Added getter for currentUser for Remarks.java
        return currentUser;
    }

    private void applyCustomStyles() {
        // Apply consistent button styling
        UtilMethods.styleButton(approveRequest);
        UtilMethods.styleButton(refreshButton);
        UtilMethods.styleButton(deleteRequest);
        UtilMethods.styleButton(refreshButton1);
        UtilMethods.styleButton(rejectRequest);
        UtilMethods.styleButton(submitRequest);
        UtilMethods.styleButton(viewRequest);

        // Style back button slightly differently if desired (e.g., less prominent)
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backButton.setBackground(new Color(100, 100, 100)); // Darker gray
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createRaisedBevelBorder());
        backButton.setPreferredSize(new Dimension(120, 30));
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton approveRequest;
    private javax.swing.JButton backButton;
    private javax.swing.JButton deleteRequest;
    private javax.swing.JTextField empNoField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable leaveBalanceTable;
    private javax.swing.JTextField leaveReason;
    private javax.swing.JComboBox<String> leaveType;
    private com.toedter.calendar.JDateChooser processRequestedDate;
    private javax.swing.JButton refreshButton;
    private javax.swing.JButton refreshButton1;
    private javax.swing.JButton rejectRequest;
    private javax.swing.JTable requestTable;
    private com.toedter.calendar.JDateChooser requestedDate;
    private javax.swing.JButton submitRequest;
    private javax.swing.JButton viewRequest;
    // End of variables declaration//GEN-END:variables
}
