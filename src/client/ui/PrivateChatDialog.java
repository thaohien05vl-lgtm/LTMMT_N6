/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import common.Protocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * PrivateChatDialog:
 *
 * Cửa sổ trò chuyện riêng tư 1-1 giữa 2 người dùng.
 * - Giao diện Dark theme đồng bộ hoàn toàn với AuthUI và ChatFrame.
 * - Hỗ trợ gửi tin nhắn riêng qua giao thức Socket.
 * - Tự động tải và đồng bộ lịch sử tin nhắn từ cơ sở dữ liệu SQLite.
 * - Hiển thị bubble bo tròn hiện đại (Tin của mình căn phải, bạn bè căn trái).
 */
public class PrivateChatDialog extends JFrame {

    private final AuthController controller;
    private final String myUsername;
    private final String myDisplayName;
    private final String targetUsername;
    private final String targetDisplayName;

    private JPanel messagesBox;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel charCountLabel;
    private JLabel statusLabel;

    private boolean historyRequested = false;
    private final java.util.Set<String> seenMessageKeys = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public PrivateChatDialog(
            JFrame parent,
            AuthController controller,
            String myUsername,
            String myDisplayName,
            String targetUsername,
            String targetDisplayName) {

        super("Trò chuyện riêng - " + (targetDisplayName != null ? targetDisplayName : targetUsername));

        this.controller = controller;
        this.myUsername = myUsername;
        this.myDisplayName = myDisplayName != null ? myDisplayName : myUsername;
        this.targetUsername = targetUsername;
        this.targetDisplayName = targetDisplayName != null ? targetDisplayName : targetUsername;

        initUI(parent);
        loadHistory();
    }

    private void initUI(JFrame parent) {
        setSize(500, 640);
        setMinimumSize(new Dimension(420, 500));
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        if (parent != null) {
            setLocationRelativeTo(parent);
            // Dịch nhẹ một chút để không che hoàn toàn ChatFrame
            setLocation(getX() + 40, getY() + 30);
        } else {
            setLocationRelativeTo(null);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AuthUI.BACKGROUND_TOP);

        // ======================================================
        // 1. HEADER (TIÊU ĐỀ & THÔNG TIN ĐỐI PHƯƠNG)
        // ======================================================
        JPanel header = createHeaderPanel();
        root.add(header, BorderLayout.NORTH);

        // ======================================================
        // 2. DANH SÁCH BUBBLE TIN NHẮN
        // ======================================================
        messagesBox = new JPanel();
        messagesBox.setLayout(new BoxLayout(messagesBox, BoxLayout.Y_AXIS));
        messagesBox.setBackground(AuthUI.CARD_COLOR);
        messagesBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AuthUI.CARD_COLOR);
        wrapper.add(messagesBox, BorderLayout.NORTH);

        scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(AuthUI.CARD_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AuthUI.styleScrollPane(scrollPane);

        root.add(scrollPane, BorderLayout.CENTER);

        // ======================================================
        // 3. KHU VỰC SOẠN & GỬI TIN NHẮN (BOTTOM)
        // ======================================================
        JPanel bottom = createBottomPanel();
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        AuthUI.addThemeListener(() -> {
            SwingUtilities.invokeLater(() -> {
                root.setBackground(AuthUI.BACKGROUND_TOP);
                messagesBox.setBackground(AuthUI.CARD_COLOR);
                wrapper.setBackground(AuthUI.CARD_COLOR);
                scrollPane.getViewport().setBackground(AuthUI.CARD_COLOR);
                bottom.setBackground(AuthUI.CARD_COLOR);
                header.setBackground(AuthUI.BACKGROUND_BOTTOM);
                AuthUI.refreshContainer(this);
            });
        });

        // Lắng nghe đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(AuthUI.BACKGROUND_BOTTOM);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AuthUI.BORDER_COLOR),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Avatar tròn chứa chữ cái đầu
        AvatarCircle avatar = new AvatarCircle(targetDisplayName);
        header.add(avatar, BorderLayout.WEST);

        // Tên và username
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(targetDisplayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(AuthUI.TEXT_COLOR);

        JLabel subLabel = new JLabel("@" + targetUsername + " • Trò chuyện riêng tư 1-1");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(AuthUI.MUTED_COLOR);

        statusLabel = new JLabel("Lưu trữ SQLite an toàn");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(AuthUI.ACCENT_LIGHT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(subLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(statusLabel);

        header.add(infoPanel, BorderLayout.CENTER);

        return header;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(8, 6));
        bottom.setBackground(AuthUI.CARD_COLOR);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AuthUI.BORDER_COLOR),
                new EmptyBorder(10, 14, 10, 14)
        ));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputField.setForeground(AuthUI.TEXT_COLOR);
        inputField.setCaretColor(AuthUI.TEXT_COLOR);
        inputField.setBackground(AuthUI.INPUT_COLOR);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AuthUI.BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        sendButton = AuthUI.createButton("Gửi", true, 80, 36);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Bấm nút Gửi
        sendButton.addActionListener(e -> sendMessage());

