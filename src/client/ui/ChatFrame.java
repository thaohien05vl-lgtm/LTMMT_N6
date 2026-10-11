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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    private JButton sendButton;
    private JButton searchButton;
    private JButton logoutButton;

    // Nút tạo phòng mới.
    private JButton createRoomButton;

    // Nút vào phòng.
    private JButton joinRoomButton;

    // Nút bổ nhiệm thành viên thành Admin.
    private JButton promoteAdminButton;

    // Vai trò nhận từ Server trong phòng hiện tại.
    // Mặc định MEMBER cho đến khi Server trả lời.
    private RoomRole currentRole = RoomRole.MEMBER;

    private JLabel roomTitle;
    private JLabel connectionStatus;
    private JLabel memberBadge;
    private JLabel roomCountLabel;
    private JLabel charCounter;

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
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
            }
        });

        initComponents();
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
        JLabel brand = new JLabel("Group Chat Network");

        brand.setFont(AuthUI.font(Font.BOLD, 18));
        brand.setForeground(AuthUI.TEXT_COLOR);

        JLabel tagline = new JLabel(
                "Trò chuyện nhóm theo thời gian thực"
        );

        tagline.setFont(AuthUI.font(Font.PLAIN, 12));
        tagline.setForeground(AuthUI.MUTED_COLOR);

        JPanel texts = new JPanel();

        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        texts.add(brand);
        texts.add(tagline);

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
            controller.logout();
        });

        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 16, 0)
        );

        right.setOpaque(false);

        right.add(chip);
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

                g2.setColor(AuthUI.alpha(AuthUI.ACCENT_COLOR, 38));

                g2.fillRoundRect(
                        0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight()
                );

                g2.setColor(AuthUI.alpha(AuthUI.ACCENT_COLOR, 110));

                g2.drawRoundRect(
                        0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight()
                );

                g2.dispose();
            }
        };

        badgeBox.setOpaque(false);
        badgeBox.add(memberBadge, BorderLayout.CENTER);

        JPanel roomHeader = new JPanel(new BorderLayout(8, 0));

        roomHeader.setOpaque(false);

        roomHeader.add(titleBox, BorderLayout.WEST);

        roomHeader.add(
                centered(badgeBox, GridBagConstraints.EAST),
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

        Well chatWell = new Well(20, new BorderLayout());

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
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateCounter();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        updateCounter();
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

        panel.add(north, BorderLayout.NORTH);
        panel.add(chatWell, BorderLayout.CENTER);
        panel.add(composer, BorderLayout.SOUTH);

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

        JScrollPane usersScroll = new JScrollPane(userList);

        AuthUI.styleScrollPane(usersScroll);

        Well membersWell = new Well(18, new BorderLayout());

        membersWell.add(usersScroll, BorderLayout.CENTER);

        // Tab thành viên có thêm khu vực quản lý vai trò.
        promoteAdminButton = AuthUI.createButton(
                "Bổ nhiệm Admin",
                true,
                100,
                40
        );

        // Tạm khóa cho đến khi Server xác nhận quyền OWNER.
        promoteAdminButton.setEnabled(false);

        promoteAdminButton.setToolTipText(
                "Chỉ Owner của phòng được bổ nhiệm Admin"
        );

        promoteAdminButton.addActionListener(e -> {
            promoteSelectedUser();
        });

        JPanel membersPanel = new JPanel(new BorderLayout(0, 10));

        membersPanel.setOpaque(false);

        membersPanel.add(membersWell, BorderLayout.CENTER);
        membersPanel.add(promoteAdminButton, BorderLayout.SOUTH);

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

        roomTitle.setText("# " + roomName);

        // Xóa nội dung phòng cũ để tránh trộn lịch sử hai phòng.
        chatList.clearRows();

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

        controller.sendProtocol(
                Protocol.build(Protocol.GET_MY_ROLE)
        );

        updateJoinButtonState();
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
         * Không tự thêm tin nhắn vào khung chat tại đây.
         * Server sẽ broadcast CHAT về Client sau khi xử lý,
         * tránh hiển thị trùng một tin nhắn.
         *
         * Server tách nội dung SEND_MESSAGE tại dấu | đầu tiên,
         * nên phần nội dung có thể chứa dấu |.
         */
        controller.sendProtocol(
                Protocol.build(
                        Protocol.SEND_MESSAGE,
                        content
                )
        );

        messageField.setText("");
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

                if (promoteAdminButton != null) {

                    promoteAdminButton.setEnabled(
                            currentRole == RoomRole.OWNER
                    );
                }

                showStatus(
                        "Phòng: " + currentRoom
                        + " | Vai trò: " + currentRole.name(),
                        currentRole == RoomRole.OWNER
                                ? AuthUI.SUCCESS_COLOR
                                : AuthUI.MUTED_COLOR
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
        // TIN NHẮN CHAT THỜI GIAN THỰC
        // CHAT|room|username|displayName|content
        // ------------------------------------------

        if (type.equals(Protocol.CHAT)) {

            String[] parts = message.split("\\|", 5);

            if (parts.length >= 5
                    && parts[1].equals(currentRoom)) {

                String senderUsername = parts[2];
                String displayName = parts[3];
                String content = parts[4];

                boolean mine =
                        senderUsername.equalsIgnoreCase(username);

                if (mine) {
                    myDisplayName = displayName;
                }

                appendChatMessage(
                        displayName,
                        "@" + senderUsername,
                        currentTime(),
                        content,
                        mine
                );
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

                String sentAt = readableTime(parts[3]);

                String displayName = parts[4];

                String content = parts[5];

                appendChatMessage(
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
                                content
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
                                "Hoàn tất · Số kết quả: " + parts[2]
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
            String sender,
            String handle,
            String time,
            String content,
            boolean mine) {

        addRow(
                chatList,
                chatScroll,
                new MessageRow(
                        mine
                                ? MessageRow.Kind.ME
                                : MessageRow.Kind.OTHER,
                        sender,
                        handle,
                        time,
                        content
                ),
                mine
        );
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
                        message
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
                        message
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
                        message
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

        /*
         * Hỗ trợ các chuỗi timestamp phổ biến:
         * yyyy-MM-dd HH:mm:ss
         * yyyy-MM-ddTHH:mm:ss
         */
        if (rawTime.length() >= 16
                && (rawTime.charAt(10) == ' '
                || rawTime.charAt(10) == 'T')) {

            return rawTime.substring(11, 16);
        }

        return rawTime;
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
                                AuthUI.alpha(AuthUI.ACCENT_COLOR, 215),
                                rx + rw,
                                ry,
                                AuthUI.alpha(AuthUI.ACCENT_ALT, 190)
                        )
                );

                g2.fillRoundRect(rx, ry, rw, rh, 16, 16);

            } else if (hovered) {

                g2.setColor(AuthUI.alpha(AuthUI.SURFACE_COLOR, 210));

                g2.fillRoundRect(rx, ry, rw, rh, 16, 16);
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

    private static final class Well extends JPanel {

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
                Color b = AuthUI.ACCENT_ALT;

                if (hover) {
                    a = AuthUI.mix(a, Color.WHITE, 0.16f);
                    b = AuthUI.mix(b, Color.WHITE, 0.16f);
                }

                if (pressed) {
                    a = AuthUI.mix(a, Color.BLACK, 0.2f);
                    b = AuthUI.mix(b, Color.BLACK, 0.2f);
                }

                g2.setPaint(new GradientPaint(x, y, a, x + d, y + d, b));
            }

            g2.fill(new Ellipse2D.Float(x, y, d, d));

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
                                AuthUI.ACCENT_ALT
                        )
                );

                g2.fillRoundRect(0, 0, w, h, 12, 12);

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

        private static final Color BUBBLE_OTHER = new Color(38, 48, 86);

        private static final Font NAME_FONT =
                AuthUI.font(Font.BOLD, 12);

        private static final Font META_FONT =
                AuthUI.font(Font.PLAIN, 11);

        private static final Font BODY_FONT =
                AuthUI.font(Font.PLAIN, 14);

        private static final Font SYSTEM_FONT =
                AuthUI.font(Font.PLAIN, 12);

        private final Kind kind;
        private final String sender;
        private final String handle;
        private final String time;
        private final String content;

        // Kết quả đo được lưu lại theo chiều rộng để vẽ lại nhanh.
        private int measuredWidth = -1;
        private List<String> lines = new ArrayList<>();
        private int bubbleW;
        private int bubbleH;
        private int lineH;
        private int totalH;

        MessageRow(
                Kind kind,
                String sender,
                String handle,
                String time,
                String content) {

            this.kind = kind;
            this.sender = sender == null ? "" : sender;
            this.handle = handle;
            this.time = time == null ? "" : time;
            this.content = content == null ? "" : content;

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

        private boolean isNotice() {
            return kind == Kind.SYSTEM || kind == Kind.ERROR;
        }

        // Chuột phải để sao chép nội dung dòng.
        private void maybeShowMenu(MouseEvent e) {

            if (!e.isPopupTrigger()) {
                return;
            }

            JPopupMenu menu = new JPopupMenu();

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

            Font body = isNotice() ? SYSTEM_FONT : BODY_FONT;

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

            switch (kind) {

                case OTHER:
                    totalH = Math.max(AVATAR, META_H + bubbleH) + ROW_GAP;
                    break;

                case ME:
                    totalH = META_H + bubbleH + ROW_GAP;
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

            // Bong bóng.
            int by = META_H;

            g2.setColor(BUBBLE_OTHER);

            g2.fillRoundRect(mx, by, bubbleW, bubbleH, 18, 18);

            // Góc trên trái vuông hơn để giống "đuôi" bong bóng.
            g2.fillRect(mx, by, 10, 10);

            g2.setColor(AuthUI.TEXT_COLOR);

            drawLines(g2, mx + PAD_X, by + PAD_Y);
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

            g2.setPaint(
                    new GradientPaint(
                            bx,
                            by,
                            AuthUI.ACCENT_COLOR,
                            bx + bubbleW,
                            by + bubbleH,
                            AuthUI.ACCENT_ALT
                    )
            );

            g2.fillRoundRect(bx, by, bubbleW, bubbleH, 18, 18);

            // Góc trên phải vuông hơn.
            g2.fillRect(bx + bubbleW - 10, by, 10, 10);

            g2.setColor(Color.WHITE);

            drawLines(g2, bx + PAD_X, by + PAD_Y);
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
                            : new Color(255, 255, 255, 16)
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

            g2.setFont(BODY_FONT);

            FontMetrics fm = g2.getFontMetrics();

            int y = top + fm.getAscent() + 1;

            for (String line : lines) {

                g2.drawString(line, x, y);

                y += lineH;
            }
        }
    }
}