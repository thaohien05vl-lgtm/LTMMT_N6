/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * LoginFrame:
 * Cửa sổ đăng nhập của GroupChatNetwork.
 *
 * Không trực tiếp đọc hoặc ghi Socket.
 * Mọi yêu cầu được chuyển qua AuthController.
 */
public class LoginFrame extends JFrame {

    private final AuthController controller;

    private JTextField usernameField;

    private JPasswordField passwordField;

    private JButton loginButton;

    private JButton registerButton;

    private JLabel statusLabel;

    public LoginFrame(AuthController controller) {

        this.controller = controller;

        setTitle("Group Chat - Đăng nhập");

        setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE
        );

        setSize(
                new Dimension(470, 650)
        );

        setResizable(false);

        setLocationRelativeTo(null);

        // Đóng ứng dụng sẽ đóng ClientConnection dùng chung.
        addWindowListener(new WindowCloseListener(controller));

        initComponents();

        AuthUI.addThemeListener(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                AuthUI.refreshContainer(this);
            });
        });
    }

    private static final class WindowCloseListener extends WindowAdapter {

        private final AuthController controller;

        WindowCloseListener(AuthController controller) {
            this.controller = controller;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            controller.shutdown();
        }
    }

    // ==========================================================
    // KHỞI TẠO GIAO DIỆN
    // ==========================================================

    private void initComponents() {

        JPanel root = AuthUI.createRootPanel();

        JPanel card = AuthUI.createCardPanel();

        card.setPreferredSize(
                new Dimension(390, 540)
        );

        card.setLayout(
                new javax.swing.BoxLayout(
                        card,
                        javax.swing.BoxLayout.Y_AXIS
                )
        );

        // Hàng nút chuyển đổi theme Sáng / Tối phía trên cùng
        JPanel topBar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);
        topBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        topBar.add(AuthUI.createThemeToggleButton(this));
        card.add(topBar);

        card.add(Box.createVerticalStrut(10));

        // Tiêu đề.
        JLabel brand = AuthUI.brand(
                "GROUP CHAT NETWORK"
        );

        JLabel title = AuthUI.title(
                "Chào mừng trở lại"
        );

        JLabel subtitle = AuthUI.subtitle(
                "Đăng nhập để kết nối cùng mọi người"
        );

        brand.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        title.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        subtitle.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        card.add(brand);

        card.add(Box.createVerticalStrut(12));

        card.add(title);

        card.add(Box.createVerticalStrut(8));

        card.add(subtitle);

        card.add(Box.createVerticalStrut(32));

        // Tài khoản.
        card.add(
                AuthUI.fieldLabel("Tên tài khoản")
        );

        card.add(Box.createVerticalStrut(8));

        usernameField = new JTextField();

        usernameField.setToolTipText(
                "Nhập username đã đăng ký"
        );

        AuthUI.styleInput(usernameField);

        card.add(usernameField);

        card.add(Box.createVerticalStrut(18));

        // Mật khẩu.
        card.add(
                AuthUI.fieldLabel("Mật khẩu")
        );

        card.add(Box.createVerticalStrut(8));

        passwordField = new JPasswordField();

        passwordField.setToolTipText(
                "Nhập mật khẩu tài khoản"
        );

        AuthUI.styleInput(passwordField);

        card.add(passwordField);

        card.add(Box.createVerticalStrut(18));

        // Vùng thông báo.
        statusLabel = AuthUI.createStatusLabel();

        AuthUI.setStatus(
                statusLabel,
                "Đang chuẩn bị kết nối đến Server...",
                AuthUI.MUTED_COLOR
        );

        card.add(statusLabel);

        card.add(Box.createVerticalStrut(12));

        // Nút đăng nhập.
        loginButton =
                AuthUI.createPrimaryButton(
                        "ĐĂNG NHẬP"
                );

        loginButton.addActionListener(e -> handleLogin());

        card.add(loginButton);

        card.add(Box.createVerticalStrut(12));

        // Nút chuyển sang đăng ký.
        registerButton =
                AuthUI.createSecondaryButton(
                        "Tạo tài khoản mới"
                );

        registerButton.addActionListener(e -> {
            controller.openRegister();
        });

        card.add(registerButton);

        card.add(Box.createVerticalStrut(20));

        JLabel footer = new AuthUI.DynamicLabel(
                "Kết nối • Trò chuyện • Chia sẻ",
                AuthUI.LabelKind.MUTED
        );

        footer.setFont(
                AuthUI.font(java.awt.Font.PLAIN, 12)
        );

        footer.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        footer.setAlignmentX(0.5f);

        card.add(footer);

        JLabel securityBadge = new AuthUI.DynamicLabel(
                "Bảo mật mã hóa SHA-256 (256-bit)",
                AuthUI.LabelKind.SECURITY
        );
        securityBadge.setFont(AuthUI.font(java.awt.Font.PLAIN, 11));
        securityBadge.setAlignmentX(0.5f);
        securityBadge.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(Box.createVerticalStrut(8));
        card.add(securityBadge);

        root.add(card);

        setContentPane(root);
    }

    // ==========================================================
    // XỬ LÝ NÚT ĐĂNG NHẬP
    // ==========================================================

    private void handleLogin() {

        String username =
                usernameField.getText().trim();

        char[] password =
                passwordField.getPassword();

        if (username.isEmpty()) {

            AuthUI.setStatus(
                    statusLabel,
                    "Vui lòng nhập tên tài khoản.",
                    AuthUI.ERROR_COLOR
            );

            java.util.Arrays.fill(password, '\0');

            return;
        }

        if (password.length == 0) {

            AuthUI.setStatus(
                    statusLabel,
                    "Vui lòng nhập mật khẩu.",
                    AuthUI.ERROR_COLOR
            );

            java.util.Arrays.fill(password, '\0');

            return;
        }

        /*
         * AuthController chịu trách nhiệm xử lý mật khẩu,
         * kiểm tra kết nối và gửi yêu cầu qua ClientConnection.
         */
        controller.login(
                username,
                password
        );

        // Xóa giá trị hiển thị sau khi gửi yêu cầu.
        passwordField.setText("");
    }

    // ==========================================================
    // PHẢN HỒI TỪ AUTHCONTROLLER
    // ==========================================================

    public void showStatus(
            String message,
            Boolean success) {

        Color color;

        if (success == null) {

            color = AuthUI.MUTED_COLOR;

        } else if (success) {

            color = AuthUI.SUCCESS_COLOR;

        } else {

            color = AuthUI.ERROR_COLOR;
        }

        AuthUI.setStatus(
                statusLabel,
                message,
                color
        );
    }

    public void setBusy(boolean busy) {

        usernameField.setEnabled(!busy);

        passwordField.setEnabled(!busy);

        loginButton.setEnabled(!busy);

        registerButton.setEnabled(!busy);

        if (busy) {

            loginButton.setText(
                    "ĐANG XỬ LÝ..."
            );

        } else {

            loginButton.setText(
                    "ĐĂNG NHẬP"
            );
        }
    }

    public void loginSucceeded(String serverMessage) {

        setBusy(false);

        usernameField.setEnabled(false);

        passwordField.setEnabled(false);

        loginButton.setEnabled(false);

        registerButton.setEnabled(false);

        showStatus(
                serverMessage
                + "\nXác thực thành công. "
                + "Màn hình chat sẽ được tích hợp ở Checkpoint 4.4.",
                true
        );
    }
    
    // ==========================================================
// ĐẶT LẠI MÀN HÌNH SAU KHI ĐĂNG XUẤT
// ==========================================================

public void resetAfterLogout() {

    usernameField.setText("");
    passwordField.setText("");

    usernameField.setEnabled(true);
    passwordField.setEnabled(true);

    loginButton.setEnabled(true);
    registerButton.setEnabled(true);

    loginButton.setText("ĐĂNG NHẬP");

    showStatus(
            "Đã đăng xuất. Đang kết nối lại Server...",
            null
    );
}
}
