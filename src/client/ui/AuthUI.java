/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * AuthUI:
 * Chứa các thành phần giao diện được dùng chung
 * giữa LoginFrame, RegisterFrame và ChatFrame.
 *
 * Phong cách: dark mode tím - chàm, bo góc lớn, đổ bóng nhẹ,
 * nút gradient có hiệu ứng hover, thanh cuộn mỏng.
 *
 * Không xử lý Socket hoặc giao thức mạng tại đây.
 *
 * Các hàm cũ (createRootPanel, createCardPanel, title, subtitle,
 * fieldLabel, styleInput, createPrimaryButton, createSecondaryButton,
 * createStatusLabel, setStatus) vẫn giữ nguyên tên và chữ ký,
 * nên LoginFrame và RegisterFrame không cần sửa.
 */
public final class AuthUI {

    // ==========================================================
    // BẢNG MÀU GIAO DIỆN
    // ==========================================================

    public static final Color BACKGROUND_TOP =
            new Color(11, 15, 36);

    public static final Color BACKGROUND_BOTTOM =
            new Color(30, 20, 64);

    public static final Color CARD_COLOR =
            new Color(23, 29, 54);

    public static final Color INPUT_COLOR =
            new Color(14, 20, 40);

    public static final Color ACCENT_COLOR =
            new Color(139, 92, 246);

    // Màu phụ của gradient (chàm), dùng cho nút và bong bóng chat.
    public static final Color ACCENT_ALT =
            new Color(99, 102, 241);

    // Màu nhấn sáng, dùng cho chữ nhấn trên nền tối.
    public static final Color ACCENT_LIGHT =
            new Color(196, 181, 253);

    // Nền phụ: nút phụ, trạng thái hover của danh sách.
    public static final Color SURFACE_COLOR =
            new Color(36, 45, 80);

    public static final Color TEXT_COLOR =
            new Color(245, 247, 255);

    public static final Color MUTED_COLOR =
            new Color(165, 176, 202);

    public static final Color BORDER_COLOR =
            new Color(60, 72, 108);

    public static final Color SUCCESS_COLOR =
            new Color(74, 222, 128);

    public static final Color ERROR_COLOR =
            new Color(251, 113, 133);

    private static final String FONT_FAMILY = "Segoe UI";

    // Vùng đệm để vẽ bóng đổ quanh card.
    private static final int CARD_SHADOW = 6;

    // Bảng màu avatar: mỗi người dùng nhận một cặp màu cố định.
    private static final Color[][] AVATAR_PALETTE = {
        {new Color(139, 92, 246), new Color(99, 102, 241)},
        {new Color(236, 72, 153), new Color(244, 114, 182)},
        {new Color(14, 165, 233), new Color(56, 189, 248)},
        {new Color(16, 185, 129), new Color(52, 211, 153)},
        {new Color(245, 158, 11), new Color(251, 191, 36)},
        {new Color(239, 68, 68), new Color(251, 113, 133)},
        {new Color(20, 184, 166), new Color(45, 212, 191)},
        {new Color(168, 85, 247), new Color(217, 70, 239)}
    };

    private AuthUI() {
        // Không cho tạo đối tượng AuthUI.
    }

