import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BankApp extends JFrame {
    private JTextField tfName, tfAmount;
    private JLabel lblBalance, lblStatus;
    private JButton btnCreate, btnDeposit, btnWithdraw, btnViewAll, btnClear;
    private String currentAccount = null;

   
    private final String DB_URL = "jdbc:mysql://localhost:3306/bankdb";
    private final String DB_USER = "root";
    private final String DB_PASS = "###";

    public BankApp() {
        setTitle("🏦 Smart Bank Account Management");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 248, 255));

        createTableIfNotExists(); // ✅ Auto DB + Table setup
        initUI();
    }

    private void initUI() {
        // 🌈 Header with gradient background
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(0, 0, new Color(72, 61, 139),
                        getWidth(), getHeight(), new Color(123, 104, 238));
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(600, 70));
        header.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Smart Bank Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        // 🧾 Main Panel
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(250, 250, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblName = new JLabel("Account Holder Name:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tfName = new JTextField(20);
        tfName.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tfName.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        JLabel lblAmount = new JLabel("Amount:");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tfAmount = new JTextField(20);
        tfAmount.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tfAmount.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        lblBalance = new JLabel("Balance: ₹0.00");
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBalance.setForeground(new Color(25, 25, 112));

        lblStatus = new JLabel("No account selected");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblStatus.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(lblName, gbc);
        gbc.gridx = 1; panel.add(tfName, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(lblAmount, gbc);
        gbc.gridx = 1; panel.add(tfAmount, gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(lblBalance, gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(lblStatus, gbc);

        // 🎨 Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 12, 12));
        buttonPanel.setBackground(new Color(245, 248, 255));

        btnCreate = new JButton("🆕 Create Account");
        btnDeposit = new JButton("💰 Deposit");
        btnWithdraw = new JButton("💸 Withdraw");
        btnViewAll = new JButton("📋 View Accounts");
        btnClear = new JButton("❌ Clear");

        styleButton(btnCreate, new Color(72, 118, 255));
        styleButton(btnDeposit, new Color(60, 179, 113));
        styleButton(btnWithdraw, new Color(220, 20, 60));
        styleButton(btnViewAll, new Color(255, 140, 0));
        styleButton(btnClear, new Color(105, 105, 105));

        buttonPanel.add(btnCreate);
        buttonPanel.add(btnDeposit);
        buttonPanel.add(btnWithdraw);
        buttonPanel.add(btnViewAll);
        buttonPanel.add(btnClear);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        add(panel, BorderLayout.CENTER);

        // 🎯 Button Actions
        btnCreate.addActionListener(e -> createAccount());
        btnDeposit.addActionListener(e -> deposit());
        btnWithdraw.addActionListener(e -> withdraw());
        btnViewAll.addActionListener(e -> viewAllAccounts());
        btnClear.addActionListener(e -> clearFields());
    }

    private void styleButton(JButton btn, Color color) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover animation
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(color);
            }
        });
    }

    private void createAccount() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter account holder name!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Account already exists!");
                currentAccount = name;
                updateBalanceLabel();
            } else {
                PreparedStatement pst = conn.prepareStatement("INSERT INTO accounts(name, balance) VALUES(?, ?)");
                pst.setString(1, name);
                pst.setDouble(2, 0.0);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Account created successfully!");
                currentAccount = name;
                lblBalance.setText("Balance: ₹0.00");
                lblStatus.setText("Active Account: " + name);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }

    private void deposit() {
        if (currentAccount == null) {
            JOptionPane.showMessageDialog(this, "Create or select an account first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            double amount = Double.parseDouble(tfAmount.getText());
            if (amount <= 0) throw new NumberFormatException();

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement pst = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE name = ?");
                pst.setDouble(1, amount);
                pst.setString(2, currentAccount);
                pst.executeUpdate();
            }
            lblStatus.setText("Deposited ₹" + amount + " to " + currentAccount);
            updateBalanceLabel();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }

    private void withdraw() {
        if (currentAccount == null) {
            JOptionPane.showMessageDialog(this, "Create or select an account first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            double amount = Double.parseDouble(tfAmount.getText());
            if (amount <= 0) throw new NumberFormatException();

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement check = conn.prepareStatement("SELECT balance FROM accounts WHERE name = ?");
                check.setString(1, currentAccount);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    if (amount > balance) {
                        JOptionPane.showMessageDialog(this, "Insufficient balance!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    PreparedStatement pst = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE name = ?");
                    pst.setDouble(1, amount);
                    pst.setString(2, currentAccount);
                    pst.executeUpdate();
                    lblStatus.setText("Withdrew ₹" + amount + " from " + currentAccount);
                    updateBalanceLabel();
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }

    private void updateBalanceLabel() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement pst = conn.prepareStatement("SELECT balance FROM accounts WHERE name = ?");
            pst.setString(1, currentAccount);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                lblBalance.setText("Balance: ₹" + rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating balance: " + e.getMessage());
        }
    }

    private void viewAllAccounts() {
        StringBuilder sb = new StringBuilder("💼 All Accounts:\n\n");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM accounts")) {

            while (rs.next()) {
                sb.append("👤 Name: ").append(rs.getString("name"))
                        .append("\n💰 Balance: ₹").append(rs.getDouble("balance"))
                        .append("\n📅 Created: ").append(rs.getTimestamp("created_at"))
                        .append("\n--------------------------\n");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error reading accounts: " + e.getMessage());
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "Accounts Overview", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearFields() {
        tfName.setText("");
        tfAmount.setText("");
        lblBalance.setText("Balance: ₹0.00");
        lblStatus.setText("No account selected");
        currentAccount = null;
    }

    // ✅ Fixed version that auto-creates the DB and table
    private void createTableIfNotExists() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Step 1: Create DB if not exists
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false", DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS bankdb");
                System.out.println("✅ Database 'bankdb' ready.");
            }

            // Step 2: Create table if not exists
            try (Connection conn = DriverManager.getConnection(DB_URL + "?useSSL=false", DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) UNIQUE,
                        balance DOUBLE DEFAULT 0.0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                System.out.println("✅ Table 'accounts' ready.");
            }

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "❌ MySQL JDBC Driver not found! Add mysql-connector-j.jar.", "Driver Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "❌ SQL Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankApp().setVisible(true));
    }
}
