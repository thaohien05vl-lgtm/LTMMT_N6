/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client.ui;

import common.Protocol;
import model.RoomRole;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * ChatFrame:
 * Giao diện chat chính của GroupChatNetwork.
 *
 * Lưu ý:
 * - Không tạo Socket mới.
 * - Không tạo BufferedReader hoặc PrintWriter.
 * - Không tự tạo Reader Thread.
 * - Gửi và nhận dữ liệu thông qua AuthController.
 *
 * Giao diện mới:
 * - Tin nhắn hiển thị dạng bong bóng chat (của mình bên phải, gradient tím).
 * - Avatar chữ cái đầu, màu cố định theo từng người dùng.
 * - Danh sách phòng / thành viên dạng thẻ bo góc, có hover.
 * - Ô soạn tin nhắn bo tròn, nút gửi tròn, bộ đếm ký tự.
 * - Tab Thành viên / Tìm kiếm dạng segmented control.
 *
 * Logic giao thức (Protocol) giữ nguyên như bản cũ.
 */
public class ChatFrame extends JFrame {

    private static final int MAX_MESSAGE_LENGTH = 500;

    // Giới hạn số dòng tin nhắn giữ trên giao diện để tránh nặng bộ nhớ.
    private static final int MAX_ROWS = 600;

    private final AuthController controller;
    private final String username;

    // Tên hiển thị của chính mình. Tự nhận ra khi có CHAT do mình gửi,
    // hoặc có thể gọi setDisplayName(...) sau khi đăng nhập.
    private String myDisplayName;

    // Phòng mà người dùng đang xem.
    private String currentRoom = "Sảnh chính";

    // Ngăn sự kiện chọn phòng chạy khi đang cập nhật danh sách.
    private boolean updatingRoomList = false;

    // ==========================================================
    // CÁC THÀNH PHẦN GIAO DIỆN
    // ==========================================================

    private DefaultListModel<String> roomModel;
    private DefaultListModel<String> userModel;

    private JList<String> roomList;
    private JList<String> userList;

    private MessageList chatList;
    private MessageList searchList;

    private JScrollPane chatScroll;
    private JScrollPane searchScroll;

    private HintTextField messageField;
    private HintTextField searchField;
    private JLabel brandLabel;
    private JLabel taglineLabel;

    private JButton sendButton;
    private JButton searchButton;
    private JButton logoutButton;

    // Nút tạo phòng mới.
    private JButton createRoomButton;

    // Nút vào phòng.
    private JButton joinRoomButton;

    // Nút bổ nhiệm thành viên thành Admin.
    private JButton promoteAdminButton;

    // Nút Kick thành viên ra khỏi phòng (dành cho Admin / Owner).
    private JButton kickButton;

    // Nút Xóa phòng (chỉ dành cho Owner tạo phòng).
    private JButton deleteRoomButton;

    // Nút Tải lại lịch sử phòng
    private JButton reloadHistoryButton;

    // Menu gợi ý nhắc tên @ (Autocomplete mention)
    private JPopupMenu mentionPopup;

    // Vai trò nhận từ Server trong phòng hiện tại.
    // Mặc định MEMBER cho đến khi Server trả lời.
    private RoomRole currentRole = RoomRole.MEMBER;

    private JLabel roomTitle;
    private JLabel connectionStatus;
    private JLabel memberBadge;
    private JLabel roomCountLabel;
    private JLabel charCounter;

    // ==========================================================
    // REACTION — MAP messageId → MessageRow để cập nhật reaction
    // ==========================================================

    /** messageId (từ server) → MessageRow tương ứng trên giao diện. */
    private final Map<Integer, MessageRow> messageRowMap = new HashMap<>();

    /** Lưu tin nhắn gần nhất do chính mình gửi để hiển thị avatar 'đã xem' như Messenger */
    private MessageRow lastSentMineMessageRow = null;

    /** messageId → chuỗi reaction dạng "❤️:2,👍:1" (luôn mới nhất). */
    private final Map<Integer, String> reactionPayloadMap = new HashMap<>();

    /** Quản lý các cửa sổ trò chuyện riêng 1-1 (key = username viết thường). */
    private final Map<String, PrivateChatDialog> privateChatWindows = new java.util.concurrent.ConcurrentHashMap<>();

    // ==========================================================
    // REPLY — TRẢ LỜI TIN NHẮN CỤ THỂ
    // ==========================================================

    private int replyTargetMessageId = 0;
    private String replyTargetSender = null;
    private String replyTargetSnippet = null;
    private JPanel replyPreviewPanel;
    private JLabel replySenderLabel;
    private JLabel replySnippetLabel;
    private final Map<Integer, ReplyInfo> replyInfoMap = new HashMap<>();

    // ==========================================================
    // UNREAD MESSAGES — THÔNG BÁO TIN NHẮN CHƯA ĐỌC THEO PHÒNG
    // ==========================================================
    private final Map<String, Integer> unreadRoomCounts = new HashMap<>();

    // ==========================================================
    // TYPING INDICATOR — ĐANG SOẠN TIN NHẮN GIỐNG MESSENGER
    // ==========================================================
    /** Tập hợp các người dùng (displayName) đang gõ tin nhắn trong phòng hiện tại. */
    private final Set<String> typingUsers = Collections.synchronizedSet(new LinkedHashSet<>());
    private javax.swing.Timer typingDebounceTimer;
    private javax.swing.Timer typingAnimTimer;
    private boolean isSelfTyping = false;
    private float typingAnimPhase = 0.0f;
    private JPanel typingIndicatorPanel;
    private JLabel typingStatusLabel;
    private JComponent typingDotsCanvas;

    static final class ReplyInfo {
        final int replyToId;
        final String replySender;
        final String replySnippet;

        ReplyInfo(int replyToId, String replySender, String replySnippet) {
            this.replyToId = replyToId;
            this.replySender = replySender == null ? "Người dùng" : replySender;
            this.replySnippet = replySnippet == null ? "" : replySnippet;
        }
    }

    private JButton privateChatButton;

    private SegmentButton tabMembers;
    private SegmentButton tabSearch;

    private CardLayout rightLayout;
    private JPanel rightCards;

    // ==========================================================
    // CONSTRUCTOR
    // ==========================================================

    public ChatFrame(
            AuthController controller,
            String username) {

        this.controller = controller;
        this.username = username;

        setTitle("Group Chat Network");
        setSize(1240, 800);
        setMinimumSize(new Dimension(1040, 680));
        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE
        );

        // Đóng cửa sổ thì đóng ứng dụng một cách có kiểm soát.
        addWindowListener(new WindowCloseListener(controller, privateChatWindows));

        initComponents();