    // ==========================================================
    // HÀM TIỆN ÍCH DÙNG CHUNG
    // ==========================================================

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.max(0, Math.min(255, alpha))
        );
    }

    /** Trộn hai màu: t = 0 giữ màu a, t = 1 lấy màu b. */
    public static Color mix(Color a, Color b, float t) {

        float k = Math.max(0f, Math.min(1f, t));

        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k)
        );
    }

    /** Bật khử răng cưa cho hình và chữ. */
    public static void applyQuality(Graphics2D g2) {

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        g2.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE
        );

        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
    }

    // ==========================================================
    // NỀN GRADIENT + VỆT SÁNG
    // ==========================================================

    public static JPanel createRootPanel() {

        JPanel panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                Graphics2D g2 =
                        (Graphics2D) g.create();

                applyQuality(g2);

                int w = getWidth();
                int h = getHeight();

                g2.setPaint(
                        new GradientPaint(
                                0,
                                0,
                                BACKGROUND_TOP,
                                w,
                                h,
                                BACKGROUND_BOTTOM
                        )
                );

                g2.fillRect(0, 0, w, h);

                // Hai vệt sáng mờ tạo chiều sâu cho nền.
                paintGlow(
                        g2,
                        w * 0.12f,
                        h * 0.08f,
                        Math.max(w, h) * 0.55f,
                        alpha(ACCENT_COLOR, 70),
                        w,
                        h
                );

                paintGlow(
                        g2,
                        w * 0.92f,
                        h * 0.96f,
                        Math.max(w, h) * 0.50f,
                        alpha(ACCENT_ALT, 55),
                        w,
                        h
                );

                g2.dispose();
            }
        };

        panel.setLayout(
                new java.awt.GridBagLayout()
        );

        panel.setOpaque(true);

        return panel;
    }

    private static void paintGlow(
            Graphics2D g2,
            float cx,
            float cy,
            float radius,
            Color color,
            int w,
            int h) {

        RadialGradientPaint glow = new RadialGradientPaint(
                cx,
                cy,
                radius,
                new float[]{0f, 1f},
                new Color[]{color, alpha(color, 0)}
        );

        g2.setPaint(glow);
        g2.fillRect(0, 0, w, h);
    }

    // ==========================================================
    // KHUNG CARD BO GÓC CÓ ĐỔ BÓNG
    // ==========================================================

    public static JPanel createCardPanel() {
        return createCardPanel(28, 26, 28, true);
    }

    /**
     * Card tùy chỉnh.
     *
     * @param arc    độ bo góc
     * @param padV   đệm trên/dưới bên trong card
     * @param padH   đệm trái/phải bên trong card
     * @param shadow có vẽ bóng đổ hay không
     */
    public static JPanel createCardPanel(
            final int arc,
            int padV,
            int padH,
            final boolean shadow) {

        final int s = shadow ? CARD_SHADOW : 0;

        JPanel panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 =
                        (Graphics2D) g.create();

                applyQuality(g2);

                int x = s;
                int y = s;
                int w = getWidth() - 2 * s;
                int h = getHeight() - 2 * s;

                // Bóng đổ nhiều lớp, mờ dần ra ngoài.
                for (int i = s; i > 0; i--) {

                    g2.setColor(new Color(0, 0, 0, 11));

                    g2.fillRoundRect(
                            x - i,
                            y - i + 2,
                            w + 2 * i,
                            h + 2 * i,
                            arc + i * 2,
                            arc + i * 2
                    );
                }

                // Nền card: gradient rất nhẹ từ trên xuống.
                g2.setPaint(
                        new GradientPaint(
                                0,
                                y,
                                mix(CARD_COLOR, Color.WHITE, 0.05f),
                                0,
                                y + h,
                                CARD_COLOR
                        )
                );

                g2.fillRoundRect(x, y, w - 1, h - 1, arc, arc);

                // Viền card.
                g2.setColor(alpha(BORDER_COLOR, 190));

                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

                g2.dispose();
            }
        };

        panel.setOpaque(false);

        panel.setBorder(
                new EmptyBorder(
                        padV + s,
                        padH + s,
                        padV + s,
                        padH + s
                )
        );

        return panel;
    }

    // ==========================================================
    // LABEL
    // ==========================================================

    public static JLabel title(String text) {

        JLabel label = new JLabel(text);

        label.setFont(font(Font.BOLD, 28));

        label.setForeground(TEXT_COLOR);

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        return label;
    }

    public static JLabel subtitle(String text) {

        JLabel label = new JLabel(text);

        label.setFont(font(Font.PLAIN, 13));

        label.setForeground(MUTED_COLOR);

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        return label;
    }

    public static JLabel fieldLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(font(Font.BOLD, 13));

        label.setForeground(TEXT_COLOR);

        label.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        Dimension pref = label.getPreferredSize();

        label.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        pref.height
                )
        );

        label.setBorder(
                new EmptyBorder(0, 4, 0, 0)
        );

        return label;
    }

    // ==========================================================
    // Ô NHẬP LIỆU BO GÓC, ĐỔI VIỀN KHI FOCUS
    // Dùng được cho JTextField và JPasswordField.
    // ==========================================================

    public static void styleInput(final JTextField field) {

        field.setFont(font(Font.PLAIN, 14));

        field.setBackground(INPUT_COLOR);

        field.setForeground(TEXT_COLOR);

        field.setCaretColor(ACCENT_LIGHT);

        field.setSelectionColor(ACCENT_COLOR);

        field.setSelectedTextColor(TEXT_COLOR);

        final Border normal = inputBorder(BORDER_COLOR);
        final Border focused = inputBorder(ACCENT_COLOR);

        field.setBorder(normal);

        field.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(normal);
            }
        });

        field.setPreferredSize(
                new Dimension(320, 44)
        );

        field.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44)
        );

        field.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );
    }

    private static Border inputBorder(Color line) {

        return BorderFactory.createCompoundBorder(
                new RoundedBorder(line, 14, CARD_COLOR),
                new EmptyBorder(10, 14, 10, 14)
        );
    }

    /**
     * Viền bo góc. Phần góc bên ngoài đường bo được tô lại bằng
     * màu nền phía sau để ô nhập (hình chữ nhật) trông như bo tròn.
     */
    public static final class RoundedBorder extends AbstractBorder {

        private final Color color;
        private final int arc;
        private final Color backdrop;

        public RoundedBorder(Color color, int arc, Color backdrop) {
            this.color = color;
            this.arc = arc;
            this.backdrop = backdrop;
        }

        @Override
        public void paintBorder(
                Component c,
                Graphics g,
                int x,
                int y,
                int width,
                int height) {

            Graphics2D g2 = (Graphics2D) g.create();

            applyQuality(g2);

            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(
                    x + 0.5f,
                    y + 0.5f,
                    width - 1f,
                    height - 1f,
                    arc,
                    arc
            );

            Area corners = new Area(
                    new Rectangle2D.Float(x, y, width, height)
            );

            corners.subtract(new Area(shape));

            g2.setColor(backdrop);
            g2.fill(corners);

            g2.setColor(color);
            g2.draw(shape);

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }

    // ==========================================================
    // NÚT BẤM GRADIENT
    // ==========================================================

    public static JButton createPrimaryButton(
            String text) {

        return createButton(text, true, 320, 46);
    }

    public static JButton createSecondaryButton(
            String text) {

        return createButton(text, false, 320, 46);
    }

    public static JButton createButton(
            String text,
            boolean primary,
            int width,
            int height) {

        JButton button = new ModernButton(text, primary);

        button.setPreferredSize(
                new Dimension(width, height)
        );

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, height)
        );

        button.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        return button;
    }

    private static final class ModernButton extends JButton {

        private final boolean primary;

        ModernButton(String text, boolean primary) {

            super(text);

            this.primary = primary;

            setFont(font(Font.BOLD, 14));
            setForeground(TEXT_COLOR);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setMargin(new Insets(0, 0, 0, 0));

            setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            applyQuality(g2);

            int w = getWidth();
            int h = getHeight();
            int arc = 14;

            ButtonModel model = getModel();

            boolean hover = model.isRollover();
            boolean pressed = model.isPressed() && model.isArmed();

            Color textColor = TEXT_COLOR;

            if (!isEnabled()) {

                g2.setColor(alpha(SURFACE_COLOR, 140));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                textColor = alpha(MUTED_COLOR, 140);

            } else if (primary) {

                Color a = ACCENT_COLOR;
                Color b = ACCENT_ALT;

                if (hover) {
                    a = mix(a, Color.WHITE, 0.14f);
                    b = mix(b, Color.WHITE, 0.14f);
                }

                if (pressed) {
                    a = mix(a, Color.BLACK, 0.18f);
                    b = mix(b, Color.BLACK, 0.18f);
                }

                g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Đường sáng mảnh ở mép trên cho cảm giác nổi.
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawLine(arc / 2, 1, w - arc / 2, 1);

            } else {

                Color bg = SURFACE_COLOR;

                if (hover) {
                    bg = mix(bg, Color.WHITE, 0.08f);
                }

                if (pressed) {
                    bg = mix(bg, Color.BLACK, 0.15f);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                g2.setColor(
                        hover
                                ? alpha(ACCENT_COLOR, 200)
                                : alpha(BORDER_COLOR, 220)
                );

                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            g2.setFont(getFont());

            FontMetrics fm = g2.getFontMetrics();

            String text = getText();

            int tx = (w - fm.stringWidth(text)) / 2;

            int ty = (h - fm.getHeight()) / 2
                    + fm.getAscent()
                    + (pressed ? 1 : 0);

            g2.setColor(textColor);
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }

    // ==========================================================
    // THÔNG BÁO TRẠNG THÁI
    // ==========================================================

    public static JLabel createStatusLabel() {

        JLabel label = new JLabel(
                "Đang chuẩn bị kết nối..."
        );

        label.setFont(font(Font.PLAIN, 12));

        label.setForeground(MUTED_COLOR);

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        label.setHorizontalAlignment(
                JLabel.CENTER
        );

        label.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 60)
        );

        return label;
    }

    public static void setStatus(
            JLabel label,
            String message,
            Color color) {

        if (message == null) {
            message = "";
        }

        // Tránh để ký tự trong thông báo bị hiểu thành HTML.
        String safeMessage = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");

        label.setForeground(color);

        label.setText(
                "<html><div style='text-align:center;'>"
                + safeMessage
                + "</div></html>"
        );
    }

    // ==========================================================
    // AVATAR CHỮ CÁI ĐẦU
    // ==========================================================

    /** Cặp màu gradient cố định theo tên người dùng. */
    public static Color[] avatarColors(String name) {

        int hash = name == null
                ? 0
                : name.trim().toLowerCase().hashCode() & 0x7fffffff;

        return AVATAR_PALETTE[hash % AVATAR_PALETTE.length];
    }

    /** Lấy chữ cái viết tắt: "Nguyễn Văn An" -> "NA", "khi" -> "K". */
    public static String initials(String name) {

        if (name == null) {
            return "?";
        }

        String clean = name.trim();

        while (clean.startsWith("@")) {
            clean = clean.substring(1);
        }

        if (clean.isEmpty()) {
            return "?";
        }

        String[] parts = clean.split("\\s+");

        String first = new String(
                Character.toChars(parts[0].codePointAt(0))
        );

        if (parts.length >= 2) {

            String last = parts[parts.length - 1];

            first += new String(
                    Character.toChars(last.codePointAt(0))
            );
        }

        return first.toUpperCase();
    }

    public static void paintAvatar(
            Graphics2D g2,
            int x,
            int y,
            int size,
            String name) {

        Color[] colors = avatarColors(name);

        g2.setPaint(
                new GradientPaint(
                        x,
                        y,
                        colors[0],
                        x + size,
                        y + size,
                        colors[1]
                )
        );

        g2.fillOval(x, y, size, size);

        g2.setColor(Color.WHITE);

        g2.setFont(
                font(Font.BOLD, Math.max(10, size * 2 / 5))
        );

        FontMetrics fm = g2.getFontMetrics();

        String text = initials(name);

        g2.drawString(
                text,
                x + (size - fm.stringWidth(text)) / 2,
                y + (size - fm.getHeight()) / 2 + fm.getAscent()
        );
    }

    // ==========================================================
    // LOGO ỨNG DỤNG
    // ==========================================================

    public static JComponent createLogo(final int size) {

        JComponent logo = new JComponent() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                applyQuality(g2);

                int arc = Math.round(size * 0.34f);

                g2.setPaint(
                        new GradientPaint(
                                0,
                                0,
                                ACCENT_COLOR,
                                size,
                                size,
                                ACCENT_ALT
                        )
                );

                g2.fillRoundRect(0, 0, size, size, arc, arc);

                // Bong bóng chat màu trắng.
                float bw = size * 0.58f;
                float bh = size * 0.40f;
                float bx = (size - bw) / 2f;
                float by = size * 0.20f;

                g2.setColor(Color.WHITE);

                g2.fill(
                        new RoundRectangle2D.Float(
                                bx, by, bw, bh, size * 0.2f, size * 0.2f
                        )
                );

                Path2D tail = new Path2D.Float();

                tail.moveTo(bx + bw * 0.20f, by + bh - 1);
                tail.lineTo(bx + bw * 0.20f, by + bh + size * 0.15f);
                tail.lineTo(bx + bw * 0.46f, by + bh - 1);
                tail.closePath();

                g2.fill(tail);

                // Ba chấm bên trong bong bóng.
                float dot = size * 0.07f;
                float cy = by + bh / 2f - dot / 2f;

                g2.setColor(ACCENT_COLOR);

                for (int i = 0; i < 3; i++) {

                    float cx = size / 2f
                            + (i - 1) * dot * 2.1f
                            - dot / 2f;

                    g2.fill(
                            new java.awt.geom.Ellipse2D.Float(
                                    cx, cy, dot, dot
                            )
                    );
                }

                g2.dispose();
            }
        };

        Dimension d = new Dimension(size, size);

        logo.setPreferredSize(d);
        logo.setMinimumSize(d);
        logo.setMaximumSize(d);

        return logo;
    }

    // ==========================================================
    // THANH CUỘN MỎNG
    // ==========================================================

    public static void styleScrollPane(JScrollPane scroll) {

        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);

        JViewport viewport = scroll.getViewport();

        viewport.setOpaque(false);

        // Tránh vệt mờ khi cuộn trên nền trong suốt.
        viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        styleScrollBar(scroll.getVerticalScrollBar(), true);
        styleScrollBar(scroll.getHorizontalScrollBar(), false);

        scroll.getVerticalScrollBar().setUnitIncrement(18);

        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel() {
            {
                setOpaque(false);
            }
        });
    }

    private static void styleScrollBar(JScrollBar bar, boolean vertical) {

        bar.setUI(new SlimScrollBarUI());
        bar.setOpaque(false);
        bar.setBorder(null);

        bar.setPreferredSize(
                vertical
                        ? new Dimension(10, 0)
                        : new Dimension(0, 10)
        );
    }

    private static final class SlimScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            thumbColor = alpha(MUTED_COLOR, 90);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {

            JButton button = new JButton();

            Dimension zero = new Dimension(0, 0);

            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);

            return button;
        }

        @Override
        protected void paintTrack(
                Graphics g,
                JComponent c,
                Rectangle trackBounds) {
            // Không vẽ track, chỉ vẽ thumb.
        }

        @Override
        protected void paintThumb(
                Graphics g,
                JComponent c,
                Rectangle r) {

            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            applyQuality(g2);

            Color color = isDragging
                    ? alpha(ACCENT_COLOR, 210)
                    : isThumbRollover()
                            ? alpha(MUTED_COLOR, 170)
                            : alpha(MUTED_COLOR, 90);

            g2.setColor(color);

            boolean vertical =
                    scrollbar.getOrientation() == JScrollBar.VERTICAL;

            int inset = 2;

            g2.fillRoundRect(
                    r.x + (vertical ? inset : 0),
                    r.y + (vertical ? 0 : inset),
                    r.width - (vertical ? 2 * inset : 0),
                    r.height - (vertical ? 0 : 2 * inset),
                    8,
                    8
            );

            g2.dispose();
        }
    }
}