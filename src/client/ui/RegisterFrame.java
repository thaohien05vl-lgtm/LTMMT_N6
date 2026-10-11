/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

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
 * RegisterFrame:
 * Cửa sổ tạo tài khoản mới.
 *
 * Việc gửi REGISTER được thực hiện thông qua AuthController.
 */
public class RegisterFrame extends JFrame {

    private final AuthController controller;

    private JTextField usernameField;

    private JTextField displayNameField;

    private JPasswordField passwordField;

    private JPasswordField confirmPasswordField;

    private JButton registerButton;

    private JButton backButton;

    private JLabel statusLabel;

    public RegisterFrame(AuthController controller) {

        this.controller = controller;

        setTitle("Group Chat - Đăng ký");

        setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE
        );

        setSize(
                new Dimension(500, 760)
        );

        setResizable(false);

        setLocationRelativeTo(null);

        // Đóng cửa sổ đăng ký thì quay về đăng nhập.
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                controller.returnToLogin();
            }
        });

        initComponents();
    }

    // ==========================================================
    // KHỞI TẠO GIAO DIỆN
    // ==========================================================

    private void initComponents() {

        JPanel root = AuthUI.createRootPanel();

        JPanel card = AuthUI.createCardPanel();

        card.setPreferredSize(
                new Dimension(420, 680)
        );

        card.setLayout(
                new javax.swing.BoxLayout(
                        card,
                        javax.swing.BoxLayout.Y_AXIS
                )
        );

        JLabel brand = AuthUI.subtitle(
                "GROUP CHAT NETWORK"
        );

        brand.setForeground(
                AuthUI.ACCENT_COLOR
        );

        JLabel title = AuthUI.title(
                "Tạo tài khoản"
        );

        JLabel subtitle = AuthUI.subtitle(
                "Tham gia cuộc trò chuyện ngay hôm nay"
        );

        title.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        subtitle.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        card.add(brand);

        card.add(Box.createVerticalStrut(10));

        card.add(title);

        card.add(Box.createVerticalStrut(8));

        card.add(subtitle);

        card.add(Box.createVerticalStrut(22));

        // Username.
        card.add(
                AuthUI.fieldLabel("Tên tài khoản")
        );

        card.add(Box.createVerticalStrut(6));

        usernameField = new JTextField();

        usernameField.setToolTipText(
                "3–20 ký tự, gồm chữ, số hoặc dấu gạch dưới"
        );

        AuthUI.styleInput(usernameField);

        card.add(usernameField);

        card.add(Box.createVerticalStrut(12));

        // Display name.
        card.add(
                AuthUI.fieldLabel("Tên hiển thị")
        );

        card.add(Box.createVerticalStrut(6));

        displayNameField = new JTextField();

        displayNameField.setToolTipText(
                "Tên sẽ xuất hiện trong phòng chat"
        );

        AuthUI.styleInput(displayNameField);

        card.add(displayNameField);

        card.add(Box.createVerticalStrut(12));

        // Password.
        card.add(
                AuthUI.fieldLabel("Mật khẩu")
        );

        card.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField();

        AuthUI.styleInput(passwordField);

        card.add(passwordField);

        card.add(Box.createVerticalStrut(12));

        // Confirm password.
        card.add(
                AuthUI.fieldLabel("Xác nhận mật khẩu")
        );

        card.add(Box.createVerticalStrut(6));

        confirmPasswordField =
                new JPasswordField();

        AuthUI.styleInput(
                confirmPasswordField
        );

        card.add(confirmPasswordField);

        card.add(Box.createVerticalStrut(14));

        // Thông báo trạng thái.
        statusLabel = AuthUI.createStatusLabel();

        AuthUI.setStatus(
                statusLabel,
                "Mật khẩu cần có từ 8 đến 128 ký tự.",
                AuthUI.MUTED_COLOR
        );

        card.add(statusLabel);

        card.add(Box.createVerticalStrut(12));

        // Nút đăng ký.
        registerButton =
                AuthUI.createPrimaryButton(
                        "TẠO TÀI KHOẢN"
                );

        registerButton.addActionListener(e -> {
            handleRegister();
        });

        card.add(registerButton);

        card.add(Box.createVerticalStrut(10));

        // Nút quay về đăng nhập.
        backButton =
                AuthUI.createSecondaryButton(
                        "Quay lại đăng nhập"
                );

        backButton.addActionListener(e -> {
            controller.returnToLogin();
        });

        card.add(backButton);

        root.add(card);

        setContentPane(root);
    }

    // ==========================================================
    // XỬ LÝ ĐĂNG KÝ
    // ==========================================================

    private void handleRegister() {

        String username =
                usernameField.getText().trim();

        String displayName =
                displayNameField.getText().trim();

        char[] password =
                passwordField.getPassword();

        char[] confirm =
                confirmPasswordField.getPassword();

        if (username.isEmpty()
                || displayName.isEmpty()
                || password.length == 0
                || confirm.length == 0) {

            showStatus(
                    "Vui lòng nhập đầy đủ thông tin.",
                    false
            );

            Arrays.fill(password, '\0');

            Arrays.fill(confirm, '\0');

            return;
        }

        // So sánh hai mảng ký tự thay vì chuyển thành String.
        if (!Arrays.equals(password, confirm)) {

            showStatus(
                    "Mật khẩu xác nhận không trùng khớp.",
                    false
            );

            Arrays.fill(password, '\0');

            Arrays.fill(confirm, '\0');

            passwordField.setText("");

            confirmPasswordField.setText("");

            return;
        }

        /*
         * Controller sẽ kiểm tra username/password,
         * chuyển password thành Base64 và gửi REGISTER.
         */
        controller.register(
                username,
                displayName,
                password
        );

        // Controller đã xử lý bản sao char[] được cung cấp.
        Arrays.fill(confirm, '\0');

        passwordField.setText("");

        confirmPasswordField.setText("");
    }

    // ==========================================================
    // PHẢN HỒI TỪ AUTHCONTROLLER
    // ==========================================================

    public void showStatus(
            String message,
            Boolean success) {

        java.awt.Color color;

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

    public void showResult(
            String serverMessage,
            boolean success) {

        setBusy(false);

        showStatus(
                serverMessage,
                success
        );

        if (success) {

            /*
             * Đã tạo tài khoản thành công.
             * Người dùng quay lại LoginFrame để đăng nhập.
             */
            registerButton.setEnabled(false);

        } else {

            registerButton.setEnabled(true);
        }
    }

    public void setBusy(boolean busy) {

        usernameField.setEnabled(!busy);

        displayNameField.setEnabled(!busy);

        passwordField.setEnabled(!busy);

        confirmPasswordField.setEnabled(!busy);

        registerButton.setEnabled(!busy);

        backButton.setEnabled(!busy);

        if (busy) {

            registerButton.setText(
                    "ĐANG XỬ LÝ..."
            );

        } else {

            registerButton.setText(
                    "TẠO TÀI KHOẢN"
            );
        }
    }
}