        AuthUI.addThemeListener(() -> {
            SwingUtilities.invokeLater(() -> {
                if (brandLabel != null) {
                    brandLabel.setText(AuthUI.getCurrentTheme().getTitle());
                }
                if (taglineLabel != null) {
                    taglineLabel.setText(AuthUI.getCurrentTheme().getSubtitle());
                }
                setTitle(AuthUI.getCurrentTheme().getTitle() + " - " + username);
                AuthUI.refreshContainer(this);
                if (chatList != null) {
                    chatList.revalidate();
                    chatList.repaint();
                }
                if (searchList != null) {
                    searchList.revalidate();
                    searchList.repaint();
                }
            });
        });
    }

    private static final class WindowCloseListener extends WindowAdapter {

        private final AuthController controller;
        private final Map<String, PrivateChatDialog> privateChatWindows;

        WindowCloseListener(AuthController controller, Map<String, PrivateChatDialog> privateChatWindows) {
            this.controller = controller;
            this.privateChatWindows = privateChatWindows;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            if (privateChatWindows != null) {
                for (PrivateChatDialog d : privateChatWindows.values()) {
                    if (d != null) {
                        d.dispose();
                    }
                }
                privateChatWindows.clear();
            }
            controller.shutdown();
        }
    }

    /**
     * Tùy chọn: cho phép controller báo tên hiển thị của người dùng
     * để nhận ra tin nhắn của mình trong lịch sử (HISTORY).
     */
    public void setDisplayName(String displayName) {
        this.myDisplayName = displayName;
    }

    // ==========================================================
    // KHỞI TẠO TOÀN BỘ GIAO DIỆN
    // ==========================================================

    private void initComponents() {

        JPanel root = AuthUI.createRootPanel();

        root.setLayout(new BorderLayout(4, 4));

        // Các card đã có vùng bóng đổ 6px nên đệm ngoài chỉ cần 10px.
        root.setBorder(
                new EmptyBorder(10, 10, 10, 10)
        );

        // Phần đầu trang.
        root.add(createHeader(), BorderLayout.NORTH);

        // Phần nội dung chính gồm 3 khu vực.
        JPanel body = new JPanel(new BorderLayout(4, 0));

        body.setOpaque(false);

        body.add(createRoomPanel(), BorderLayout.WEST);
        body.add(createChatPanel(), BorderLayout.CENTER);
        body.add(createRightPanel(), BorderLayout.EAST);

        root.add(body, BorderLayout.CENTER);

        setContentPane(root);

        // Thông báo ban đầu.
        appendSystemMessage(
                "Đã mở giao diện chat. Đang đồng bộ dữ liệu..."
        );

        setSearchNotice(
                "Nhập từ khóa để tìm trong lịch sử của phòng đang mở."
        );
    }

    // ==========================================================
    // HEADER
    // ==========================================================

    private JPanel createHeader() {

        JPanel header = AuthUI.createCardPanel(22, 10, 18, true);

        header.setLayout(new BorderLayout(12, 0));

        // --- Thương hiệu ---
        brandLabel = new JLabel(AuthUI.getCurrentTheme().getTitle());
        brandLabel.setFont(AuthUI.font(Font.BOLD, 18));
        brandLabel.setForeground(AuthUI.TEXT_COLOR);

        taglineLabel = new JLabel(AuthUI.getCurrentTheme().getSubtitle());
        taglineLabel.setFont(AuthUI.font(Font.PLAIN, 12));
        taglineLabel.setForeground(AuthUI.MUTED_COLOR);

        JPanel texts = new JPanel();

        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        texts.add(brandLabel);
        texts.add(taglineLabel);

        JPanel brandBox = new JPanel(new BorderLayout(12, 0));

        brandBox.setOpaque(false);

        brandBox.add(
                centered(AuthUI.createLogo(42), GridBagConstraints.WEST),
                BorderLayout.WEST
        );

        brandBox.add(
                centered(texts, GridBagConstraints.WEST),
                BorderLayout.CENTER
        );

        // --- Tài khoản + đăng xuất ---
        JLabel nameLabel = new JLabel(username);

        nameLabel.setFont(AuthUI.font(Font.BOLD, 13));
        nameLabel.setForeground(AuthUI.TEXT_COLOR);

        JLabel onlineLabel = new JLabel("Đang trực tuyến");

        onlineLabel.setFont(AuthUI.font(Font.PLAIN, 11));
        onlineLabel.setForeground(AuthUI.SUCCESS_COLOR);

        JPanel accountTexts = new JPanel();

        accountTexts.setOpaque(false);
        accountTexts.setLayout(
                new BoxLayout(accountTexts, BoxLayout.Y_AXIS)
        );

        accountTexts.add(nameLabel);
        accountTexts.add(onlineLabel);

        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        chip.setOpaque(false);

        chip.add(new AvatarView(username, 38, true));
        chip.add(accountTexts);

        logoutButton = AuthUI.createButton(
                "Đăng xuất",
                false,
                110,
                38
        );

        logoutButton.addActionListener(e -> {
            for (PrivateChatDialog d : privateChatWindows.values()) {
                if (d != null) {
                    d.dispose();
                }
            }
            privateChatWindows.clear();
            controller.logout();
        });

        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 14, 0)
        );

        right.setOpaque(false);

        JButton themeBtn = AuthUI.createThemeToggleButton(this);

        right.add(chip);
        right.add(themeBtn);
        right.add(logoutButton);

        header.add(brandBox, BorderLayout.WEST);

        header.add(
                centered(right, GridBagConstraints.EAST),
                BorderLayout.EAST
        );

        return header;
    }

    // ==========================================================
    // SIDEBAR TRÁI: DANH SÁCH PHÒNG
    // ==========================================================

    private JPanel createRoomPanel() {

        JPanel panel = AuthUI.createCardPanel(22, 16, 12, true);

        panel.setPreferredSize(
                new Dimension(240, 0)
        );

        panel.setLayout(new BorderLayout(0, 12));

        JLabel title = createSectionTitle("PHÒNG CHAT");

        roomCountLabel = new JLabel("1");

        roomCountLabel.setFont(AuthUI.font(Font.BOLD, 11));
        roomCountLabel.setForeground(AuthUI.ACCENT_LIGHT);
        roomCountLabel.setBorder(new EmptyBorder(2, 2, 4, 6));

        JPanel titleRow = new JPanel(new BorderLayout());

        titleRow.setOpaque(false);

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(roomCountLabel, BorderLayout.EAST);

        roomModel = new DefaultListModel<>();

        roomModel.addElement("Sảnh chính");

        roomList = new JList<>(roomModel);

        roomList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        styleList(roomList, true);

        // Khi chọn phòng trong danh sách, chỉ cập nhật trạng thái nút Vào phòng.
        // Không tự ý gửi JOIN_ROOM lên Server khi chưa có xác nhận từ người dùng.
        roomList.addListSelectionListener(e -> {

            if (e.getValueIsAdjusting()
                    || updatingRoomList) {
                return;
            }

            updateJoinButtonState();
        });

        // Nhấp đúp chuột (Double-click) vào phòng để hỏi xác nhận và vào phòng.
        roomList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {

                    confirmAndJoinSelectedRoom();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(roomList);

        AuthUI.styleScrollPane(scroll);

        Well listWell = new Well(18, new BorderLayout());

        listWell.add(scroll, BorderLayout.CENTER);

        JLabel note = new JLabel(
                "<html><div style='width:160px;'>"
                + "Chọn phòng rồi nhấn <b>Vào phòng</b> "
                + "hoặc nhấp đúp để tham gia."
                + "</div></html>"
        );

        note.setFont(AuthUI.font(Font.PLAIN, 11));

        note.setForeground(AuthUI.MUTED_COLOR);

        note.setBorder(
                new EmptyBorder(2, 6, 0, 4)
        );

        // Khu vực dưới danh sách phòng: nút vào phòng + nút tạo phòng + ghi chú.
        JPanel roomBottom = new JPanel();

        roomBottom.setLayout(
                new BoxLayout(roomBottom, BoxLayout.Y_AXIS)
        );

        roomBottom.setOpaque(false);

        joinRoomButton = AuthUI.createButton(
                "Vào phòng",
                true,
                100,
                38
        );

        joinRoomButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        joinRoomButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );

        joinRoomButton.setEnabled(false);

        joinRoomButton.addActionListener(e -> {
            confirmAndJoinSelectedRoom();
        });

        createRoomButton = AuthUI.createButton(
                "+ Tạo phòng",
                false,
                100,
                38
        );

        createRoomButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        createRoomButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );

        createRoomButton.addActionListener(e -> {
            promptCreateRoom();
        });

        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        roomBottom.add(joinRoomButton);
        roomBottom.add(Box.createVerticalStrut(8));
        roomBottom.add(createRoomButton);
        roomBottom.add(Box.createVerticalStrut(10));
        roomBottom.add(note);

        panel.add(titleRow, BorderLayout.NORTH);
        panel.add(listWell, BorderLayout.CENTER);
        panel.add(roomBottom, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================================
    // KHU VỰC CHAT Ở GIỮA
    // ==========================================================

    private JPanel createChatPanel() {

        JPanel panel = AuthUI.createCardPanel(22, 16, 18, true);

        panel.setLayout(new BorderLayout(0, 12));

        // Thanh tiêu đề phòng đang mở.
        roomTitle = new JLabel("# Sảnh chính");

        roomTitle.setFont(AuthUI.font(Font.BOLD, 20));
        roomTitle.setForeground(AuthUI.TEXT_COLOR);

        connectionStatus = new JLabel("Đang đồng bộ...");

        connectionStatus.setFont(AuthUI.font(Font.PLAIN, 12));
        connectionStatus.setForeground(AuthUI.MUTED_COLOR);

        JPanel titleBox = new JPanel();

        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        titleBox.add(roomTitle);
        titleBox.add(connectionStatus);

        memberBadge = new JLabel("0 thành viên");

        memberBadge.setFont(AuthUI.font(Font.BOLD, 12));
        memberBadge.setForeground(AuthUI.ACCENT_LIGHT);
        memberBadge.setBorder(new EmptyBorder(6, 14, 6, 14));
        memberBadge.setOpaque(false);

        // Huy hiệu bo tròn cho số thành viên.
        JPanel badgeBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                AuthUI.applyQuality(g2);

                g2.setPaint(
                        new GradientPaint(
                                0, 0, AuthUI.alpha(AuthUI.ACCENT_COLOR, 40),
                                getWidth(), getHeight(), AuthUI.alpha(AuthUI.ACCENT_PINK, 35)
                        )
                );

                g2.fillRoundRect(
                        0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight()
                );

                g2.setPaint(
                        new GradientPaint(
                                0, 0, AuthUI.alpha(AuthUI.ACCENT_COLOR, 140),
                                getWidth(), getHeight(), AuthUI.alpha(AuthUI.ACCENT_PINK, 120)
                        )
                );

                g2.drawRoundRect(
                        0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight()
                );

                g2.dispose();
            }
        };

        badgeBox.setOpaque(false);
        badgeBox.add(memberBadge, BorderLayout.CENTER);

        // Nút Xóa phòng: chỉ hiển thị / kích hoạt cho Owner của phòng tùy chỉnh
        deleteRoomButton = AuthUI.createButton(
                "Xóa phòng",
                false,
                95,
                32
        );
        deleteRoomButton.setFont(AuthUI.font(Font.BOLD, 12));
        deleteRoomButton.setForeground(AuthUI.ERROR_COLOR);
        deleteRoomButton.setToolTipText("Chỉ Người tạo phòng (Owner) mới có quyền xóa phòng này");
        deleteRoomButton.setVisible(false);
        deleteRoomButton.addActionListener(e -> {
            confirmAndDeleteCurrentRoom();
        });

        // Nút Tải lại lịch sử phòng
        reloadHistoryButton = AuthUI.createButton(
                "Tải lại lịch sử",
                false,
                115,
                32
        );
        reloadHistoryButton.setFont(AuthUI.font(Font.BOLD, 12));
        reloadHistoryButton.setForeground(AuthUI.ACCENT_LIGHT);
        reloadHistoryButton.setToolTipText("Tải lại toàn bộ lịch sử tin nhắn của phòng chat này");
        reloadHistoryButton.addActionListener(e -> reloadRoomHistory());

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.setOpaque(false);
        rightBox.add(reloadHistoryButton);
        rightBox.add(deleteRoomButton);
        rightBox.add(badgeBox);

        JPanel roomHeader = new JPanel(new BorderLayout(8, 0));

        roomHeader.setOpaque(false);

        roomHeader.add(titleBox, BorderLayout.WEST);

        roomHeader.add(
                centered(rightBox, GridBagConstraints.EAST),
                BorderLayout.EAST
        );

        JPanel divider = new JPanel();

        divider.setBackground(
                AuthUI.alpha(AuthUI.BORDER_COLOR, 150)
        );

        divider.setPreferredSize(new Dimension(0, 1));

        JPanel north = new JPanel(new BorderLayout(0, 12));

        north.setOpaque(false);

        north.add(roomHeader, BorderLayout.NORTH);
        north.add(divider, BorderLayout.SOUTH);

        // Khung nội dung chat.
        chatList = new MessageList();

        chatScroll = new JScrollPane(chatList);

        chatScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        AuthUI.styleScrollPane(chatScroll);

        Well chatWell = new Well(20, new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                AuthUI.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();

                if (AuthUI.isHoneyPink()) {
                    // --- Honey Chat: Mèo & Cánh hoa đào trang trí ở góc dưới và trên ---
                    // Cánh hoa đào rơi nhẹ (hoa anh đào / sakura petals)
                    g2.setColor(new Color(251, 113, 133, 40));
                    g2.fillOval(w - 75, 45, 16, 10);
                    g2.fillOval(w - 45, 75, 12, 14);
                    g2.fillOval(35, h / 2, 14, 11);

                    // Chú mèo dễ thương ở góc phải dưới (matching user screenshot)
                    int catW = 100;
                    int catH = 80;
                    int catX = w - catW - 18;
                    int catY = h - catH - 12;

                    if (catX > 180 && catY > 100) {
                        // Thân mèo tròn màu trắng kem
                        g2.setColor(new Color(255, 255, 255, 200));
                        g2.fillRoundRect(catX + 15, catY + 25, 70, 55, 35, 35);
                        // Đầu mèo tròn
                        g2.fillOval(catX + 12, catY + 5, 76, 52);

                        // Tai mèo
                        java.awt.Polygon leftEar = new java.awt.Polygon(
                                new int[]{catX + 16, catX + 32, catX + 38},
                                new int[]{catY + 18, catY - 6, catY + 12}, 3
                        );
                        java.awt.Polygon rightEar = new java.awt.Polygon(
                                new int[]{catX + 62, catX + 68, catX + 84},
                                new int[]{catY + 12, catY - 6, catY + 18}, 3
                        );
                        g2.fill(leftEar);
                        g2.fill(rightEar);

                        // Bên trong tai màu hồng phấn
                        g2.setColor(new Color(251, 113, 133, 140));
                        java.awt.Polygon inLeftEar = new java.awt.Polygon(
                                new int[]{catX + 22, catX + 32, catX + 36},
                                new int[]{catY + 16, catY - 2, catY + 12}, 3
                        );
                        java.awt.Polygon inRightEar = new java.awt.Polygon(
                                new int[]{catX + 64, catX + 68, catX + 78},
                                new int[]{catY + 12, catY - 2, catY + 16}, 3
                        );
                        g2.fill(inLeftEar);
                        g2.fill(inRightEar);

                        // Viền mèo màu nâu socola dịu
                        g2.setColor(new Color(159, 115, 126, 160));
                        g2.setStroke(new java.awt.BasicStroke(1.6f));
                        g2.drawOval(catX + 12, catY + 5, 76, 52);
                        g2.draw(leftEar);
                        g2.draw(rightEar);

                        // Mắt cười híp mí đáng yêu (^_^)
                        g2.drawArc(catX + 30, catY + 22, 12, 10, 0, 180);
                        g2.drawArc(catX + 58, catY + 22, 12, 10, 0, 180);

                        // Mũi và miệng mèo
                        g2.setColor(new Color(244, 63, 94, 200));
                        g2.fillOval(catX + 48, catY + 31, 5, 4);
                        g2.setColor(new Color(159, 115, 126, 160));
                        g2.drawArc(catX + 42, catY + 31, 9, 8, 180, 180);
                        g2.drawArc(catX + 50, catY + 31, 9, 8, 180, 180);

                        // Má hồng pastel
                        g2.setColor(new Color(251, 113, 133, 90));
                        g2.fillOval(catX + 22, catY + 28, 11, 7);
                        g2.fillOval(catX + 67, catY + 28, 11, 7);

                        // Trái tim nhỏ bay trên đầu mèo
                        g2.setColor(new Color(244, 63, 94, 160));
                        g2.fillOval(catX + 4, catY + 10, 8, 8);
                        g2.fillOval(catX + 9, catY + 10, 8, 8);
                        int[] hx = {catX + 4, catX + 17, catX + 10};
                        int[] hy = {catY + 15, catY + 15, catY + 22};
                        g2.fillPolygon(hx, hy, 3);
                    }
                } else if (AuthUI.isChillBlue()) {
                    // --- Chill Talk: Mây bồng bềnh & Ngôi sao lấp lánh (sparkles) ---
                    g2.setColor(new Color(255, 255, 255, 170));
                    int cloudW = 120;
                    int cloudH = 65;
                    int cloudX = w - cloudW - 20;
                    int cloudY = h - cloudH - 15;

                    if (cloudX > 180 && cloudY > 100) {
                        g2.fillOval(cloudX + 10, cloudY + 18, 48, 45);
                        g2.fillOval(cloudX + 35, cloudY + 4, 55, 52);
                        g2.fillOval(cloudX + 68, cloudY + 18, 46, 42);
                        g2.fillRoundRect(cloudX + 12, cloudY + 30, 98, 30, 20, 20);

                        g2.setColor(new Color(100, 116, 139, 170));
                        g2.setStroke(new java.awt.BasicStroke(1.6f));
                        g2.drawArc(cloudX + 44, cloudY + 24, 10, 8, 0, 180);
                        g2.drawArc(cloudX + 62, cloudY + 24, 10, 8, 0, 180);

                        g2.setColor(new Color(251, 146, 60, 90));
                        g2.fillOval(cloudX + 36, cloudY + 29, 9, 6);
                        g2.fillOval(cloudX + 72, cloudY + 29, 9, 6);
                    }

                    g2.setColor(new Color(251, 191, 36, 120));
                    int sx = w - 45;
                    int sy = 60;
                    g2.drawLine(sx - 7, sy, sx + 7, sy);
                    g2.drawLine(sx, sy - 7, sx, sy + 7);
                    g2.fillOval(sx - 2, sy - 2, 5, 5);
                } else if (AuthUI.isGameSquad()) {
                    // --- Game Squad: Ánh sáng neon tím & góc phòng game huyền ảo ---
                    g2.setColor(new Color(139, 92, 246, 35));
                    g2.fillOval(w - 140, h - 140, 130, 130);
                    // Màn hình gaming & mèo máy tính ở góc dưới
                    int gx = w - 110;
                    int gy = h - 90;
                    if (gx > 180 && gy > 100) {
                        g2.setColor(new Color(91, 33, 182, 120));
                        g2.fillRoundRect(gx, gy, 85, 55, 10, 10);
                        g2.setColor(new Color(168, 85, 247, 90));
                        g2.drawRoundRect(gx, gy, 85, 55, 10, 10);
                        // Chú mèo ngắm màn hình game
                        g2.setColor(new Color(26, 19, 48, 220));
                        g2.fillOval(gx + 60, gy + 32, 22, 20);
                        g2.fillPolygon(new int[]{gx + 62, gx + 67, gx + 69}, new int[]{gy + 34, gy + 26, gy + 34}, 3);
                        g2.fillPolygon(new int[]{gx + 73, gx + 77, gx + 80}, new int[]{gy + 34, gy + 26, gy + 34}, 3);
                    }
                } else if (AuthUI.isDiaryGreen()) {
                    // --- Nhật Ký Chúng Mình: Cành lá xanh mướt & thiên nhiên thanh bình ---
                    g2.setColor(new Color(34, 197, 94, 60));
                    int lx = w - 80;
                    int ly = 40;
                    g2.fillOval(lx, ly, 22, 14);
                    g2.fillOval(lx + 15, ly + 15, 18, 12);
                    g2.fillOval(lx + 25, ly + 35, 20, 13);
                    g2.fillOval(30, h - 80, 24, 16);
                    g2.fillOval(50, h - 65, 18, 12);
                } else if (AuthUI.isTeamDark()) {
                    // --- Team Marketing: Mạng lưới công việc tối giản & đường line mờ hiện đại ---
                    g2.setColor(new Color(16, 185, 129, 30));
                    int mx = w - 110;
                    int my = h - 80;
                    if (mx > 180 && my > 100) {
                        g2.fillRect(mx + 10, my + 30, 14, 30);
                        g2.fillRect(mx + 30, my + 15, 14, 45);
                        g2.fillRect(mx + 50, my + 5, 14, 55);
                        g2.fillRect(mx + 70, my + 22, 14, 38);
                    }
                } else if (AuthUI.isStarNight()) {
                    // --- Bạn Thân: Bầu trời đêm ngàn sao & Ánh trăng dịu dàng ---
                    g2.setColor(new Color(253, 224, 71, 90));
                    int ms = 48;
                    int mx = w - ms - 30;
                    int my = 35;
                    java.awt.geom.Area moon = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(mx, my, ms, ms));
                    moon.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(mx + 16, my - 5, ms * 0.85f, ms * 0.85f)));
                    g2.fill(moon);

                    // Những vì sao nhỏ li ti
                    g2.setColor(new Color(255, 255, 255, 140));
                    g2.fillOval(mx - 30, my + 20, 3, 3);
                    g2.fillOval(mx - 80, my + 45, 2, 2);
                    g2.fillOval(mx - 50, my + 80, 3, 3);
                    g2.fillOval(w - 70, h - 60, 3, 3);
                    g2.fillOval(w - 120, h - 90, 2, 2);
                    g2.fillOval(45, h / 2 - 40, 3, 3);
                }

                g2.dispose();
            }
        };

        chatWell.add(chatScroll, BorderLayout.CENTER);

        // Thanh nhập tin nhắn.
        Well composer = new Well(22, new BorderLayout(8, 0));

        composer.setBorder(
                new EmptyBorder(6, 16, 6, 6)
        );

        messageField = new HintTextField(
                "Nhập tin nhắn rồi nhấn Enter để gửi..."
        );

        styleBareField(messageField, 14);

        messageField.setToolTipText(
                "Nhập tin nhắn, tối đa " + MAX_MESSAGE_LENGTH + " ký tự"
        );

        messageField.addActionListener(e -> sendMessage());

        messageField.addFocusListener(
                new WellFocus(composer)
        );

        charCounter = new JLabel("");

        charCounter.setFont(AuthUI.font(Font.PLAIN, 11));
        charCounter.setForeground(AuthUI.MUTED_COLOR);

        messageField.getDocument().addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateCounter();
                        onUserTypingAction();
                        SwingUtilities.invokeLater(() -> checkMentionAutocomplete());
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateCounter();
                        onUserTypingAction();
                        SwingUtilities.invokeLater(() -> checkMentionAutocomplete());
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        updateCounter();
                        onUserTypingAction();
                        SwingUtilities.invokeLater(() -> checkMentionAutocomplete());
                    }
                }
        );

        sendButton = new SendButton();

        sendButton.addActionListener(e -> sendMessage());

        JPanel east = new JPanel(new BorderLayout(10, 0));

        east.setOpaque(false);

        east.add(charCounter, BorderLayout.CENTER);
        east.add(sendButton, BorderLayout.EAST);

        composer.add(messageField, BorderLayout.CENTER);
        composer.add(east, BorderLayout.EAST);

        // Khung hiển thị trích dẫn khi đang trả lời tin nhắn (Reply)
        replyPreviewPanel = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AuthUI.applyQuality(g2);
                g2.setColor(AuthUI.SURFACE_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // Viền bên trái màu xanh Messenger để nhấn mạnh trích dẫn
                g2.setColor(AuthUI.ACCENT_COLOR);
                g2.fillRoundRect(0, 0, 4, getHeight() - 1, 4, 4);
                g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 160));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        replyPreviewPanel.setOpaque(false);
        replyPreviewPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        replyPreviewPanel.setVisible(false);

        replySenderLabel = new JLabel("Đang trả lời...");
        replySenderLabel.setFont(AuthUI.font(Font.BOLD, 12));
        replySenderLabel.setForeground(AuthUI.ACCENT_LIGHT);

        replySnippetLabel = new JLabel("");
        replySnippetLabel.setFont(AuthUI.font(Font.ITALIC, 12));
        replySnippetLabel.setForeground(AuthUI.MUTED_COLOR);

        JPanel replyTextBox = new JPanel();
        replyTextBox.setOpaque(false);
        replyTextBox.setLayout(new BoxLayout(replyTextBox, BoxLayout.Y_AXIS));
        replyTextBox.add(replySenderLabel);
        replyTextBox.add(replySnippetLabel);

        JButton cancelReplyBtn = new JButton("X");
        cancelReplyBtn.setFont(AuthUI.font(Font.BOLD, 12));
        cancelReplyBtn.setForeground(AuthUI.MUTED_COLOR);
        cancelReplyBtn.setContentAreaFilled(false);
        cancelReplyBtn.setBorderPainted(false);
        cancelReplyBtn.setFocusPainted(false);
        cancelReplyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelReplyBtn.addActionListener(e -> cancelReply());

        replyPreviewPanel.add(replyTextBox, BorderLayout.CENTER);
        replyPreviewPanel.add(cancelReplyBtn, BorderLayout.EAST);

        // Khung hiển thị người đang soạn tin nhắn với 3 chấm nhấp nháy động Messenger
        typingIndicatorPanel = createTypingIndicatorPanel();

        JPanel middleStack = new JPanel();
        middleStack.setLayout(new BoxLayout(middleStack, BoxLayout.Y_AXIS));
        middleStack.setOpaque(false);
        middleStack.add(typingIndicatorPanel);
        middleStack.add(Box.createVerticalStrut(4));
        middleStack.add(replyPreviewPanel);

        JPanel bottomBox = new JPanel(new BorderLayout(0, 4));
        bottomBox.setOpaque(false);
        bottomBox.add(middleStack, BorderLayout.NORTH);
        bottomBox.add(composer, BorderLayout.CENTER);

        panel.add(north, BorderLayout.NORTH);
        panel.add(chatWell, BorderLayout.CENTER);
        panel.add(bottomBox, BorderLayout.SOUTH);

        return panel;
    }

    private void updateCounter() {

        int length = messageField.getText().length();

        if (length == 0) {
            charCounter.setText("");
            return;
        }

        charCounter.setText(length + "/" + MAX_MESSAGE_LENGTH);

        charCounter.setForeground(
                length > MAX_MESSAGE_LENGTH
                        ? AuthUI.ERROR_COLOR
                        : AuthUI.MUTED_COLOR
        );
    }

    // ==========================================================
    // TYPING INDICATOR UI & LOGIC (GIỐNG MESSENGER)
    // ==========================================================

    /**
     * Tạo panel thông báo "đang soạn tin nhắn" với hiệu ứng 3 chấm nhấp nháy động
     * vẽ trực tiếp bằng Graphics2D (không dùng emoji để tránh lỗi font trên Windows).
     */
    private JPanel createTypingIndicatorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AuthUI.applyQuality(g2);
                g2.setColor(AuthUI.SURFACE_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 160));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(3, 10, 3, 10));
        panel.setVisible(false);

        // Canvas vẽ 3 chấm Messenger nhấp nháy chuyển động sin
        typingDotsCanvas = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(28, 16);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                AuthUI.applyQuality(g2);

                int dotRadius = 5;
                int baseY = 8;
                int startX = 2;
                int gap = 8;

                // 3 chấm nảy nhẹ theo sóng sin (phase khác nhau) giống Facebook Messenger
                for (int i = 0; i < 3; i++) {
                    double offset = Math.sin(typingAnimPhase + (i * 0.9));
                    int y = (int) Math.round(baseY + (offset * 2.8) - (dotRadius / 2.0));
                    int x = startX + i * gap;

                    // Độ mờ alpha từ 120 đến 255 tạo hiệu ứng nhấp nháy mượt mà
                    int alpha = (int) Math.round(180 + (offset * 70));
                    alpha = Math.max(80, Math.min(255, alpha));

                    g2.setColor(new Color(0, 132, 255, alpha)); // Màu xanh Facebook Messenger
                    g2.fillOval(x, y, dotRadius, dotRadius);
                }
                g2.dispose();
            }
        };

        typingStatusLabel = new JLabel("ai đó đang soạn tin nhắn...");
        typingStatusLabel.setFont(AuthUI.font(Font.ITALIC, 12));
        typingStatusLabel.setForeground(AuthUI.MUTED_COLOR);

        panel.add(typingDotsCanvas);
        panel.add(typingStatusLabel);

        // Timer điều khiển hoạt ảnh 3 chấm nảy nhấp nháy (~25 fps)
        typingAnimTimer = new javax.swing.Timer(40, e -> {
            typingAnimPhase += 0.2f;
            if (typingAnimPhase > Math.PI * 200) {
                typingAnimPhase = 0.0f;
            }
            if (typingDotsCanvas != null && typingDotsCanvas.isShowing()) {
                typingDotsCanvas.repaint();
            }
        });

        // Timer debounce 2 giây sau lần gõ cuối cùng thì gửi TYPING|0
        typingDebounceTimer = new javax.swing.Timer(2000, e -> {
            stopSelfTyping();
        });
        typingDebounceTimer.setRepeats(false);

        return panel;
    }

    /**
     * Gọi khi người dùng nhấn phím trong ô nhập chat.
     */
    private void onUserTypingAction() {
        String text = messageField.getText();
        if (text == null || text.trim().isEmpty()) {
            stopSelfTyping();
            return;
        }

        // Bắt đầu gõ -> thông báo cho server nếu chưa báo
        if (!isSelfTyping) {
            isSelfTyping = true;
            controller.sendProtocol(Protocol.build(Protocol.TYPING, "1"));
        }

        // Reset bộ đếm thời gian debounce
        if (typingDebounceTimer != null) {
            typingDebounceTimer.restart();
        }
    }

    /**
     * Ngừng trạng thái gõ của bản thân và gửi thông báo tới server.
     */
    private void stopSelfTyping() {
        if (typingDebounceTimer != null) {
            typingDebounceTimer.stop();
        }
        if (isSelfTyping) {
            isSelfTyping = false;
            controller.sendProtocol(Protocol.build(Protocol.TYPING, "0"));
        }
    }

    /**
     * Cập nhật hiển thị dòng thông báo và hoạt ảnh 3 chấm khi có thành viên khác gõ tin.
     */
    private void updateTypingIndicatorUI() {
        SwingUtilities.invokeLater(() -> {
            if (typingIndicatorPanel == null || typingStatusLabel == null) {
                return;
            }

            if (typingUsers.isEmpty()) {
                if (typingAnimTimer != null && typingAnimTimer.isRunning()) {
                    typingAnimTimer.stop();
                }
                typingIndicatorPanel.setVisible(false);
            } else {
                String text;
                synchronized (typingUsers) {
                    List<String> list = new ArrayList<>(typingUsers);
                    if (list.size() == 1) {
                        text = list.get(0) + " đang soạn tin nhắn...";
                    } else if (list.size() == 2) {
                        text = list.get(0) + " và " + list.get(1) + " đang soạn tin nhắn...";
                    } else {
                        text = list.get(0) + " và " + (list.size() - 1) + " người khác đang soạn tin...";
                    }
                }
                typingStatusLabel.setText(text);
                typingIndicatorPanel.setVisible(true);

                if (typingAnimTimer != null && !typingAnimTimer.isRunning()) {
                    typingAnimTimer.start();
                }
            }

            if (typingIndicatorPanel.getParent() != null) {
                typingIndicatorPanel.getParent().revalidate();
                typingIndicatorPanel.getParent().repaint();
            }
        });
    }

    // ==========================================================
    // SIDEBAR PHẢI: THÀNH VIÊN VÀ TÌM KIẾM
    // ==========================================================

    private JPanel createRightPanel() {

        JPanel panel = AuthUI.createCardPanel(22, 14, 12, true);

        panel.setPreferredSize(
                new Dimension(310, 0)
        );

        panel.setLayout(new BorderLayout());

        // --- Thanh chuyển tab dạng segmented control ---
        tabMembers = new SegmentButton("Thành viên");
        tabSearch = new SegmentButton("Tìm kiếm");

        ButtonGroup group = new ButtonGroup();

        group.add(tabMembers);
        group.add(tabSearch);

        tabMembers.setSelected(true);

        tabMembers.addActionListener(e -> showRightTab("members"));
        tabSearch.addActionListener(e -> showRightTab("search"));

        Well track = new Well(16, new GridLayout(1, 2, 4, 0));

        track.setBorder(new EmptyBorder(4, 4, 4, 4));

        track.add(tabMembers);
        track.add(tabSearch);

        JPanel top = new JPanel(new BorderLayout());

        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 12, 0));

        top.add(track, BorderLayout.CENTER);

        // --- Tab thành viên ---
        userModel = new DefaultListModel<>();

        userList = new JList<>(userModel);

        userList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        styleList(userList, false);

        // Nhấp đúp vào thành viên để mở khung chat riêng 1-1
        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int index = userList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    userList.setSelectedIndex(index);
                    String selected = userList.getModel().getElementAt(index);
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        openPrivateChat(selected);
                    }
                }
            }
        });

        // Menu chuột phải trên danh sách thành viên
        javax.swing.JPopupMenu userPopup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem pmItem = new javax.swing.JMenuItem("Nhắn tin riêng");
        pmItem.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected != null) {
                openPrivateChat(selected);
            }
        });
        userPopup.add(pmItem);

        // Mục Bổ nhiệm Admin trên menu chuột phải (chỉ Owner thấy)
        javax.swing.JMenuItem promotePopupItem = new javax.swing.JMenuItem("Bổ nhiệm Admin");
        promotePopupItem.addActionListener(e -> promoteSelectedUser());
        userPopup.add(promotePopupItem);

        // Mục Kick khỏi phòng trên menu chuột phải (Admin / Owner thấy)
        javax.swing.JMenuItem kickPopupItem = new javax.swing.JMenuItem("Kick khỏi phòng");
        kickPopupItem.addActionListener(e -> kickSelectedUser());
        userPopup.add(kickPopupItem);

        // Lắng nghe sự kiện mở menu để ẩn/hiện mục theo quyền hiện tại
        userPopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                boolean isOwner = (currentRole == RoomRole.OWNER);
                boolean isAdmin = (currentRole == RoomRole.ADMIN);
                boolean notDefaultRoom = !"Sảnh chính".equalsIgnoreCase(currentRoom);

                promotePopupItem.setVisible(isOwner && notDefaultRoom);
                kickPopupItem.setVisible((isOwner || isAdmin) && notDefaultRoom);
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        userList.setComponentPopupMenu(userPopup);

        JScrollPane usersScroll = new JScrollPane(userList);

        AuthUI.styleScrollPane(usersScroll);

        Well membersWell = new Well(18, new BorderLayout());

        membersWell.add(usersScroll, BorderLayout.CENTER);

        // Nút mở chat riêng 1-1
        privateChatButton = AuthUI.createButton(
                "Nhắn tin riêng",
                true,
                100,
                38
        );
        privateChatButton.setToolTipText(
                "Mở cửa sổ nhắn tin riêng tư 1-1 với người được chọn"
        );
        privateChatButton.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng chọn một người dùng trong danh sách thành viên.",
                        "Chưa chọn người nhận",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            openPrivateChat(selected);
        });

        // Tab thành viên: Khu vực quản lý vai trò và Kick thành viên
        promoteAdminButton = AuthUI.createButton(
                "Bổ nhiệm Admin",
                false,
                100,
                38
        );
        promoteAdminButton.setEnabled(false);
        promoteAdminButton.setToolTipText(
                "Chỉ Owner của phòng được bổ nhiệm Admin"
        );
        promoteAdminButton.addActionListener(e -> {
            promoteSelectedUser();
        });

        // Nút Kick thành viên (cho Admin và Owner)
        kickButton = AuthUI.createButton(
                "Kick thành viên",
                false,
                100,
                38
        );
        kickButton.setEnabled(false);
        kickButton.setToolTipText(
                "Admin hoặc Chủ phòng có quyền kick thành viên ra khỏi phòng"
        );
        kickButton.addActionListener(e -> {
            kickSelectedUser();
        });

        JPanel memberActions = new JPanel(new java.awt.GridLayout(3, 1, 0, 6));
        memberActions.setOpaque(false);
        memberActions.add(privateChatButton);
        memberActions.add(promoteAdminButton);
        memberActions.add(kickButton);

        JPanel membersPanel = new JPanel(new BorderLayout(0, 10));

        membersPanel.setOpaque(false);

        membersPanel.add(membersWell, BorderLayout.CENTER);
        membersPanel.add(memberActions, BorderLayout.SOUTH);

        // --- Tab tìm kiếm ---
        searchField = new HintTextField("Nhập từ khóa cần tìm...");

        styleBareField(searchField, 13);

        searchField.setToolTipText(
                "Nhập từ khóa, tối đa 100 ký tự"
        );

        searchField.addActionListener(e -> searchMessages());

        Well searchBox = new Well(16, new BorderLayout());

        searchBox.setBorder(new EmptyBorder(4, 12, 4, 12));

        searchBox.add(searchField, BorderLayout.CENTER);

        searchField.addFocusListener(
                new WellFocus(searchBox)
        );

        searchButton = AuthUI.createButton(
                "Tìm kiếm",
                true,
                100,
                40
        );

        searchButton.addActionListener(e -> searchMessages());

        JPanel searchTop = new JPanel(new BorderLayout(0, 8));

        searchTop.setOpaque(false);
        searchTop.setBorder(new EmptyBorder(0, 0, 10, 0));

        searchTop.add(searchBox, BorderLayout.NORTH);
        searchTop.add(searchButton, BorderLayout.SOUTH);

        searchList = new MessageList();

        searchScroll = new JScrollPane(searchList);

        searchScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        AuthUI.styleScrollPane(searchScroll);

        Well resultsWell = new Well(18, new BorderLayout());

        resultsWell.add(searchScroll, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new BorderLayout());

        searchPanel.setOpaque(false);

        searchPanel.add(searchTop, BorderLayout.NORTH);
        searchPanel.add(resultsWell, BorderLayout.CENTER);

        // --- Ghép hai tab bằng CardLayout ---
        rightLayout = new CardLayout();

        rightCards = new JPanel(rightLayout);

        rightCards.setOpaque(false);

        rightCards.add(membersPanel, "members");
        rightCards.add(searchPanel, "search");

        panel.add(top, BorderLayout.NORTH);
        panel.add(rightCards, BorderLayout.CENTER);

        return panel;
    }

    private void showRightTab(String name) {

        rightLayout.show(rightCards, name);

        if (name.equals("search")) {
            tabSearch.setSelected(true);
            searchField.requestFocusInWindow();
        } else {
            tabMembers.setSelected(true);
        }
    }

    // ==========================================================
    // CẬP NHẬT TRẠNG THÁI NÚT VÀO PHÒNG
    // ==========================================================

    private void updateJoinButtonState() {

        if (joinRoomButton == null || roomList == null) {
            return;
        }

        String selected = roomList.getSelectedValue();

        // Chỉ bật nút khi chọn một phòng khác với phòng hiện tại
        boolean canJoin = (selected != null
                && !selected.trim().isEmpty()
                && !selected.equals(currentRoom));

        joinRoomButton.setEnabled(canJoin);
    }

    // ==========================================================
    // XÁC NHẬN VÀ THAM GIA PHÒNG ĐÃ CHỌN
    // ==========================================================

    private void confirmAndJoinSelectedRoom() {

        String selected = roomList.getSelectedValue();

        if (selected == null || selected.trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn một phòng trong danh sách.",
                    "Chưa chọn phòng",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        if (selected.equals(currentRoom)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Bạn hiện đang ở phòng \"" + currentRoom + "\" rồi.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        // Bật hộp thoại hỏi xác nhận trước khi vào phòng
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có muốn tham gia phòng \"" + selected + "\" không?",
                "Xác nhận vào phòng",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            joinRoom(selected);
        } else {
            // Người dùng bấm Không -> trả lại lựa chọn về phòng hiện tại
            updatingRoomList = true;
            try {
                roomList.setSelectedValue(currentRoom, true);
            } finally {
                updatingRoomList = false;
            }
            updateJoinButtonState();
        }
    }

    // ==========================================================
    // XỬ LÝ CHUYỂN PHÒNG
    // ==========================================================

    private void joinRoom(String roomName) {

        if (roomName == null || roomName.isBlank()) {
            return;
        }

        currentRoom = roomName;

        // Xóa thông báo tin nhắn chưa đọc khi người dùng đã vào xem phòng này
        unreadRoomCounts.remove(roomName);
        if (roomList != null) {
            roomList.repaint();
        }

        // Dừng gõ và dọn dẹp trạng thái typing khi chuyển phòng
        stopSelfTyping();
        typingUsers.clear();
        updateTypingIndicatorUI();

        roomTitle.setText("# " + roomName);

        // Xóa nội dung phòng cũ để tránh trộn lịch sử hai phòng.
        chatList.clearRows();
        lastSentMineMessageRow = null;
        cancelReply();
        replyInfoMap.clear();

        userModel.clear();

        updateMemberCount(0);

        showStatus(
                "Đang chuyển sang phòng " + roomName + "...",
                AuthUI.MUTED_COLOR
        );

        appendSystemMessage(
                "Đang tải dữ liệu phòng " + roomName + "..."
        );

        /*
         * Server sẽ xác minh phòng, chuyển Client sang phòng mới,
         * rồi gửi lịch sử cùng thông báo HISTORY_END.
         */
        controller.sendProtocol(
                Protocol.build(
                        Protocol.JOIN_ROOM,
                        roomName
                )
        );

        controller.sendProtocol(
                Protocol.build(Protocol.GET_USERS)
        );

        // Mỗi phòng có vai trò riêng: khóa nút cho tới khi Server trả lời.
        currentRole = RoomRole.MEMBER;

        promoteAdminButton.setEnabled(false);
        if (kickButton != null) {
            kickButton.setEnabled(false);
        }
        if (deleteRoomButton != null) {
            deleteRoomButton.setVisible(false);
            deleteRoomButton.setEnabled(false);
        }

        controller.sendProtocol(
                Protocol.build(Protocol.GET_MY_ROLE)
        );

        updateJoinButtonState();
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    /**
     * Tải lại lịch sử tin nhắn của phòng hiện tại.
     * Người dùng có thể bấm nút 'Tải lại lịch sử' hoặc tự động gọi sau khi kết nối lại mạng.
     */
    public void reloadRoomHistory() {
        if (currentRoom == null || currentRoom.isBlank()) {
            return;
        }

        chatList.clearRows();
        messageRowMap.clear();
        lastSentMineMessageRow = null;
        cancelReply();
        replyInfoMap.clear();

        showStatus("Đang tải lại lịch sử phòng " + currentRoom + "...", AuthUI.ACCENT_LIGHT);
        appendSystemMessage("Đang làm mới lịch sử phòng " + currentRoom + " từ cơ sở dữ liệu...");

        controller.sendProtocol(Protocol.build(Protocol.JOIN_ROOM, currentRoom));
        controller.sendProtocol(Protocol.build(Protocol.GET_USERS));
        controller.sendProtocol(Protocol.build(Protocol.GET_MY_ROLE));
    }

    /**
     * Được gọi từ AuthController khi mạng được khôi phục và xác thực lại thành công.
     */
    public void onReconnected() {
        setBusy(false);
        showConnectionNotice("Mạng đã được khôi phục. Đang tự động làm mới lịch sử phòng...", true);
        reloadRoomHistory();
    }

    /**
     * Kiểm tra và hiển thị popup gợi ý nhắc tên @ (mention autocomplete).
     */
    private void checkMentionAutocomplete() {
        if (messageField == null) {
            return;
        }

        String text = messageField.getText();
        int caretPos = messageField.getCaretPosition();

        if (text == null || caretPos <= 0 || caretPos > text.length()) {
            hideMentionPopup();
            return;
        }

        // Tìm vị trí ký tự '@' gần nhất trước con trỏ
        int atIdx = text.lastIndexOf('@', caretPos - 1);
        if (atIdx < 0) {
            hideMentionPopup();
            return;
        }

        // Nếu trước '@' không phải khoảng trắng hoặc đầu dòng -> bỏ qua
        if (atIdx > 0 && !Character.isWhitespace(text.charAt(atIdx - 1))) {
            hideMentionPopup();
            return;
        }

        // Token người dùng đang gõ sau '@'
        String query = text.substring(atIdx + 1, caretPos).toLowerCase();
        if (query.contains(" ")) {
            hideMentionPopup();
            return;
        }

        // Tạo danh sách gợi ý
        List<String> candidates = new ArrayList<>();
        if ("everyone".startsWith(query)) {
            candidates.add("everyone");
        }

        if (userModel != null) {
            for (int i = 0; i < userModel.getSize(); i++) {
                String u = userModel.getElementAt(i);
                if (u != null && !u.isBlank()) {
                    String clean = u.replace(" (Admin)", "").replace(" (Owner)", "").trim();
                    if (!clean.equalsIgnoreCase(this.username) && clean.toLowerCase().startsWith(query)) {
                        if (!candidates.contains(clean)) {
                            candidates.add(clean);
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            hideMentionPopup();
            return;
        }

        if (mentionPopup == null) {
            mentionPopup = new JPopupMenu();
            mentionPopup.setFocusable(false);
        } else {
            mentionPopup.removeAll();
        }

        for (String c : candidates) {
            JMenuItem item = new JMenuItem("@" + c);
            item.setFont(AuthUI.font(Font.PLAIN, 13));
            if ("everyone".equalsIgnoreCase(c)) {
                item.setText("@everyone (Tất cả mọi người)");
                item.setFont(AuthUI.font(Font.BOLD, 13));
                item.setForeground(new Color(245, 158, 11));
            }
            item.addActionListener(e -> {
                insertMention(atIdx, caretPos, c);
            });
            mentionPopup.add(item);
        }

        try {
            java.awt.geom.Rectangle2D r = messageField.modelToView2D(atIdx);
            if (r != null) {
                int popupH = candidates.size() * 28 + 10;
                mentionPopup.show(messageField, (int) r.getX(), (int) r.getY() - popupH);
            } else {
                mentionPopup.show(messageField, 10, -35);
            }
            messageField.requestFocusInWindow();
        } catch (Exception ex) {
            mentionPopup.show(messageField, 10, -35);
        }
    }

    private void hideMentionPopup() {
        if (mentionPopup != null && mentionPopup.isVisible()) {
            mentionPopup.setVisible(false);
        }
    }

    private void insertMention(int atIdx, int caretPos, String candidate) {
        String text = messageField.getText();
        if (text == null) return;
        String prefix = text.substring(0, atIdx);
        String suffix = caretPos <= text.length() ? text.substring(caretPos) : "";
        String replacement = "@" + candidate + " ";
        messageField.setText(prefix + replacement + suffix);
        messageField.setCaretPosition((prefix + replacement).length());
        messageField.requestFocusInWindow();
        hideMentionPopup();
    }

    // ==========================================================
    // MỞ HỘP THOẠI TẠO PHÒNG
    // ==========================================================

    private void promptCreateRoom() {

        String input = JOptionPane.showInputDialog(
                this,
                "Nhập tên phòng mới:\n"
                + "3-40 ký tự; hỗ trợ tiếng Việt.",
                "Tạo phòng mới",
                JOptionPane.PLAIN_MESSAGE
        );

        // Người dùng bấm Cancel.
        if (input == null) {
            return;
        }

        // Chuẩn hóa NFC để xử lý tiếng Việt từ bàn phím UniKey/IME.
        // Bàn phím gõ combining marks (NFD) → chuẩn hóa về NFC trước khi kiểm tra.
        String roomName = java.text.Normalizer.normalize(
                input.trim(),
                java.text.Normalizer.Form.NFC
        );

        // Kiểm tra sơ bộ ở Client để tránh gửi dữ liệu sai.
        // \p{M} bao gồm combining diacritical marks (NFD từ bàn phím IME).
        // Server vẫn phải kiểm tra lại toàn bộ đầu vào.
        if (!roomName.matches("[\\p{L}\\p{M}\\p{N}_ -]{3,40}")) {

            JOptionPane.showMessageDialog(
                    this,
                    "Tên phòng phải dài 3-40 ký tự, "
                    + "chỉ gồm chữ, số, dấu cách, _ hoặc -.",
                    "Tên phòng không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // Kiểm tra trùng tên ở phía Client dựa vào danh sách phòng hiện có.
        // Server vẫn là người phán quyết cuối cùng.
        for (int i = 0; i < roomModel.size(); i++) {
            String existing = java.text.Normalizer.normalize(
                    roomModel.get(i),
                    java.text.Normalizer.Form.NFC
            );
            if (existing.equalsIgnoreCase(roomName)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Tên phòng \"" + roomName + "\" đã tồn tại.",
                        "Tên phòng trùng",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        createRoomButton.setEnabled(false);

        showConnectionNotice(
                "Đang tạo phòng " + roomName + "...",
                true
        );

        // Chỉ gửi yêu cầu; không tự tạo phòng ở phía Client.
        controller.sendProtocol(
                Protocol.build(
                        Protocol.CREATE_ROOM,
                        roomName
                )
        );

        // Cho phép tiếp tục thao tác sau khi gửi yêu cầu.
        createRoomButton.setEnabled(true);
    }

    // ==========================================================
    // OWNER BỔ NHIỆM THÀNH VIÊN THÀNH ADMIN
    // ==========================================================

    private void promoteSelectedUser() {

        String targetUsername = userList.getSelectedValue();

        if (targetUsername == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Hãy chọn một thành viên trong danh sách.",
                    "Chưa chọn thành viên",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        if (targetUsername.equalsIgnoreCase(username)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không cần bổ nhiệm chính mình.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        // Kiểm tra trải nghiệm người dùng; bảo mật thật nằm ở Server.
        if (currentRole != RoomRole.OWNER) {

            showConnectionNotice(
                    "Chỉ Owner mới được bổ nhiệm Admin.",
                    false
            );

            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bổ nhiệm "
                + targetUsername
                + " thành Admin của phòng "
                + currentRoom
                + "?",
                "Xác nhận bổ nhiệm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        promoteAdminButton.setEnabled(false);

        controller.sendProtocol(
                Protocol.build(
                        Protocol.PROMOTE_ADMIN,
                        targetUsername
                )
        );
    }

    // ==========================================================
    // QUYỀN ADMIN / OWNER KICK THÀNH VIÊN KHỎI PHÒNG
    // ==========================================================

    private void kickSelectedUser() {

        String targetUsername = userList.getSelectedValue();

        if (targetUsername == null || targetUsername.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Hãy chọn một thành viên trong danh sách để kick.",
                    "Chưa chọn thành viên",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        targetUsername = targetUsername.trim();

        if (targetUsername.equalsIgnoreCase(username)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không thể tự kick chính mình ra khỏi phòng.",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if ("Sảnh chính".equalsIgnoreCase(currentRoom)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể kick thành viên ra khỏi 'Sảnh chính'.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (currentRole != RoomRole.OWNER && currentRole != RoomRole.ADMIN) {
            showConnectionNotice(
                    "Chỉ Admin hoặc Chủ phòng mới có quyền kick thành viên.",
                    false
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn kick @" + targetUsername
                + " ra khỏi phòng " + currentRoom + " không?\nThành viên này sẽ bị đưa về 'Sảnh chính'.",
                "Xác nhận Kick thành viên",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (kickButton != null) {
            kickButton.setEnabled(false);
        }

        controller.sendProtocol(
                Protocol.build(
                        Protocol.KICK_USER,
                        targetUsername
                )
        );
    }

    // ==========================================================
    // OWNER XÓA PHÒNG HIỆN TẠI
    // ==========================================================

    private void confirmAndDeleteCurrentRoom() {
        if ("Sảnh chính".equalsIgnoreCase(currentRoom)
                || "Lập trình mạng".equalsIgnoreCase(currentRoom)
                || "Giải trí".equalsIgnoreCase(currentRoom)
                || "Học nhóm".equalsIgnoreCase(currentRoom)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể xóa các phòng mặc định của hệ thống.",
                    "Không thể xóa phòng",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (currentRole != RoomRole.OWNER) {
            JOptionPane.showMessageDialog(
                    this,
                    "Chỉ người tạo phòng (Owner) mới có quyền xóa phòng này.",
                    "Không có quyền",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn XÓA VĨNH VIỄN phòng '" + currentRoom + "' không?\n"
                + "Tất cả tin nhắn trong phòng sẽ bị xóa và các thành viên sẽ được chuyển về Sảnh chính.",
                "Xác nhận xóa phòng",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (deleteRoomButton != null) {
            deleteRoomButton.setEnabled(false);
        }

        showConnectionNotice("Đang xóa phòng " + currentRoom + "...", true);

        controller.sendProtocol(
                Protocol.build(
                        Protocol.DELETE_ROOM,
                        currentRoom
                )
        );
    }

    // ==========================================================
    // MỞ CỬA SỔ CHAT RIÊNG 1-1
    // ==========================================================

    private void openPrivateChat(String targetUser) {
        if (targetUser == null || targetUser.isBlank()) {
            return;
        }

        targetUser = targetUser.trim();

        if (targetUser.equalsIgnoreCase(this.username)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không thể tự gửi tin nhắn riêng cho chính mình.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        final String finalTarget = targetUser;
        PrivateChatDialog dialog = privateChatWindows.computeIfAbsent(
                finalTarget.toLowerCase(java.util.Locale.ROOT),
                k -> new PrivateChatDialog(
                        this,
                        controller,
                        this.username,
                        this.myDisplayName,
                        finalTarget,
                        finalTarget
                )
        );

        dialog.setVisible(true);
        dialog.toFront();
        dialog.requestFocus();
    }

    /** Bắt đầu trả lời một tin nhắn cụ thể. */
    private void startReply(int messageId, String sender, String content) {
        if (messageId <= 0 || content == null) return;
        this.replyTargetMessageId = messageId;
        this.replyTargetSender = sender == null ? "Người dùng" : sender;
        String snippet = content.replace("\r", " ").replace("\n", " ").trim();
        if (snippet.length() > 60) {
            snippet = snippet.substring(0, 57) + "...";
        }
        this.replyTargetSnippet = snippet;

        replySenderLabel.setText("Đang trả lời " + this.replyTargetSender + ":");
        replySnippetLabel.setText("\"" + this.replyTargetSnippet + "\"");
        replyPreviewPanel.setVisible(true);

        if (replyPreviewPanel.getParent() != null) {
            replyPreviewPanel.getParent().revalidate();
            replyPreviewPanel.getParent().repaint();
        }
        messageField.requestFocusInWindow();
    }

    /** Hủy trạng thái trả lời tin nhắn. */
    private void cancelReply() {
        this.replyTargetMessageId = 0;
        this.replyTargetSender = null;
        this.replyTargetSnippet = null;
        if (replyPreviewPanel != null) {
            replyPreviewPanel.setVisible(false);
            if (replyPreviewPanel.getParent() != null) {
                replyPreviewPanel.getParent().revalidate();
                replyPreviewPanel.getParent().repaint();
            }
        }
    }

    // ==========================================================
    // GỬI TIN NHẮN
    // ==========================================================

    private void sendMessage() {

        String content = messageField.getText().trim();

        if (content.isEmpty()) {
            return;
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {

            showConnectionNotice(
                    "Tin nhắn tối đa " + MAX_MESSAGE_LENGTH + " ký tự.",
                    false
            );

            return;
        }

        /*
         * Nếu đang trong trạng thái Reply, gửi lệnh REPLY_MESSAGE kèm replyToId.
         * Ngược lại gửi SEND_MESSAGE bình thường.
         */
        if (replyTargetMessageId > 0) {
            controller.sendProtocol(
                    Protocol.build(
                            Protocol.REPLY_MESSAGE,
                            String.valueOf(replyTargetMessageId),
                            content
                    )
            );
            cancelReply();
        } else {
            controller.sendProtocol(
                    Protocol.build(
                            Protocol.SEND_MESSAGE,
                            content
                    )
            );
        }

        messageField.setText("");
        stopSelfTyping();
        messageField.requestFocusInWindow();
    }

    // ==========================================================
    // TÌM KIẾM LỊCH SỬ TIN NHẮN
    // ==========================================================

    private void searchMessages() {

        String keyword = searchField.getText().trim();

        if (keyword.isEmpty()) {

            setSearchNotice("Hãy nhập từ khóa cần tìm.");

            return;
        }

        if (keyword.length() > 100) {

            setSearchNotice("Từ khóa tối đa 100 ký tự.");

            return;
        }

        setSearchNotice(
                "Đang tìm kiếm trong phòng " + currentRoom + "..."
        );

        /*
         * Yêu cầu tìm kiếm chỉ được gửi qua Controller
         * và ClientConnection hiện tại.
         */
        controller.sendProtocol(
                Protocol.build(
                        Protocol.SEARCH_MESSAGES,
                        keyword
                )
        );
    }

    // ==========================================================
    // TIẾP NHẬN DỮ LIỆU TỪ AUTHCONTROLLER
    // ==========================================================

    public void handleServerMessage(String message) {

        // Đảm bảo thao tác Swing chạy trên Event Dispatch Thread.
        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> handleServerMessage(message)
            );

            return;
        }

        if (message == null || message.isEmpty()) {
            return;
        }

        /*
         * Mặc định chỉ tách ở dấu | đầu tiên.
         * Các loại CHAT, HISTORY, SEARCH_RESULT sẽ được tách
         * với giới hạn phù hợp để giữ nguyên nội dung tin nhắn.
         */
        String[] header = message.split("\\|", 2);

        String type = header[0];

        String payload =
                header.length > 1
                        ? header[1]
                        : "";

        // ------------------------------------------
        // TẠO PHÒNG THÀNH CÔNG
        // CREATE_ROOM_SUCCESS|roomName|message
        // ------------------------------------------

        if (type.equals(Protocol.CREATE_ROOM_SUCCESS)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 2) {

                currentRoom = parts[1];

                roomTitle.setText("# " + currentRoom);

                // Xóa tin nhắn của phòng cũ.
                chatList.clearRows();

                // Xóa map cũ khi chuyển phòng.
                messageRowMap.clear();
                reactionPayloadMap.clear();

                userModel.clear();

                updateMemberCount(0);

                showStatus(
                        "Bạn là Owner của phòng mới.",
                        AuthUI.SUCCESS_COLOR
                );

                appendSystemMessage(
                        "Tạo phòng thành công: " + currentRoom
                );

                /*
                 * Chọn lại phòng hiện tại nếu nó có trong danh sách.
                 * Ngăn listener tự gửi JOIN_ROOM ngoài ý muốn.
                 */
                updatingRoomList = true;

                try {
                    roomList.setSelectedValue(currentRoom, true);
                } finally {
                    updatingRoomList = false;
                }

                updateJoinButtonState();

                // Yêu cầu Server cung cấp dữ liệu mới nhất.
                controller.sendProtocol(
                        Protocol.build(Protocol.GET_ROOMS)
                );

                controller.sendProtocol(
                        Protocol.build(Protocol.GET_USERS)
                );

                controller.sendProtocol(
                        Protocol.build(Protocol.GET_MY_ROLE)
                );
            }

            return;
        }

        // ------------------------------------------
        // TẠO PHÒNG THẤT BẠI
        // ------------------------------------------

        if (type.equals(Protocol.CREATE_ROOM_FAILED)) {

            showConnectionNotice(payload, false);

            return;
        }

        // ------------------------------------------
        // VAI TRÒ TRONG PHÒNG HIỆN TẠI
        // MY_ROLE|roomName|OWNER/ADMIN/MEMBER
        // ------------------------------------------

        if (type.equals(Protocol.MY_ROLE)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3
                    && parts[1].equals(currentRoom)) {

                currentRole = RoomRole.fromDatabase(parts[2]);

                boolean isOwner = (currentRole == RoomRole.OWNER);
                boolean isAdmin = (currentRole == RoomRole.ADMIN);
                boolean notDefaultRoom = !"Sảnh chính".equalsIgnoreCase(currentRoom);

                if (promoteAdminButton != null) {
                    promoteAdminButton.setEnabled(isOwner && notDefaultRoom);
                }

                if (kickButton != null) {
                    kickButton.setEnabled((isOwner || isAdmin) && notDefaultRoom);
                }

                if (deleteRoomButton != null) {
                    boolean canDelete = isOwner && notDefaultRoom;
                    deleteRoomButton.setVisible(canDelete);
                    deleteRoomButton.setEnabled(canDelete);
                }

                showStatus(
                        "Phòng: " + currentRoom
                        + " | Vai trò: " + currentRole.name(),
                        isOwner
                                ? AuthUI.SUCCESS_COLOR
                                : (isAdmin ? AuthUI.ACCENT_COLOR : AuthUI.MUTED_COLOR)
                );
            }

            return;
        }

        // ------------------------------------------
        // BỔ NHIỆM ADMIN THÀNH CÔNG
        // PROMOTE_ADMIN_SUCCESS|username|roomName
        // ------------------------------------------

        if (type.equals(Protocol.PROMOTE_ADMIN_SUCCESS)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3
                    && parts[2].equals(currentRoom)) {

                appendSystemMessage(
                        parts[1] + " đã được bổ nhiệm làm Admin."
                );

                showConnectionNotice(
                        "Bổ nhiệm Admin thành công.",
                        true
                );
            }

            if (promoteAdminButton != null) {
                promoteAdminButton.setEnabled(
                        currentRole == RoomRole.OWNER
                );
            }

            return;
        }

        // ------------------------------------------
        // BỔ NHIỆM ADMIN THẤT BẠI
        // ------------------------------------------

        if (type.equals(Protocol.PROMOTE_ADMIN_FAILED)) {

            showConnectionNotice(payload, false);

            if (promoteAdminButton != null) {
                promoteAdminButton.setEnabled(
                        currentRole == RoomRole.OWNER
                );
            }

            return;
        }

        // ------------------------------------------
        // KICK THÀNH VIÊN THÀNH CÔNG
        // KICK_USER_SUCCESS|targetUsername|roomName
        // ------------------------------------------

        if (type.equals(Protocol.KICK_USER_SUCCESS)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3 && parts[2].equals(currentRoom)) {
                appendSystemMessage("Đã kick @" + parts[1] + " ra khỏi phòng.");
                showConnectionNotice("Đã kick @" + parts[1] + " thành công.", true);
            }

            if (kickButton != null) {
                boolean canKick = (currentRole == RoomRole.OWNER || currentRole == RoomRole.ADMIN);
                kickButton.setEnabled(canKick && !"Sảnh chính".equalsIgnoreCase(currentRoom));
            }

            return;
        }

        // ------------------------------------------
        // KICK THÀNH VIÊN THẤT BẠI
        // KICK_USER_FAILED|reason
        // ------------------------------------------

        if (type.equals(Protocol.KICK_USER_FAILED)) {

            showConnectionNotice(payload, false);
            JOptionPane.showMessageDialog(
                    this,
                    payload,
                    "Không thể Kick thành viên",
                    JOptionPane.ERROR_MESSAGE
            );

            if (kickButton != null) {
                boolean canKick = (currentRole == RoomRole.OWNER || currentRole == RoomRole.ADMIN);
                kickButton.setEnabled(canKick && !"Sảnh chính".equalsIgnoreCase(currentRoom));
            }

            return;
        }

        // ------------------------------------------
        // CHÍNH MÌNH BỊ KICK RA KHỎI PHÒNG
        // KICKED|fromRoom|kickerDisplayName
        // ------------------------------------------

        if (type.equals(Protocol.KICKED)) {

            String[] parts = message.split("\\|", 3);
            String fromRoom = parts.length > 1 ? parts[1] : currentRoom;
            String kickerName = parts.length > 2 ? parts[2] : "Quản trị viên";

            currentRoom = "Sảnh chính";
            currentRole = RoomRole.MEMBER;

            // Đồng bộ chọn Sảnh chính trong danh sách phòng
            for (int i = 0; i < roomModel.size(); i++) {
                if (roomModel.get(i).equalsIgnoreCase("Sảnh chính")) {
                    roomList.setSelectedIndex(i);
                    break;
                }
            }

            roomTitle.setText("# Sảnh chính");
            chatList.clearRows();
            cancelReply();
            replyInfoMap.clear();
            userModel.clear();
            updateMemberCount(0);

            if (promoteAdminButton != null) {
                promoteAdminButton.setEnabled(false);
            }
            if (kickButton != null) {
                kickButton.setEnabled(false);
            }
            if (deleteRoomButton != null) {
                deleteRoomButton.setVisible(false);
                deleteRoomButton.setEnabled(false);
            }

            showStatus("Phòng: Sảnh chính | Vai trò: MEMBER", AuthUI.MUTED_COLOR);
            showConnectionNotice("Bạn đã bị kick khỏi phòng '" + fromRoom + "'.", false);

            JOptionPane.showMessageDialog(
                    this,
                    "Bạn đã bị " + kickerName + " kick ra khỏi phòng '" + fromRoom + "'!\nBạn đã được chuyển về 'Sảnh chính'.",
                    "Bị kick khỏi phòng",
                    JOptionPane.WARNING_MESSAGE
            );

            controller.sendProtocol(Protocol.build(Protocol.GET_USERS));
            controller.sendProtocol(Protocol.build(Protocol.GET_MY_ROLE));
            return;
        }

        // ------------------------------------------
        // XÓA PHÒNG THÀNH CÔNG (CHÍNH MÌNH LÀ OWNER ĐÃ XÓA)
        // DELETE_ROOM_SUCCESS|roomName
        // ------------------------------------------

        if (type.equals(Protocol.DELETE_ROOM_SUCCESS)) {

            String[] parts = message.split("\\|", 2);
            String deletedRoom = parts.length > 1 ? parts[1] : "";

            if (deletedRoom.equalsIgnoreCase(currentRoom)) {
                currentRoom = "Sảnh chính";
                currentRole = RoomRole.MEMBER;
                roomTitle.setText("# Sảnh chính");
                chatList.clearRows();
                cancelReply();
                replyInfoMap.clear();
                userModel.clear();
                updateMemberCount(0);

                if (promoteAdminButton != null) {
                    promoteAdminButton.setEnabled(false);
                }
                if (kickButton != null) {
                    kickButton.setEnabled(false);
                }
                if (deleteRoomButton != null) {
                    deleteRoomButton.setVisible(false);
                    deleteRoomButton.setEnabled(false);
                }
            }

            showConnectionNotice("Đã xóa phòng '" + deletedRoom + "' thành công.", true);
            appendSystemMessage("Bạn đã xóa thành công phòng '" + deletedRoom + "'.");

            JOptionPane.showMessageDialog(
                    this,
                    "Đã xóa vĩnh viễn phòng '" + deletedRoom + "' thành công!",
                    "Xóa phòng thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            controller.sendProtocol(Protocol.build(Protocol.GET_ROOMS));
            controller.sendProtocol(Protocol.build(Protocol.GET_USERS));
            controller.sendProtocol(Protocol.build(Protocol.GET_MY_ROLE));
            return;
        }

        // ------------------------------------------
        // XÓA PHÒNG THẤT BẠI
        // DELETE_ROOM_FAILED|reason
        // ------------------------------------------

        if (type.equals(Protocol.DELETE_ROOM_FAILED)) {

            if (deleteRoomButton != null) {
                deleteRoomButton.setEnabled(currentRole == RoomRole.OWNER && !"Sảnh chính".equalsIgnoreCase(currentRoom));
            }

            showConnectionNotice(payload, false);
            JOptionPane.showMessageDialog(
                    this,
                    payload,
                    "Không thể xóa phòng",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // ------------------------------------------
        // PHÒNG BỊ CHỦ PHÒNG XÓA (GỬI ĐẾN CÁC THÀNH VIÊN TRONG PHÒNG)
        // ROOM_DELETED|roomName|deletedByDisplayName
        // ------------------------------------------

        if (type.equals(Protocol.ROOM_DELETED)) {

            String[] parts = message.split("\\|", 3);
            String deletedRoom = parts.length > 1 ? parts[1] : "";
            String ownerName = parts.length > 2 ? parts[2] : "Chủ phòng";

            currentRoom = "Sảnh chính";
            currentRole = RoomRole.MEMBER;

            // Đồng bộ chọn Sảnh chính trong danh sách phòng
            for (int i = 0; i < roomModel.size(); i++) {
                if (roomModel.get(i).equalsIgnoreCase("Sảnh chính")) {
                    roomList.setSelectedIndex(i);
                    break;
                }
            }

            roomTitle.setText("# Sảnh chính");
            chatList.clearRows();
            cancelReply();
            replyInfoMap.clear();
            userModel.clear();
            updateMemberCount(0);

            if (promoteAdminButton != null) {
                promoteAdminButton.setEnabled(false);
            }
            if (kickButton != null) {
                kickButton.setEnabled(false);
            }
            if (deleteRoomButton != null) {
                deleteRoomButton.setVisible(false);
                deleteRoomButton.setEnabled(false);
            }

            showStatus("Phòng: Sảnh chính | Vai trò: MEMBER", AuthUI.MUTED_COLOR);
            showConnectionNotice("Phòng '" + deletedRoom + "' đã bị xóa.", false);

            JOptionPane.showMessageDialog(
                    this,
                    "Phòng '" + deletedRoom + "' đã bị " + ownerName + " xóa.\nBạn đã được chuyển về 'Sảnh chính'.",
                    "Phòng đã bị xóa",
                    JOptionPane.WARNING_MESSAGE
            );

            controller.sendProtocol(Protocol.build(Protocol.GET_ROOMS));
            controller.sendProtocol(Protocol.build(Protocol.GET_USERS));
            controller.sendProtocol(Protocol.build(Protocol.GET_MY_ROLE));
            return;
        }

        // ------------------------------------------
        // TIN NHẮN CHAT THỜI GIAN THỰC
        // CHAT|room|messageId|username|displayName|content
        // ------------------------------------------

        if (type.equals(Protocol.CHAT)) {

            String[] parts = message.split("\\|", 6);

            if (parts.length >= 6) {
                String targetRoom = parts[1];
                if (targetRoom.equals(currentRoom)) {
                    int msgId = parseId(parts[2]);
                    String senderUsername = parts[3];
                    String displayName = parts[4];
                    String content = parts[5];

                    boolean mine =
                            senderUsername.equalsIgnoreCase(username);

                    if (mine) {
                        myDisplayName = displayName;
                    }

                    appendChatMessage(
                            msgId,
                            displayName,
                            "@" + senderUsername,
                            currentTime(),
                            content,
                            mine
                    );

                    if (!mine) {
                        // Người khác gửi tin nhắn vào phòng mình đang mở -> phát âm thanh thông báo Messenger và đánh dấu đã xem
                        AuthUI.playMessageNotificationSound();
                        controller.sendProtocol(Protocol.build(Protocol.SEEN_MESSAGE));
                    }
                } else {
                    // Tin nhắn thuộc phòng khác -> phát âm thanh thông báo và tăng số lượng tin nhắn chưa đọc
                    AuthUI.playMessageNotificationSound();
                    int currentUnread = unreadRoomCounts.getOrDefault(targetRoom, 0);
                    unreadRoomCounts.put(targetRoom, currentUnread + 1);
                    if (roomList != null) {
                        roomList.repaint();
                    }
                }
            }

            return;
        }

        // ------------------------------------------
        // LỊCH SỬ TIN NHẮN
        // HISTORY|room|id|sentAt|displayName|content
        // ------------------------------------------

        if (type.equals(Protocol.HISTORY)) {

            String[] parts = message.split("\\|", 6);

            if (parts.length >= 6
                    && parts[1].equals(currentRoom)) {

                int msgId = parseId(parts[2]);
                String sentAt = readableTime(parts[3]);
                String displayName = parts[4];
                String content = parts[5];

                appendChatMessage(
                        msgId,
                        displayName,
                        null,
                        sentAt,
                        content,
                        isMine(displayName)
                );
            }

            return;
        }

        // ------------------------------------------
        // KẾT THÚC TẢI LỊCH SỬ
        // HISTORY_END|room
        // ------------------------------------------

        if (type.equals(Protocol.HISTORY_END)) {

            if (payload.equals(currentRoom)) {

                showStatus(
                        "Lịch sử đã được tải xong.",
                        AuthUI.MUTED_COLOR
                );

                // Gửi thông báo cho mọi người trong phòng biết mình đã xem phòng chat
                controller.sendProtocol(Protocol.build(Protocol.SEEN_MESSAGE));
            }

            return;
        }

        // ------------------------------------------
        // CẢM XÚC TIN NHẮN (REACTION)
        // REACTION_UPDATE|messageId|payload
        // payload dạng: "❤️:2,👍:1" hoặc rỗng
        // ------------------------------------------

        if (type.equals(Protocol.REACTION_UPDATE)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 2) {

                int msgId = parseId(parts[1]);
                String rxPayload = parts.length >= 3 ? parts[2] : "";

                if (msgId > 0) {
                    reactionPayloadMap.put(msgId, rxPayload);
                    MessageRow row = messageRowMap.get(msgId);
                    if (row != null) {
                        row.updateReactions(rxPayload);
                    }
                }
            }

            return;
        }

        // ------------------------------------------
        // THU HỒI TIN NHẮN (MESSAGE RECALLED)
        // MESSAGE_RECALLED|room|messageId
        // ------------------------------------------

        if (type.equals(Protocol.MESSAGE_RECALLED)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3 && parts[1].equals(currentRoom)) {

                int msgId = parseId(parts[2]);

                if (msgId > 0) {
                    reactionPayloadMap.remove(msgId);
                    MessageRow row = messageRowMap.get(msgId);
                    if (row != null) {
                        row.markAsRecalled();
                        chatList.revalidate();
                        chatList.repaint();
                    }
                }
            }

            return;
        }

        // ------------------------------------------
        // TRÍCH DẪN TRẢ LỜI TIN NHẮN (REPLY INFO)
        // REPLY_INFO|messageId|replyToId|replySender|replySnippet
        // ------------------------------------------

        if (type.equals(Protocol.REPLY_INFO)) {

            String[] parts = message.split("\\|", 5);

            if (parts.length >= 4) {

                int msgId = parseId(parts[1]);
                int replyToId = parseId(parts[2]);
                String replySender = parts[3];
                String replySnippet = parts.length >= 5 ? parts[4] : "";

                if (msgId > 0) {
                    ReplyInfo info = new ReplyInfo(replyToId, replySender, replySnippet);
                    replyInfoMap.put(msgId, info);
                    MessageRow row = messageRowMap.get(msgId);
                    if (row != null) {
                        row.setReplyInfo(replyToId, replySender, replySnippet);
                        chatList.revalidate();
                        chatList.repaint();
                    }
                }
            }

            return;
        }

        // ------------------------------------------
        // DANH SÁCH PHÒNG
        // ROOM_LIST|room1, room2, room3
        // ------------------------------------------

        if (type.equals(Protocol.ROOM_LIST)) {

            updateRoomList(payload);

            return;
        }

        // ------------------------------------------
        // DANH SÁCH THÀNH VIÊN
        // USER_LIST|room|user1, user2
        // ------------------------------------------

        if (type.equals(Protocol.USER_LIST)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3
                    && parts[1].equals(currentRoom)) {

                updateUserList(parts[2]);
            }

            return;
        }

        // ------------------------------------------
        // TIN NHẮN RIÊNG TƯ (PRIVATE CHAT 1-1)
        // PRIVATE_CHAT|senderUsername|senderDisplayName|receiverUsername|receiverDisplayName|sentAt|content
        // ------------------------------------------

        if (type.equals(Protocol.PRIVATE_CHAT)) {

            String[] parts = message.split("\\|", 7);

            if (parts.length >= 7) {

                String senderUsername = parts[1];
                String senderDisplayName = parts[2];
                String receiverUsername = parts[3];
                String receiverDisplayName = parts[4];
                String sentAt = parts[5];
                String content = parts[6];

                boolean isMine = senderUsername.equalsIgnoreCase(this.username);
                String partnerUsername = isMine ? receiverUsername : senderUsername;
                String partnerDisplayName = isMine ? receiverDisplayName : senderDisplayName;

                PrivateChatDialog dialog = privateChatWindows.computeIfAbsent(
                        partnerUsername.toLowerCase(java.util.Locale.ROOT),
                        k -> new PrivateChatDialog(
                                this,
                                controller,
                                this.username,
                                this.myDisplayName,
                                partnerUsername,
                                partnerDisplayName
                        )
                );

                dialog.appendMessage(
                        senderUsername,
                        senderDisplayName,
                        sentAt,
                        content,
                        isMine
                );

                if (!isMine) {
                    AuthUI.playMessageNotificationSound();
                }

                if (!dialog.isVisible()) {
                    dialog.setVisible(true);
                }
                dialog.toFront();
            }

            return;
        }

        // ------------------------------------------
        // LỊCH SỬ TIN NHẮN RIÊNG (PRIVATE HISTORY)
        // PRIVATE_HISTORY|senderUsername|senderDisplayName|receiverUsername|receiverDisplayName|sentAt|content
        // ------------------------------------------

        if (type.equals(Protocol.PRIVATE_HISTORY)) {

            String[] parts = message.split("\\|", 7);

            if (parts.length >= 7) {

                String senderUsername = parts[1];
                String senderDisplayName = parts[2];
                String receiverUsername = parts[3];
                String receiverDisplayName = parts[4];
                String sentAt = parts[5];
                String content = parts[6];

                boolean isMine = senderUsername.equalsIgnoreCase(this.username);
                String partnerUsername = isMine ? receiverUsername : senderUsername;
                String partnerDisplayName = isMine ? receiverDisplayName : senderDisplayName;

                PrivateChatDialog dialog = privateChatWindows.computeIfAbsent(
                        partnerUsername.toLowerCase(java.util.Locale.ROOT),
                        k -> new PrivateChatDialog(
                                this,
                                controller,
                                this.username,
                                this.myDisplayName,
                                partnerUsername,
                                partnerDisplayName
                        )
                );

                dialog.appendMessage(
                        senderUsername,
                        senderDisplayName,
                        sentAt,
                        content,
                        isMine
                );
            }

            return;
        }

        // ------------------------------------------
        // KẾT THÚC LỊCH SỬ TIN NHẮN RIÊNG
        // PRIVATE_HISTORY_END|targetUsername|count
        // ------------------------------------------

        if (type.equals(Protocol.PRIVATE_HISTORY_END)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3) {

                String targetUsername = parts[1];
                int count = parseId(parts[2]);

                PrivateChatDialog dialog = privateChatWindows.get(
                        targetUsername.toLowerCase(java.util.Locale.ROOT)
                );

                if (dialog != null) {
                    dialog.onHistoryLoaded(count);
                }
            }

            return;
        }

        // ------------------------------------------
        // KẾT QUẢ TÌM KIẾM
        // SEARCH_RESULT|room|id|sentAt|sender|content
        // ------------------------------------------

        if (type.equals(Protocol.SEARCH_RESULT)) {

            String[] parts = message.split("\\|", 6);

            if (parts.length >= 6
                    && parts[1].equals(currentRoom)) {

                String time = readableTime(parts[3]);

                String sender = parts[4];

                String content = parts[5];

                addRow(
                        searchList,
                        searchScroll,
                        new MessageRow(
                                MessageRow.Kind.SEARCH,
                                sender,
                                null,
                                time,
                                content,
                                -1, "", null
                        ),
                        false
                );
            }

            return;
        }

        // ------------------------------------------
        // KẾT THÚC TÌM KIẾM
        // SEARCH_END|room|count
        // ------------------------------------------

        if (type.equals(Protocol.SEARCH_END)) {

            String[] parts = message.split("\\|", 3);

            if (parts.length >= 3
                    && parts[1].equals(currentRoom)) {

                addRow(
                        searchList,
                        searchScroll,
                        new MessageRow(
                                MessageRow.Kind.SYSTEM,
                                null,
                                null,
                                null,
                                "Hoàn tất · Số kết quả: " + parts[2],
                                -1, "", null
                        ),
                        false
                );
            }

            return;
        }

        // ------------------------------------------
        // SYSTEM
        // ------------------------------------------

        if (type.equals(Protocol.SYSTEM)) {

            appendSystemMessage(payload);

            return;
        }

        // ------------------------------------------
        // TRẠNG THÁI SOẠN TIN NHẮN (TYPING INDICATOR)
        // TYPING|roomName|username|displayName|isTyping
        // ------------------------------------------

        if (type.equals(Protocol.TYPING)) {

            String[] parts = message.split("\\|", 5);

            if (parts.length >= 5) {
                String roomName = parts[1];
                String typingUsername = parts[2];
                String typingDisplayName = parts[3];
                boolean isTyping = "1".equals(parts[4]);

                // Chỉ hiển thị trạng thái gõ cho phòng hiện tại và không hiển thị cho chính mình
                if (roomName.equalsIgnoreCase(currentRoom) && !typingUsername.equalsIgnoreCase(this.username)) {
                    if (isTyping) {
                        typingUsers.add(typingDisplayName);
                    } else {
                        typingUsers.remove(typingDisplayName);
                    }
                    updateTypingIndicatorUI();
                }
            }

            return;
        }

        // ------------------------------------------
        // TRẠNG THÁI ĐÃ XEM TIN NHẮN (SEEN INDICATOR)
        // SEEN_MESSAGE|roomName|username|displayName
        // ------------------------------------------

        if (type.equals(Protocol.SEEN_MESSAGE)) {

            String[] parts = message.split("\\|", 4);

            if (parts.length >= 4) {
                String roomName = parts[1];
                String seenUser = parts[2];
                String seenDisplayName = parts[3];

                if (roomName.equalsIgnoreCase(currentRoom) && !seenUser.equalsIgnoreCase(this.username)) {
                    if (lastSentMineMessageRow != null) {
                        lastSentMineMessageRow.addSeenUser(seenDisplayName != null && !seenDisplayName.isBlank() ? seenDisplayName : seenUser);
                    }
                }
            }

            return;
        }

        // ------------------------------------------
        // ERROR
        // ------------------------------------------

        if (type.equals(Protocol.ERROR)) {

            appendErrorMessage(payload);

            showStatus(
                    "Server trả về thông báo lỗi.",
                    AuthUI.ERROR_COLOR
            );

            return;
        }

        // Loại phản hồi khác có thể được xử lý ở checkpoint sau.
        appendSystemMessage(
                "Đã nhận phản hồi: " + type
        );
    }

    // ==========================================================
    // CẬP NHẬT DANH SÁCH PHÒNG
    // ==========================================================

    private void updateRoomList(String data) {

        String[] rooms = data.isBlank()
                ? new String[0]
                : data.split("\\s*,\\s*");

        updatingRoomList = true;

        try {

            roomModel.clear();

            for (String room : rooms) {

                if (!room.isBlank()) {
                    roomModel.addElement(room.trim());
                }
            }

            roomCountLabel.setText(
                    String.valueOf(roomModel.getSize())
            );

            // Chọn lại phòng hiện tại nhưng không gửi JOIN_ROOM.
            if (roomModel.contains(currentRoom)) {

                roomList.setSelectedValue(
                        currentRoom,
                        true
                );

            } else if (!roomModel.isEmpty()) {

                // Không tự ý chuyển phòng khi chưa có xác nhận.
                roomList.clearSelection();
            }

        } finally {

            updatingRoomList = false;

            updateJoinButtonState();
        }
    }

    // ==========================================================
    // CẬP NHẬT DANH SÁCH THÀNH VIÊN
    // ==========================================================

    private void updateUserList(String data) {

        userModel.clear();

        if (data.isBlank()) {

            updateMemberCount(0);

            return;
        }

        String[] users = data.split("\\s*,\\s*");

        for (String user : users) {

            if (!user.isBlank()) {
                userModel.addElement(user.trim());
            }
        }

        updateMemberCount(userModel.getSize());
    }

    private void updateMemberCount(int count) {

        tabMembers.setText("Thành viên (" + count + ")");

        memberBadge.setText(count + " thành viên");
    }

    // ==========================================================
    // HIỂN THỊ TIN NHẮN
    // ==========================================================

    private boolean isMine(String senderName) {

        if (senderName == null) {
            return false;
        }

        return senderName.equalsIgnoreCase(username)
                || (myDisplayName != null
                && senderName.equals(myDisplayName));
    }

    private void appendChatMessage(
            int messageId,
            String sender,
            String handle,
            String time,
            String content,
            boolean mine) {

        if (messageId > 0 && messageRowMap.containsKey(messageId)) {
            // Tin nhắn đã tồn tại trên màn hình chat, tránh hiển thị trùng lặp
            return;
        }

        String rxPayload = reactionPayloadMap.getOrDefault(messageId, "");
        ReplyInfo rInfo = replyInfoMap.get(messageId);

        MessageRow row = new MessageRow(
                mine ? MessageRow.Kind.ME : MessageRow.Kind.OTHER,
                sender,
                handle,
                time,
                content,
                messageId,
                rxPayload,
                (msgId, emoji) -> controller.sendProtocol(
                        Protocol.build(Protocol.REACT,
                                String.valueOf(msgId), emoji)
                ),
                (msgId) -> requestRecallMessage(msgId),
                (msgId, snd, cnt) -> startReply(msgId, snd, cnt)
        );

        if (rInfo != null) {
            row.setReplyInfo(rInfo.replyToId, rInfo.replySender, rInfo.replySnippet);
        }

        if (!mine && content != null) {
            String lowerContent = content.toLowerCase();
            String mentionTag = "@" + username.toLowerCase();
            if (lowerContent.contains(mentionTag) || lowerContent.contains("@everyone")) {
                row.setMentioned(true);
            }
        }

        if (messageId > 0) {
            messageRowMap.put(messageId, row);
        }

        if (mine) {
            lastSentMineMessageRow = row;
        } else if (lastSentMineMessageRow != null) {
            // Khi đối phương gửi tin nhắn sau tin của mình -> đối phương đã đọc tin nhắn của mình
            lastSentMineMessageRow.addSeenUser(sender);
        }

        addRow(chatList, chatScroll, row, mine);
    }

    /**
     * Xác nhận và gửi yêu cầu thu hồi tin nhắn lên Server.
     */
    private void requestRecallMessage(int messageId) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn thu hồi tin nhắn này không?\n"
                + "Nội dung tin nhắn sẽ bị xóa đối với tất cả thành viên trong phòng.",
                "Xác nhận thu hồi tin nhắn",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            controller.sendProtocol(
                    Protocol.build(Protocol.RECALL_MESSAGE, String.valueOf(messageId))
            );
        }
    }

    private void appendSystemMessage(String message) {

        addRow(
                chatList,
                chatScroll,
                new MessageRow(
                        MessageRow.Kind.SYSTEM,
                        null,
                        null,
                        null,
                        message,
                        -1, "", null
                ),
                false
        );
    }

    private void appendErrorMessage(String message) {

        addRow(
                chatList,
                chatScroll,
                new MessageRow(
                        MessageRow.Kind.ERROR,
                        null,
                        null,
                        null,
                        message,
                        -1, "", null
                ),
                false
        );
    }

    /** Xóa kết quả tìm kiếm cũ và hiện một dòng thông báo. */
    private void setSearchNotice(String message) {

        searchList.clearRows();

        addRow(
                searchList,
                searchScroll,
                new MessageRow(
                        MessageRow.Kind.SYSTEM,
                        null,
                        null,
                        null,
                        message,
                        -1, "", null
                ),
                false
        );
    }

    /**
     * Thêm một dòng vào danh sách và tự cuộn xuống cuối
     * nếu người dùng đang ở gần cuối (hoặc là tin của chính mình).
     */
    private void addRow(
            MessageList list,
            JScrollPane scroll,
            MessageRow row,
            boolean forceScroll) {

        final JScrollBar bar = scroll.getVerticalScrollBar();

        boolean nearBottom =
                bar.getValue() + bar.getVisibleAmount()
                        >= bar.getMaximum() - 140;

        list.addRow(row);

        if (forceScroll || nearBottom) {

            // Chạy sau khi layout đã tính xong chiều cao mới.
            SwingUtilities.invokeLater(
                    () -> bar.setValue(bar.getMaximum())
            );
        }
    }

    // ==========================================================
    // THÔNG BÁO KẾT NỐI
    // ==========================================================

    public void showConnectionNotice(
            String message,
            boolean success) {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> showConnectionNotice(
                            message,
                            success
                    )
            );

            return;
        }

        showStatus(
                message,
                success
                        ? AuthUI.SUCCESS_COLOR
                        : AuthUI.ERROR_COLOR
        );

        if (success) {
            appendSystemMessage(message);
        } else {
            appendErrorMessage(message);
        }
    }

    private void showStatus(String message, Color color) {

        connectionStatus.setText(message);

        connectionStatus.setForeground(color);
    }

    public void setBusy(boolean busy) {

        messageField.setEnabled(!busy);
        sendButton.setEnabled(!busy);

        searchField.setEnabled(!busy);
        searchButton.setEnabled(!busy);

        roomList.setEnabled(!busy);
        createRoomButton.setEnabled(!busy);
        logoutButton.setEnabled(!busy);
    }

    // ==========================================================
    // CÁC HÀM TIỆN ÍCH GIAO DIỆN
    // ==========================================================

    private JLabel createSectionTitle(String text) {

        JLabel label = new JLabel(text);

        label.setFont(AuthUI.font(Font.BOLD, 11));

        label.setForeground(AuthUI.MUTED_COLOR);

        label.setBorder(
                new EmptyBorder(2, 6, 4, 2)
        );

        return label;
    }

    /** Canh giữa theo chiều dọc một thành phần trong thanh ngang. */
    private static JPanel centered(Component c, int anchor) {

        JPanel wrap = new JPanel(new GridBagLayout());

        wrap.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = anchor;
        gc.weightx = 1;
        gc.weighty = 1;

        wrap.add(c, gc);

        return wrap;
    }

    /** Ô nhập trong suốt, nằm bên trong một Well bo góc. */
    private static void styleBareField(JTextField field, int size) {

        field.setOpaque(false);

        field.setBorder(new EmptyBorder(8, 0, 8, 0));

        field.setFont(AuthUI.font(Font.PLAIN, size));

        field.setForeground(AuthUI.TEXT_COLOR);

        field.setCaretColor(AuthUI.ACCENT_LIGHT);

        field.setSelectionColor(AuthUI.ACCENT_COLOR);

        field.setSelectedTextColor(AuthUI.TEXT_COLOR);
    }

    private void styleList(JList<String> list, boolean roomMode) {

        list.setOpaque(false);

        list.setBackground(new Color(0, 0, 0, 0));

        list.setForeground(AuthUI.TEXT_COLOR);

        list.setFixedCellHeight(46);

        list.setCellRenderer(new ItemRenderer(roomMode));

        list.setBorder(new EmptyBorder(4, 2, 4, 2));

        // Theo dõi dòng đang được rê chuột để tô hover.
        MouseAdapter hover = new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(list, list.locationToIndex(e.getPoint()), e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(list, -1, e);
            }
        };

        list.addMouseMotionListener(hover);
        list.addMouseListener(hover);
    }

    private static void setHover(
            JList<String> list,
            int index,
            MouseEvent e) {

        // locationToIndex trả về dòng gần nhất, cần kiểm tra con trỏ
        // có thật sự nằm trong ô đó hay không.
        if (index >= 0) {

            Rectangle cell = list.getCellBounds(index, index);

            if (cell == null || !cell.contains(e.getPoint())) {
                index = -1;
            }
        }

        Object old = list.getClientProperty("hoverIndex");

        if (old instanceof Integer && (Integer) old == index) {
            return;
        }

        list.putClientProperty("hoverIndex", index);

        list.repaint();
    }

    private String currentTime() {

        return LocalTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm")
        );
    }

    private String readableTime(String rawTime) {

        if (rawTime == null || rawTime.isBlank()) {
            return currentTime();
        }

        String trimmed = rawTime.trim();

        /*
         * Nếu chuỗi thời gian đã là yyyy-MM-dd HH:mm:ss (được lưu theo localtime)
         * thì lấy trực tiếp HH:mm để không bị cộng lệch múi giờ.
         */
        if (trimmed.length() >= 16
                && (trimmed.charAt(10) == ' ' || trimmed.charAt(10) == 'T')
                && !trimmed.endsWith("Z") && !trimmed.contains("+")) {
            return trimmed.substring(11, 16);
        }

        /*
         * Dự phòng nếu là chuỗi UTC (có đuôi Z hoặc múi giờ ISO).
         */
        try {
            String isoString = trimmed.replace(" ", "T");
            if (!isoString.endsWith("Z") && !isoString.contains("+")) {
                isoString += "Z";
            }
            java.time.Instant instant = java.time.Instant.parse(isoString);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {
        }

        return trimmed;
    }

    /** Chuyển chuỗi id thành int, trả về -1 nếu lỗi. */
    private static int parseId(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ==========================================================
    // THÀNH PHẦN: RENDERER CHO DANH SÁCH PHÒNG / THÀNH VIÊN
    // ==========================================================

    private final class ItemRenderer extends JComponent
            implements ListCellRenderer<String> {

        private final boolean roomMode;

        private String value = "";
        private boolean selected;
        private boolean hovered;

        ItemRenderer(boolean roomMode) {
            this.roomMode = roomMode;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list,
                String value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            this.value = value == null ? "" : value;
            this.selected = isSelected;

            Object h = list.getClientProperty("hoverIndex");

            this.hovered = h instanceof Integer
                    && (Integer) h == index;

            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(10, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            int w = getWidth();
            int h = getHeight();

            int rx = 4;
            int ry = 3;
            int rw = w - 8;
            int rh = h - 6;

            if (selected) {

                g2.setPaint(
                        new GradientPaint(
                                rx,
                                ry,
                                AuthUI.ACCENT_COLOR,
                                rx + rw,
                                ry + rh,
                                AuthUI.ACCENT_PINK
                        )
                );

                g2.fillRoundRect(rx, ry, rw, rh, 16, 16);

                // Ánh phản quang mỏng tinh tế ở viền trên
                g2.setPaint(new GradientPaint(rx, ry, new Color(255, 255, 255, 90), rx + rw, ry, new Color(255, 255, 255, 10)));
                g2.drawLine(rx + 6, ry + 1, rx + rw - 6, ry + 1);

            } else if (hovered) {

                g2.setColor(AuthUI.alpha(AuthUI.SURFACE_COLOR, 230));

                g2.fillRoundRect(rx, ry, rw, rh, 16, 16);

                g2.setColor(AuthUI.alpha(AuthUI.ACCENT_COLOR, 60));
                g2.drawRoundRect(rx, ry, rw - 1, rh - 1, 16, 16);
            }

            int iconSize = 30;
            int iconX = rx + 10;
            int iconY = (h - iconSize) / 2;

            if (roomMode) {

                // Ô vuông bo góc chứa dấu #.
                g2.setColor(
                        selected
                                ? new Color(255, 255, 255, 45)
                                : AuthUI.alpha(AuthUI.ACCENT_COLOR, 45)
                );

                g2.fillRoundRect(iconX, iconY, iconSize, iconSize, 10, 10);

                g2.setFont(AuthUI.font(Font.BOLD, 15));

                g2.setColor(
                        selected
                                ? Color.WHITE
                                : AuthUI.ACCENT_LIGHT
                );

                FontMetrics fm = g2.getFontMetrics();

                g2.drawString(
                        "#",
                        iconX + (iconSize - fm.stringWidth("#")) / 2,
                        iconY + (iconSize - fm.getHeight()) / 2
                                + fm.getAscent()
                );

            } else {

                AuthUI.paintAvatar(g2, iconX, iconY, iconSize, value);

                // Chấm xanh "đang trực tuyến".
                g2.setColor(AuthUI.INPUT_COLOR);

                g2.fill(new Ellipse2D.Float(
                        iconX + iconSize - 11, iconY + iconSize - 11, 12, 12
                ));

                g2.setColor(AuthUI.SUCCESS_COLOR);

                g2.fill(new Ellipse2D.Float(
                        iconX + iconSize - 9, iconY + iconSize - 9, 8, 8
                ));
            }

            // Tên phòng / tên thành viên.
            int textX = iconX + iconSize + 12;

            boolean me = !roomMode && isMine(value);

            String suffix = me ? "  (Bạn)" : "";

            g2.setFont(
                    AuthUI.font(
                            selected || me ? Font.BOLD : Font.PLAIN,
                            13
                    )
            );

            FontMetrics fm = g2.getFontMetrics();

            int maxW = rx + rw - textX - 10;

            int suffixW = fm.stringWidth(suffix);

            String name = ellipsize(value, fm, maxW - suffixW);

            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(
                    selected ? Color.WHITE : AuthUI.TEXT_COLOR
            );

            g2.drawString(name, textX, ty);

            if (me) {

                g2.setColor(
                        selected
                                ? new Color(255, 255, 255, 190)
                                : AuthUI.ACCENT_LIGHT
                );

                g2.drawString(
                        suffix,
                        textX + fm.stringWidth(name),
                        ty
                );
            }

            // Huy hiệu thông báo tin nhắn chưa đọc cho danh sách phòng (Unread badge)
            if (roomMode) {
                int unread = unreadRoomCounts.getOrDefault(value, 0);
                if (unread > 0) {
                    String badgeText = unread > 99 ? "99+" : String.valueOf(unread);
                    Font badgeFont = AuthUI.font(Font.BOLD, 10);
                    g2.setFont(badgeFont);
                    FontMetrics bfm = g2.getFontMetrics(badgeFont);
                    int textWidth = bfm.stringWidth(badgeText);
                    int badgeHeight = 18;
                    int badgeWidth = Math.max(badgeHeight, textWidth + 10);
                    int badgeX = rx + rw - badgeWidth - 8;
                    int badgeY = (h - badgeHeight) / 2;

                    // Vẽ nền badge màu đỏ cam hiện đại
                    g2.setColor(new Color(239, 68, 68)); // Đỏ rực
                    g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

                    // Viền nhẹ
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.drawRoundRect(badgeX, badgeY, badgeWidth - 1, badgeHeight - 1, badgeHeight, badgeHeight);

                    // Chữ số lượng tin chưa đọc
                    g2.setColor(Color.WHITE);
                    int bty = badgeY + (badgeHeight - bfm.getHeight()) / 2 + bfm.getAscent();
                    g2.drawString(badgeText, badgeX + (badgeWidth - textWidth) / 2, bty);
                }
            }

            g2.dispose();
        }
    }

    private static String ellipsize(
            String text,
            FontMetrics fm,
            int maxWidth) {

        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }

        String dots = "…";

        int end = text.length();

        while (end > 1
                && fm.stringWidth(text.substring(0, end) + dots)
                > maxWidth) {
            end--;
        }

        return text.substring(0, end) + dots;
    }

    // ==========================================================
    // THÀNH PHẦN: KHUNG LÕM BO GÓC (Ô NHẬP, VÙNG CHAT, DANH SÁCH)
    // ==========================================================

    private static class Well extends JPanel {

        private final int arc;
        private boolean focused;

        Well(int arc, LayoutManager layout) {

            super(layout);

            this.arc = arc;

            setOpaque(false);

            setBorder(new EmptyBorder(6, 6, 6, 6));
        }

        void setFocused(boolean focused) {

            this.focused = focused;

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(AuthUI.INPUT_COLOR);

            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(
                    focused
                            ? AuthUI.alpha(AuthUI.ACCENT_COLOR, 230)
                            : AuthUI.alpha(AuthUI.BORDER_COLOR, 150)
            );

            g2.drawRoundRect(0, 0, w, h, arc, arc);

            g2.dispose();
        }
    }

    /** Đổi viền Well khi ô nhập bên trong được focus. */
    private static final class WellFocus extends FocusAdapter {

        private final Well well;

        WellFocus(Well well) {
            this.well = well;
        }

        @Override
        public void focusGained(FocusEvent e) {
            well.setFocused(true);
        }

        @Override
        public void focusLost(FocusEvent e) {
            well.setFocused(false);
        }
    }

    // ==========================================================
    // THÀNH PHẦN: Ô NHẬP CÓ CHỮ GỢI Ý
    // ==========================================================

    private static final class HintTextField extends JTextField {

        private final String hint;

        HintTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            g2.setFont(getFont());

            g2.setColor(AuthUI.alpha(AuthUI.MUTED_COLOR, 140));

            FontMetrics fm = g2.getFontMetrics();

            java.awt.Insets in = getInsets();

            g2.drawString(
                    hint,
                    in.left,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent()
            );

            g2.dispose();
        }
    }

    // ==========================================================
    // THÀNH PHẦN: NÚT GỬI TRÒN
    // ==========================================================

    private static final class SendButton extends JButton {

        SendButton() {

            setPreferredSize(new Dimension(44, 44));

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setToolTipText("Gửi (Enter)");

            setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            int d = Math.min(getWidth(), getHeight()) - 4;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;

            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed();

            if (!isEnabled()) {

                g2.setColor(AuthUI.alpha(AuthUI.SURFACE_COLOR, 160));

            } else {

                Color a = AuthUI.ACCENT_COLOR;
                Color b = AuthUI.ACCENT_PINK;

                if (hover) {
                    a = AuthUI.mix(a, Color.WHITE, 0.20f);
                    b = AuthUI.mix(b, Color.WHITE, 0.20f);
                    // Hiệu ứng phát sáng tròn quanh nút gửi khi hover
                    g2.setColor(AuthUI.alpha(AuthUI.ACCENT_COLOR, 60));
                    g2.fill(new Ellipse2D.Float(x - 2, y - 2, d + 4, d + 4));
                }

                if (pressed) {
                    a = AuthUI.mix(a, Color.BLACK, 0.2f);
                    b = AuthUI.mix(b, Color.BLACK, 0.2f);
                }

                g2.setPaint(new GradientPaint(x, y, a, x + d, y + d, b));
            }

            g2.fill(new Ellipse2D.Float(x, y, d, d));

            // Vệt phản quang ánh gương ở nửa trên nút gửi
            if (isEnabled()) {
                g2.setPaint(new GradientPaint(x, y + 1, new Color(255, 255, 255, 90), x, y + d / 2f, new Color(255, 255, 255, 10)));
                g2.fill(new Ellipse2D.Float(x + 2, y + 1, d - 4, d / 2f));
            }

            // Biểu tượng máy bay giấy.
            float s = d / 44f;

            float cx = x + d / 2f + 1.5f * s;
            float cy = y + d / 2f;

            Path2D plane = new Path2D.Float();

            plane.moveTo(cx - 10 * s, cy - 9 * s);
            plane.lineTo(cx + 11 * s, cy);
            plane.lineTo(cx - 10 * s, cy + 9 * s);
            plane.lineTo(cx - 6 * s, cy);
            plane.closePath();

            g2.setColor(
                    isEnabled()
                            ? Color.WHITE
                            : AuthUI.alpha(AuthUI.MUTED_COLOR, 160)
            );

            g2.fill(plane);

            g2.dispose();
        }
    }

    // ==========================================================
    // THÀNH PHẦN: NÚT TAB DẠNG SEGMENTED
    // ==========================================================

    private static final class SegmentButton extends JToggleButton {

        SegmentButton(String text) {

            super(text);

            setFont(AuthUI.font(Font.BOLD, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);

            setPreferredSize(new Dimension(100, 34));

            setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            int w = getWidth();
            int h = getHeight();

            boolean hover = getModel().isRollover();

            if (isSelected()) {

                g2.setPaint(
                        new GradientPaint(
                                0,
                                0,
                                AuthUI.ACCENT_COLOR,
                                w,
                                h,
                                AuthUI.ACCENT_PINK
                        )
                );

                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // Ánh phản quang mỏng
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawLine(4, 1, w - 4, 1);

            } else if (hover) {

                g2.setColor(AuthUI.alpha(AuthUI.SURFACE_COLOR, 200));

                g2.fillRoundRect(0, 0, w, h, 12, 12);
            }

            g2.setFont(getFont());

            FontMetrics fm = g2.getFontMetrics();

            g2.setColor(
                    isSelected() || hover
                            ? Color.WHITE
                            : AuthUI.MUTED_COLOR
            );

            g2.drawString(
                    getText(),
                    (w - fm.stringWidth(getText())) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent()
            );

            g2.dispose();
        }
    }

    // ==========================================================
    // THÀNH PHẦN: AVATAR TRÒN
    // ==========================================================

    private static final class AvatarView extends JComponent {

        private final String name;
        private final int size;
        private final boolean online;

        AvatarView(String name, int size, boolean online) {

            this.name = name;
            this.size = size;
            this.online = online;

            Dimension d = new Dimension(size, size);

            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            AuthUI.paintAvatar(g2, 0, 0, size - 2, name);

            if (online) {

                g2.setColor(AuthUI.CARD_COLOR);

                g2.fill(new Ellipse2D.Float(size - 14, size - 14, 14, 14));

                g2.setColor(AuthUI.SUCCESS_COLOR);

                g2.fill(new Ellipse2D.Float(size - 12, size - 12, 10, 10));
            }

            g2.dispose();
        }
    }

    // ==========================================================
    // THÀNH PHẦN: DANH SÁCH TIN NHẮN (BỐ CỤC TỰ TÍNH)
    // ==========================================================

    /**
     * Danh sách các MessageRow xếp dọc.
     * Chiều cao mỗi dòng phụ thuộc chiều rộng khung nhìn (do tự xuống
     * dòng), nên danh sách tự tính bố cục thay vì dùng BoxLayout.
     */
    private static final class MessageList extends JPanel
            implements Scrollable {

        private static final int PAD_TOP = 12;
        private static final int PAD_BOTTOM = 6;

        MessageList() {

            super(null);

            setOpaque(false);
        }

        void addRow(MessageRow row) {

            add(row);

            if (getComponentCount() > MAX_ROWS) {
                remove(0);
            }

            revalidate();
            repaint();
        }

        void clearRows() {

            removeAll();

            revalidate();
            repaint();
        }

        private int viewWidth() {

            Container parent = getParent();

            if (parent instanceof JViewport) {
                return Math.max(parent.getWidth(), 200);
            }

            return Math.max(getWidth(), 200);
        }

        private int totalHeight(int width) {

            int total = PAD_TOP + PAD_BOTTOM;

            for (Component c : getComponents()) {

                if (c instanceof MessageRow) {
                    total += ((MessageRow) c).heightFor(width);
                }
            }

            return total;
        }

        @Override
        public Dimension getPreferredSize() {

            int w = viewWidth();

            return new Dimension(w, totalHeight(w));
        }

        @Override
        public void doLayout() {

            int w = getWidth();

            int y = PAD_TOP;

            for (Component c : getComponents()) {

                if (c instanceof MessageRow) {

                    int h = ((MessageRow) c).heightFor(w);

                    c.setBounds(0, y, w, h);

                    y += h;
                }
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(300, 300);
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction) {
            return Math.max(visibleRect.height - 40, 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ==========================================================
    // THÀNH PHẦN: MỘT DÒNG TIN NHẮN (BONG BÓNG / THÔNG BÁO / KẾT QUẢ)
    // ==========================================================

    private static final class MessageRow extends JComponent {

        enum Kind {
            OTHER,   // Tin của người khác: bong bóng trái + avatar
            ME,      // Tin của mình: bong bóng phải, gradient
            SYSTEM,  // Thông báo hệ thống: viên thuốc ở giữa
            ERROR,   // Thông báo lỗi: viên thuốc đỏ
            SEARCH   // Kết quả tìm kiếm: thẻ đầy chiều ngang
        }

        private static final int AVATAR = 38;
        private static final int GAP = 10;
        private static final int SIDE = 12;
        private static final int PAD_X = 14;
        private static final int PAD_Y = 10;
        private static final int META_H = 20;
        private static final int ROW_GAP = 10;
        private static final int MAX_BUBBLE = 560;

        private static Color getBubbleOther() {
            return AuthUI.BUBBLE_OTHER_COLOR;
        }

        private static final Font NAME_FONT =
                AuthUI.font(Font.BOLD, 12);

        private static final Font META_FONT =
                AuthUI.font(Font.PLAIN, 11);

        private static final Font BODY_FONT =
                AuthUI.font(Font.PLAIN, 14);

        private static final Font SYSTEM_FONT =
                AuthUI.font(Font.PLAIN, 12);

        private static final Font REACTION_FONT =
                new Font("Segoe UI Emoji", Font.PLAIN, 13);

        /** Callback gửi lệnh REACT lên server khi người dùng bấm emoji. */
        interface ReactCallback {
            void onReact(int messageId, String emoji);
        }

        /** Callback gửi lệnh thu hồi tin nhắn lên server. */
        interface RecallCallback {
            void onRecall(int messageId);
        }

        /** Callback trả lời một tin nhắn cụ thể. */
        interface ReplyCallback {
            void onReply(int messageId, String sender, String content);
        }

        private final Kind kind;
        private final String sender;
        private final String handle;
        private final String time;
        private String content;

        // Reaction, Recall & Reply
        private final int messageId;
        private final ReactCallback reactCallback;
        private final RecallCallback recallCallback;
        private final ReplyCallback replyCallback;
        private String reactionPayload; // "❤️:2,👍:1" hoặc rỗng
        private boolean isRecalled;
        private boolean isMentioned;

        // Thông tin trích dẫn trả lời (Reply)
        private int replyToId;
        private String replySender;
        private String replySnippet;
        private int replyBoxH;

        // Danh sách người đã xem tin nhắn để vẽ avatar nhỏ ở góc dưới giống Messenger
        private final List<String> seenByUsers = new ArrayList<>();
        private static final int SEEN_AVATAR_SIZE = 16;

        // Kết quả đo được lưu lại theo chiều rộng để vẽ lại nhanh.
        private int measuredWidth = -1;
        private List<String> lines = new ArrayList<>();
        private int bubbleW;
        private int bubbleH;
        private int lineH;
        private int totalH;
        private int reactionBarH;  // chiều cao dành cho reaction bar

        MessageRow(
                Kind kind,
                String sender,
                String handle,
                String time,
                String content,
                int messageId,
                String reactionPayload,
                ReactCallback reactCallback) {

            this(kind, sender, handle, time, content, messageId, reactionPayload, reactCallback, null, null);
        }

        MessageRow(
                Kind kind,
                String sender,
                String handle,
                String time,
                String content,
                int messageId,
                String reactionPayload,
                ReactCallback reactCallback,
                RecallCallback recallCallback) {

            this(kind, sender, handle, time, content, messageId, reactionPayload, reactCallback, recallCallback, null);
        }

        MessageRow(
                Kind kind,
                String sender,
                String handle,
                String time,
                String content,
                int messageId,
                String reactionPayload,
                ReactCallback reactCallback,
                RecallCallback recallCallback,
                ReplyCallback replyCallback) {

            this.kind = kind;
            this.sender = sender == null ? "" : sender;
            this.handle = handle;
            this.time = time == null ? "" : time;
            this.content = content == null ? "" : content;
            this.messageId = messageId;
            this.reactionPayload = reactionPayload == null ? "" : reactionPayload;
            this.reactCallback = reactCallback;
            this.recallCallback = recallCallback;
            this.replyCallback = replyCallback;

            if (this.content != null && this.content.trim().equals(database.MessageDAO.RECALLED_CONTENT)) {
                this.isRecalled = true;
                this.reactionPayload = "";
            }

            setOpaque(false);

            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShowMenu(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShowMenu(e);
                }
            });
        }

        /** Thiết lập thông tin trích dẫn khi là tin reply và vẽ lại. */
        void setReplyInfo(int replyToId, String replySender, String replySnippet) {
            this.replyToId = replyToId;
            this.replySender = replySender;
            this.replySnippet = replySnippet;
            this.measuredWidth = -1;
            revalidate();
            repaint();
        }

        /** Đánh dấu tin nhắn có nhắc đến (@mention) người dùng hiện tại */
        void setMentioned(boolean mentioned) {
            this.isMentioned = mentioned;
            this.measuredWidth = -1;
            revalidate();
            repaint();
        }

        /** Đánh dấu tin nhắn đã được thu hồi và vẽ lại ngay lập tức. */
        void markAsRecalled() {
            this.content = database.MessageDAO.RECALLED_CONTENT;
            this.isRecalled = true;
            this.reactionPayload = "";
            this.measuredWidth = -1;
            revalidate();
            repaint();
        }

        /** Cập nhật reaction từ server và vẽ lại. */
        void updateReactions(String payload) {
            if (isRecalled) {
                return;
            }
            this.reactionPayload = payload == null ? "" : payload;
            this.measuredWidth = -1; // buộc measure lại
            revalidate();
            repaint();
        }

        /** Thêm người đã xem tin nhắn để vẽ avatar nhỏ Messenger */
        void addSeenUser(String userName) {
            if (userName == null || userName.isBlank() || seenByUsers.contains(userName)) {
                return;
            }
            this.seenByUsers.add(userName);
            this.measuredWidth = -1;
            revalidate();
            repaint();
        }

        private boolean isNotice() {
            return kind == Kind.SYSTEM || kind == Kind.ERROR;
        }

        // Chuột phải để sao chép nội dung hoặc thao tác.
        private void maybeShowMenu(MouseEvent e) {

            if (!e.isPopupTrigger()) {
                return;
            }

            JPopupMenu menu = new JPopupMenu();

            if (!isRecalled) {
                JMenuItem copy = new JMenuItem("Sao chép nội dung");

                copy.addActionListener(ev ->
                        Toolkit.getDefaultToolkit()
                                .getSystemClipboard()
                                .setContents(
                                        new StringSelection(content),
                                        null
                                )
                );

                menu.add(copy);
            }

            // Thêm mục trả lời tin nhắn (Reply) nếu tin chưa bị thu hồi.
            if (messageId > 0 && !isRecalled && (kind == Kind.ME || kind == Kind.OTHER) && replyCallback != null) {
                if (menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
                JMenuItem replyItem = new JMenuItem("Trả lời tin nhắn");
                replyItem.addActionListener(ev -> replyCallback.onReply(messageId, sender, content));
                menu.add(replyItem);
            }

            // Thêm mục thả cảm xúc nếu có messageId hợp lệ và chưa bị thu hồi.
            if (messageId > 0 && reactCallback != null && !isRecalled
                    && (kind == Kind.ME || kind == Kind.OTHER)) {

                menu.addSeparator();

                String[][] reactions = new String[][]{
                        {"Cảm xúc: Tim", "\u2764\uFE0F"},
                        {"Cảm xúc: Haha", "\uD83D\uDE02"},
                        {"Cảm xúc: Like", "\uD83D\uDC4D"}
                };

                for (String[] r : reactions) {

                    JMenuItem item = new JMenuItem(r[0]);

                    item.addActionListener(ev ->
                            reactCallback.onReact(messageId, r[1])
                    );

                    menu.add(item);
                }
            }

            // Thêm mục thu hồi tin nhắn nếu là tin của mình và chưa thu hồi.
            if (messageId > 0 && kind == Kind.ME && !isRecalled && recallCallback != null) {
                menu.addSeparator();
                JMenuItem recallItem = new JMenuItem("Thu hồi tin nhắn");
                recallItem.setForeground(new Color(239, 68, 68));
                recallItem.addActionListener(ev -> recallCallback.onRecall(messageId));
                menu.add(recallItem);
            }

            menu.show(this, e.getX(), e.getY());
        }

        // ------------------------------------------------------
        // ĐO KÍCH THƯỚC
        // ------------------------------------------------------

        int heightFor(int width) {

            measure(width);

            return totalH;
        }

        private void measure(int width) {

            if (measuredWidth == width) {
                return;
            }

            Font body = isRecalled
                    ? AuthUI.font(Font.ITALIC, 13)
                    : (isNotice() ? SYSTEM_FONT : BODY_FONT);

            FontMetrics fm = getFontMetrics(body);

            lineH = fm.getHeight() + (isNotice() ? 2 : 4);

            int avail;

            switch (kind) {

                case OTHER:
                    avail = Math.min(
                            width - 2 * SIDE - AVATAR - GAP,
                            MAX_BUBBLE
                    );
                    break;

                case ME:
                    avail = Math.min(width - 2 * SIDE, MAX_BUBBLE);
                    break;

                case SEARCH:
                    avail = width - 2 * SIDE;
                    break;

                default:
                    avail = Math.min(width - 2 * SIDE, 520);
                    break;
            }

            int padX = isNotice() ? 16 : PAD_X;
            int padY = isNotice() ? 7 : PAD_Y;
            int icon = kind == Kind.ERROR ? 22 : 0;

            int maxText = Math.max(60, avail - 2 * padX - icon);

            lines = wrap(content, fm, maxText);

            int textW = 0;

            for (String line : lines) {
                textW = Math.max(textW, fm.stringWidth(line));
            }

            if (kind == Kind.SEARCH) {
                bubbleW = avail;
            } else {
                bubbleW = textW + 2 * padX + icon + 2;
            }

            bubbleH = lines.size() * lineH + 2 * padY;

            // Chiều cao và chiều rộng cho hộp trích dẫn trả lời (Reply box)
            replyBoxH = 0;
            if (replyToId > 0 && !isRecalled && (kind == Kind.OTHER || kind == Kind.ME)) {
                replyBoxH = 36;
                bubbleH += replyBoxH + 4;
                int minReplyW = 160;
                if (replySender != null) {
                    minReplyW = Math.max(minReplyW, fm.stringWidth("Tra loi: " + replySender) + 36);
                }
                if (replySnippet != null) {
                    minReplyW = Math.max(minReplyW, fm.stringWidth(replySnippet) + 36);
                }
                bubbleW = Math.max(bubbleW, Math.min(avail, minReplyW));
            }

            // Tính chiều cao reaction bar (chỉ với OTHER và ME khi chưa bị thu hồi).
            reactionBarH = 0;
            if (!isRecalled && (kind == Kind.OTHER || kind == Kind.ME)
                    && !reactionPayload.isEmpty()) {
                reactionBarH = 24;
            }

            int seenH = (!isRecalled && !seenByUsers.isEmpty()) ? (SEEN_AVATAR_SIZE + 4) : 0;

            switch (kind) {

                case OTHER:
                    totalH = Math.max(AVATAR, META_H + bubbleH)
                            + reactionBarH + seenH + ROW_GAP;
                    break;

                case ME:
                    totalH = META_H + bubbleH + reactionBarH + seenH + ROW_GAP;
                    break;

                case SEARCH:
                    bubbleH += META_H;
                    totalH = bubbleH + ROW_GAP;
                    break;

                default:
                    totalH = bubbleH + ROW_GAP;
                    break;
            }

            measuredWidth = width;
        }

        /**
         * Phân tích chuỗi payload "❤️:2,👍:1" thành map emoji → count.
         * Giữ thứ tự theo chuỗi ALLOWED_EMOJIS.
         */
        private java.util.LinkedHashMap<String, Integer> parseReactions() {

            java.util.LinkedHashMap<String, Integer> map =
                    new java.util.LinkedHashMap<>();

            if (reactionPayload == null || reactionPayload.isEmpty()) {
                return map;
            }

            for (String token : reactionPayload.split(",")) {
                String[] kv = token.split(":", 2);
                if (kv.length == 2) {
                    try {
                        map.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            return map;
        }

        /** Tự xuống dòng theo chiều rộng tối đa, tôn trọng ký tự xuống dòng. */
        private static List<String> wrap(
                String text,
                FontMetrics fm,
                int maxW) {

            List<String> out = new ArrayList<>();

            for (String para : text.split("\\R", -1)) {

                if (para.isEmpty()) {
                    out.add("");
                    continue;
                }

                StringBuilder line = new StringBuilder();

                for (String word : para.split(" ", -1)) {

                    String candidate = line.length() == 0
                            ? word
                            : line + " " + word;

                    if (fm.stringWidth(candidate) <= maxW) {

                        line.setLength(0);
                        line.append(candidate);

                        continue;
                    }

                    if (line.length() > 0) {

                        out.add(line.toString());

                        line.setLength(0);
                    }

                    // Từ dài hơn cả một dòng thì cắt cứng.
                    while (fm.stringWidth(word) > maxW
                            && word.length() > 1) {

                        int cut = fitChars(word, fm, maxW);

                        out.add(word.substring(0, cut));

                        word = word.substring(cut);
                    }

                    line.append(word);
                }

                out.add(line.toString());
            }

            return out;
        }

        private static int fitChars(
                String word,
                FontMetrics fm,
                int maxW) {

            int cut = 1;

            while (cut < word.length()
                    && fm.stringWidth(word.substring(0, cut + 1))
                    <= maxW) {
                cut++;
            }

            // Không cắt giữa cặp surrogate (emoji).
            if (cut < word.length()
                    && Character.isHighSurrogate(word.charAt(cut - 1))) {

                cut = cut > 1 ? cut - 1 : Math.min(2, word.length());
            }

            return cut;
        }

        // ------------------------------------------------------
        // VẼ
        // ------------------------------------------------------

        @Override
        protected void paintComponent(Graphics g) {

            measure(getWidth());

            Graphics2D g2 = (Graphics2D) g.create();

            AuthUI.applyQuality(g2);

            switch (kind) {

                case OTHER:
                    paintOther(g2);
                    break;

                case ME:
                    paintMine(g2);
                    break;

                case SEARCH:
                    paintSearch(g2);
                    break;

                default:
                    paintNotice(g2);
                    break;
            }

            g2.dispose();
        }

        private void paintOther(Graphics2D g2) {

            AuthUI.paintAvatar(g2, SIDE, 0, AVATAR, sender);

            int mx = SIDE + AVATAR + GAP;

            // Dòng tên + giờ phía trên bong bóng.
            g2.setFont(NAME_FONT);

            g2.setColor(
                    AuthUI.mix(
                            AuthUI.avatarColors(sender)[0],
                            Color.WHITE,
                            0.4f
                    )
            );

            g2.drawString(sender, mx + 2, 14);

            int nameW = g2.getFontMetrics().stringWidth(sender);

            g2.setFont(META_FONT);

            g2.setColor(AuthUI.MUTED_COLOR);

            String meta = (handle != null && !handle.isEmpty()
                    ? handle + "  ·  "
                    : "") + time;

            g2.drawString(meta, mx + 2 + nameW + 8, 14);

            int metaW = g2.getFontMetrics().stringWidth(meta);

            // Hiển thị huy hiệu @ Bạn được nhắc nếu tin nhắn có mention
            if (isMentioned && !isRecalled) {
                int badgeX = mx + 2 + nameW + 8 + metaW + 10;
                g2.setFont(AuthUI.font(Font.BOLD, 10));
                String badgeText = "@ Bạn được nhắc";
                int bTextW = g2.getFontMetrics().stringWidth(badgeText);
                int bH = 15;
                int bY = 2;

                g2.setColor(AuthUI.isDarkMode() ? new Color(70, 48, 10) : new Color(254, 243, 199));
                g2.fillRoundRect(badgeX, bY, bTextW + 12, bH, 8, 8);
                g2.setColor(new Color(245, 158, 11));
                g2.drawRoundRect(badgeX, bY, bTextW + 11, bH - 1, 8, 8);
                g2.drawString(badgeText, badgeX + 6, bY + 11);
            }

            // Bong bóng.
            int by = META_H;

            if (isRecalled) {
                // Bong bóng mờ cho tin đã thu hồi của người khác
                g2.setColor(AuthUI.isDarkMode() ? new Color(30, 38, 60) : new Color(242, 243, 245));
                g2.fillRoundRect(mx, by, bubbleW, bubbleH, 18, 18);
                g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 150));
                g2.drawRoundRect(mx, by, bubbleW - 1, bubbleH - 1, 18, 18);
            } else if (isMentioned) {
                // Bong bóng highlight màu vàng cam nổi bật cho tin nhắn nhắc tên
                g2.setColor(AuthUI.isDarkMode() ? new Color(42, 32, 18) : new Color(255, 251, 235));
                g2.fillRoundRect(mx, by, bubbleW, bubbleH, 18, 18);
                g2.setColor(new Color(245, 158, 11));
                g2.drawRoundRect(mx, by, bubbleW - 1, bubbleH - 1, 18, 18);
            } else {
                g2.setColor(getBubbleOther());
                g2.fillRoundRect(mx, by, bubbleW, bubbleH, 18, 18);
                g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 160));
                g2.drawRoundRect(mx, by, bubbleW - 1, bubbleH - 1, 18, 18);
            }

            int contentTop = by + PAD_Y;

            // Vẽ khối trích dẫn trả lời (Reply quote box)
            if (replyToId > 0 && !isRecalled) {
                int qx = mx + 8;
                int qy = by + 8;
                int qw = bubbleW - 16;
                int qh = replyBoxH;

                g2.setColor(AuthUI.isDarkMode() ? new Color(22, 28, 44) : new Color(0, 0, 0, 15));
                g2.fillRoundRect(qx, qy, qw, qh, 10, 10);

                g2.setColor(AuthUI.ACCENT_COLOR);
                g2.fillRoundRect(qx, qy, 3, qh, 3, 3);

                g2.setFont(AuthUI.font(Font.BOLD, 11));
                g2.setColor(AuthUI.ACCENT_LIGHT);
                g2.drawString("Tra loi: " + (replySender == null ? "Nguoi dung" : replySender), qx + 10, qy + 14);

                g2.setFont(AuthUI.font(Font.ITALIC, 11));
                g2.setColor(AuthUI.MUTED_COLOR);
                String snip = replySnippet == null ? "" : replySnippet;
                if (snip.length() > 38) snip = snip.substring(0, 35) + "...";
                g2.drawString(snip, qx + 10, qy + 28);

                contentTop += replyBoxH + 4;
            }

            g2.setColor(isRecalled ? AuthUI.MUTED_COLOR : AuthUI.TEXT_COLOR);

            drawLines(g2, mx + PAD_X, contentTop);

            // Vẽ reaction bar dưới bong bóng (căn trái) nếu chưa bị thu hồi.
            int otherBottomY = by + bubbleH + 4;
            if (reactionBarH > 0 && !isRecalled) {
                paintReactionBar(g2, mx, otherBottomY, false);
                otherBottomY += reactionBarH + 2;
            }

            // Vẽ avatar nhỏ của người đã xem
            if (!isRecalled && !seenByUsers.isEmpty()) {
                paintSeenAvatars(g2, mx + bubbleW, otherBottomY, true);
            }
        }

        private void paintMine(Graphics2D g2) {

            int w = getWidth();

            int bx = w - SIDE - bubbleW;
            int by = META_H;

            // Giờ gửi canh phải phía trên bong bóng.
            g2.setFont(META_FONT);

            g2.setColor(AuthUI.MUTED_COLOR);

            String meta = "Bạn  ·  " + time;

            g2.drawString(
                    meta,
                    w - SIDE - 2 - g2.getFontMetrics().stringWidth(meta),
                    14
            );

            if (isRecalled) {
                // Bong bóng mờ cho tin đã thu hồi của chính mình
                g2.setColor(AuthUI.isDarkMode() ? new Color(30, 38, 60) : new Color(230, 235, 245));
                g2.fillRoundRect(bx, by, bubbleW, bubbleH, 18, 18);
                g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 150));
                g2.drawRoundRect(bx, by, bubbleW - 1, bubbleH - 1, 18, 18);
            } else {
                g2.setPaint(
                        new GradientPaint(
                                bx,
                                by,
                                AuthUI.ACCENT_COLOR,
                                bx + bubbleW,
                                by + bubbleH,
                                AuthUI.ACCENT_PINK
                        )
                );

                g2.fillRoundRect(bx, by, bubbleW, bubbleH, 18, 18);

                // Ánh sáng bóng bẩy trên đỉnh bubble
                g2.setPaint(new GradientPaint(bx, by + 1, new Color(255, 255, 255, 75), bx + bubbleW, by + 1, new Color(255, 255, 255, 10)));
                g2.drawLine(bx + 8, by + 1, bx + bubbleW - 8, by + 1);
            }

            int contentTop = by + PAD_Y;

            // Vẽ khối trích dẫn trả lời (Reply quote box)
            if (replyToId > 0 && !isRecalled) {
                int qx = bx + 8;
                int qy = by + 8;
                int qw = bubbleW - 16;
                int qh = replyBoxH;

                g2.setColor(new Color(0, 0, 0, 25));
                g2.fillRoundRect(qx, qy, qw, qh, 10, 10);

                g2.setColor(new Color(255, 255, 255, 220));
                g2.fillRoundRect(qx, qy, 3, qh, 3, 3);

                g2.setFont(AuthUI.font(Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString("Tra loi: " + (replySender == null ? "Nguoi dung" : replySender), qx + 10, qy + 14);

                g2.setFont(AuthUI.font(Font.ITALIC, 11));
                g2.setColor(new Color(255, 255, 255, 190));
                String snip = replySnippet == null ? "" : replySnippet;
                if (snip.length() > 38) snip = snip.substring(0, 35) + "...";
                g2.drawString(snip, qx + 10, qy + 28);

                contentTop += replyBoxH + 4;
            }

            g2.setColor(isRecalled ? AuthUI.MUTED_COLOR : Color.WHITE);

            drawLines(g2, bx + PAD_X, contentTop);

            // Vẽ reaction bar dưới bong bóng (căn phải) nếu chưa bị thu hồi.
            int bottomY = by + bubbleH + 4;
            if (reactionBarH > 0 && !isRecalled) {
                paintReactionBar(g2, bx, bottomY, true);
                bottomY += reactionBarH + 2;
            }

            // Vẽ avatar nhỏ của người đã xem tin nhắn ở góc dưới bên phải giống Messenger
            if (!isRecalled && !seenByUsers.isEmpty()) {
                paintSeenAvatars(g2, w - SIDE, bottomY, true);
            }
        }

        private void paintSeenAvatars(Graphics2D g2, int anchorX, int y, boolean rightAlign) {
            int gap = 2;
            int totalW = seenByUsers.size() * SEEN_AVATAR_SIZE - (seenByUsers.size() > 1 ? (seenByUsers.size() - 1) * 4 : 0);
            int startX = rightAlign ? (anchorX - totalW) : anchorX;
            int cx = startX;

            for (int i = 0; i < seenByUsers.size(); i++) {
                String u = seenByUsers.get(i);
                // Vẽ avatar tròn nhỏ
                AuthUI.paintAvatar(g2, cx, y, SEEN_AVATAR_SIZE, u);
                // Viền bảo vệ quanh avatar nhỏ
                g2.setColor(AuthUI.CARD_COLOR);
                g2.drawOval(cx, y, SEEN_AVATAR_SIZE, SEEN_AVATAR_SIZE);
                cx += (SEEN_AVATAR_SIZE - 4); // Xếp đè nhẹ lên nhau
            }
        }

        /**
         * Vẽ hàng pill emoji-count dưới bubble.
         */
        private void paintReactionBar(Graphics2D g2, int bubbleX, int y, boolean rightAlign) {

            java.util.LinkedHashMap<String, Integer> rxMap = parseReactions();

            if (rxMap.isEmpty()) return;

            g2.setFont(REACTION_FONT);
            FontMetrics fm = g2.getFontMetrics();

            int pillH = 20;
            int pillPadX = 6;
            int gap = 4;

            // Tính tổng chiều rộng để canh phải.
            int totalPillW = 0;
            java.util.List<String[]> pills = new java.util.ArrayList<>();

            for (java.util.Map.Entry<String, Integer> entry : rxMap.entrySet()) {
                String label = entry.getKey() + " " + entry.getValue();
                int pw = fm.stringWidth(label) + pillPadX * 2;
                pills.add(new String[]{label, String.valueOf(pw)});
                totalPillW += pw + gap;
            }

            if (totalPillW > 0) totalPillW -= gap;

            int startX = rightAlign
                    ? bubbleX + bubbleW - totalPillW
                    : bubbleX;

            int cx = startX;

            for (String[] pill : pills) {
                String label = pill[0];
                int pw = Integer.parseInt(pill[1]);

                // Nền pill
                g2.setColor(AuthUI.isDarkMode() ? new Color(34, 44, 70) : Color.WHITE);
                g2.fillRoundRect(cx, y, pw, pillH, pillH, pillH);

                // Viền pill
                g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 180));
                g2.drawRoundRect(cx, y, pw, pillH - 1, pillH, pillH);

                // Label
                g2.setColor(AuthUI.TEXT_COLOR);
                g2.drawString(label,
                        cx + pillPadX,
                        y + (pillH - fm.getHeight()) / 2 + fm.getAscent());

                cx += pw + gap;
            }
        }

        private void paintSearch(Graphics2D g2) {

            int x = SIDE;
            int w = bubbleW;

            g2.setColor(new Color(255, 255, 255, 12));

            g2.fillRoundRect(x, 0, w, bubbleH, 14, 14);

            g2.setColor(AuthUI.alpha(AuthUI.BORDER_COLOR, 170));

            g2.drawRoundRect(x, 0, w - 1, bubbleH - 1, 14, 14);

            // Dòng người gửi + giờ.
            g2.setFont(NAME_FONT);

            g2.setColor(AuthUI.ACCENT_LIGHT);

            g2.drawString(sender, x + PAD_X, PAD_Y + 10);

            g2.setFont(META_FONT);

            g2.setColor(AuthUI.MUTED_COLOR);

            g2.drawString(
                    time,
                    x + w - PAD_X - g2.getFontMetrics().stringWidth(time),
                    PAD_Y + 10
            );

            g2.setColor(AuthUI.TEXT_COLOR);

            drawLines(g2, x + PAD_X, PAD_Y + META_H);
        }

        private void paintNotice(Graphics2D g2) {

            boolean error = kind == Kind.ERROR;

            int x = (getWidth() - bubbleW) / 2;

            Color accent = error
                    ? AuthUI.ERROR_COLOR
                    : AuthUI.MUTED_COLOR;

            int arc = lines.size() > 1 ? 18 : bubbleH;

            g2.setColor(
                    error
                            ? AuthUI.alpha(AuthUI.ERROR_COLOR, 34)
                            : new Color(0, 0, 0, 15)
            );

            g2.fillRoundRect(x, 0, bubbleW, bubbleH, arc, arc);

            if (error) {

                g2.setColor(AuthUI.alpha(AuthUI.ERROR_COLOR, 110));

                g2.drawRoundRect(x, 0, bubbleW - 1, bubbleH - 1, arc, arc);

                // Biểu tượng dấu chấm than trong vòng tròn.
                int iy = (bubbleH - 16) / 2;

                g2.setColor(AuthUI.ERROR_COLOR);

                g2.fill(new Ellipse2D.Float(x + 12, iy, 16, 16));

                g2.setColor(AuthUI.BACKGROUND_TOP);

                g2.setFont(AuthUI.font(Font.BOLD, 12));

                FontMetrics ifm = g2.getFontMetrics();

                g2.drawString(
                        "!",
                        x + 12 + (16 - ifm.stringWidth("!")) / 2,
                        iy + (16 - ifm.getHeight()) / 2 + ifm.getAscent()
                );
            }

            g2.setFont(SYSTEM_FONT);

            g2.setColor(accent);

            FontMetrics fm = g2.getFontMetrics();

            int icon = error ? 22 : 0;

            int ty = 7 + fm.getAscent();

            for (String line : lines) {

                int tx = error
                        ? x + 16 + icon
                        : x + (bubbleW - fm.stringWidth(line)) / 2;

                g2.drawString(line, tx, ty);

                ty += lineH;
            }
        }

        private void drawLines(Graphics2D g2, int x, int top) {

            if (isRecalled) {
                g2.setFont(AuthUI.font(Font.ITALIC, 13));
            } else {
                g2.setFont(BODY_FONT);
            }

            FontMetrics fm = g2.getFontMetrics();

            int y = top + fm.getAscent() + 1;

            for (String line : lines) {

                g2.drawString(line, x, y);

                y += lineH;
            }
        }
    }
}