        // Nhấn phím Enter
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        // Đếm ký tự
        charCountLabel = new JLabel("0/500");
        charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        charCountLabel.setForeground(AuthUI.MUTED_COLOR);
        charCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCount();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCount();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCount();
            }

            private void updateCount() {
                int len = inputField.getText().length();
                charCountLabel.setText(len + "/500");
                if (len > 500) {
                    charCountLabel.setForeground(Color.RED);
                    sendButton.setEnabled(false);
                } else {
                    charCountLabel.setForeground(AuthUI.MUTED_COLOR);
                    sendButton.setEnabled(true);
                }
            }
        });

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);

        bottom.add(inputRow, BorderLayout.CENTER);
        bottom.add(charCountLabel, BorderLayout.SOUTH);

        return bottom;
    }

    /**
     * Yêu cầu Server nạp lịch sử tin nhắn riêng tư giữa hai người.
     */
    public void loadHistory() {
        if (!historyRequested && controller != null) {
            historyRequested = true;
            controller.sendProtocol(
                    Protocol.build(Protocol.GET_PRIVATE_HISTORY, targetUsername)
            );
        }
    }

    /**
     * Gửi tin nhắn riêng tới Server.
     */
    private void sendMessage() {
        String text = inputField.getText();
        if (text == null) {
            return;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return;
        }

        if (text.length() > 500) {
            return;
        }

        if (controller != null) {
            controller.sendProtocol(
                    Protocol.build(Protocol.SEND_PRIVATE, targetUsername, text)
            );
            inputField.setText("");
            inputField.requestFocusInWindow();
        }
    }

    /**
     * Thêm một tin nhắn vào giao diện.
     */
    public void appendMessage(
            String senderUsername,
            String senderDisplayName,
            String sentAt,
            String content,
            boolean isMine) {

        // Khóa duy nhất chống trùng lặp tin nhắn giữa realtime và history
        // Chuẩn hóa thời gian về mức phút để tránh lệch giữa định dạng DB SQLite và realtime
        String normalizedTime = BubblePanel.formatTime(sentAt);
        String msgKey = (senderUsername == null ? "" : senderUsername.toLowerCase())
                + "|" + normalizedTime
                + "|" + (content == null ? "" : content.trim());

        if (!seenMessageKeys.add(msgKey)) {
            // Tin nhắn đã tồn tại trên giao diện, bỏ qua để không lặp 2 lần
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4, 0, 4, 0));

            BubblePanel bubble = new BubblePanel(
                    content,
                    sentAt,
                    isMine,
                    isMine ? myDisplayName : senderDisplayName
            );

            if (isMine) {
                row.add(bubble, BorderLayout.EAST);
            } else {
                row.add(bubble, BorderLayout.WEST);
            }

            messagesBox.add(row);
            messagesBox.revalidate();
            messagesBox.repaint();

            scrollToBottom();
        });
    }

    /**
     * Báo hiệu khi lịch sử đã được nạp xong.
     */
    public void onHistoryLoaded(int count) {
        SwingUtilities.invokeLater(() -> {
            if (count == 0 && messagesBox.getComponentCount() == 0) {
                statusLabel.setText("Chưa có tin nhắn nào. Hãy gửi lời chào!");
            } else {
                statusLabel.setText("Đã nạp " + count + " tin nhắn từ SQLite");
            }
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vbar = scrollPane.getVerticalScrollBar();
            if (vbar != null) {
                vbar.setValue(vbar.getMaximum());
            }
        });
    }

    // ==========================================================
    // INNER CLASSES: BUBBLE & AVATAR
    // ==========================================================

    /**
     * Bong bóng tin nhắn với màu sắc và bo góc hiện đại.
     */
    private static class BubblePanel extends JPanel {
        private final boolean isMine;

        public BubblePanel(String content, String sentAt, boolean isMine, String senderName) {
            this.isMine = isMine;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));

            // Tên người gửi nếu không phải mình
            if (!isMine) {
                JLabel nameLabel = new JLabel(senderName != null ? senderName : "");
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
                nameLabel.setForeground(AuthUI.ACCENT_LIGHT);
                add(nameLabel);
                add(Box.createVerticalStrut(3));
            }

            // Nội dung tin nhắn
            // Dùng JLabel HTML để tự động ngắt dòng nếu dài
            String safeContent = content
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br/>");

            JLabel contentLabel = new JLabel("<html><body style='width: 240px;'>" + safeContent + "</body></html>");
            contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            contentLabel.setForeground(isMine ? Color.WHITE : AuthUI.TEXT_COLOR);
            add(contentLabel);

            // Thời gian gửi
            String timeText = formatTime(sentAt);
            JLabel timeLabel = new JLabel(timeText);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(isMine ? new Color(220, 240, 255) : AuthUI.MUTED_COLOR);
            timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            add(Box.createVerticalStrut(4));
            add(timeLabel);
        }

        private static String formatTime(String raw) {
            if (raw == null || raw.isBlank()) {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            }
            String trimmed = raw.trim();

            if (trimmed.length() == 5 && trimmed.charAt(2) == ':') {
                return trimmed;
            }

            try {
                String isoString = trimmed.replace(" ", "T");
                if (!isoString.endsWith("Z") && !isoString.contains("+") && isoString.length() >= 19) {
                    isoString += "Z";
                }
                Instant instant = Instant.parse(isoString);
                return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception ignored) {
            }

            if (trimmed.length() >= 16) {
                return trimmed.substring(11, 16);
            }

            return trimmed;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isMine) {
                // Tin của mình: gradient neon tím - hồng rực rỡ
                g2.setPaint(new java.awt.GradientPaint(
                        0, 0, AuthUI.ACCENT_COLOR,
                        w, h, AuthUI.ACCENT_PINK
                ));
                g2.fillRoundRect(0, 0, w, h, 18, 18);
                // Vệt phản quang kính đỉnh bubble
                g2.setPaint(new java.awt.GradientPaint(0, 1, new Color(255, 255, 255, 80), w, 1, new Color(255, 255, 255, 10)));
                g2.drawLine(6, 1, w - 6, 1);
            } else {
                // Tin đối phương: xám nhẹ hoặc dark bubble
                g2.setColor(AuthUI.BUBBLE_OTHER_COLOR);
                g2.fillRoundRect(0, 0, w, h, 18, 18);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Vòng tròn đại diện người dùng chứa chữ cái đầu.
     */
    private static class AvatarCircle extends JPanel {
        private final String letter;

        public AvatarCircle(String name) {
            setPreferredSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setOpaque(false);

            if (name != null && !name.isBlank()) {
                this.letter = name.trim().substring(0, 1).toUpperCase();
            } else {
                this.letter = "U";
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g2.setPaint(new java.awt.GradientPaint(
                    x, y, AuthUI.ACCENT_COLOR,
                    x + size, y + size, AuthUI.ACCENT_ALT
            ));
            g2.fillOval(x, y, size, size);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tx = x + (size - fm.stringWidth(letter)) / 2;
            int ty = y + ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(letter, tx, ty);

            g2.dispose();
        }
    }
}
