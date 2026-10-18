/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import javax.swing.JFrame;
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
    // CHẾ ĐỘ GIAO DIỆN: HONEY CHAT (HỒNG MÈO) & CHILL TALK (MÂY XANH)
    // ==========================================================

    public enum ThemeMode {
        HONEY_PINK("Honey Chat", "Nhóm trò chuyện ngọt ngào & ấm cúng", "🐾"),
        CHILL_BLUE("Chill Talk", "Không gian trò chuyện thư giãn & nhẹ nhàng", "☁️"),
        GAME_SQUAD("Game Squad", "Biệt đội chiến game & Voice rank", "🎮"),
        DIARY_GREEN("Nhật Ký Chúng Mình", "Lưu giữ khoảnh khắc thanh xuân & thiên nhiên", "🍃"),
        TEAM_DARK("Team Marketing", "Không gian làm việc chuyên nghiệp & tập trung", "📊"),
        STAR_NIGHT("Bạn Thân", "Ngắm sao đêm & bầu trời kỷ niệm lung linh", "🌙");

        private final String title;
        private final String subtitle;
        private final String icon;

        ThemeMode(String title, String subtitle, String icon) {
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getIcon() {
            return icon;
        }
    }

    private static ThemeMode currentTheme = ThemeMode.HONEY_PINK;
    private static boolean darkMode = false;

    private static final java.util.List<Runnable> themeListeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public static ThemeMode getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static boolean isHoneyPink() {
        return currentTheme == ThemeMode.HONEY_PINK;
    }

    public static boolean isChillBlue() {
        return currentTheme == ThemeMode.CHILL_BLUE;
    }

    public static boolean isGameSquad() {
        return currentTheme == ThemeMode.GAME_SQUAD;
    }

    public static boolean isDiaryGreen() {
        return currentTheme == ThemeMode.DIARY_GREEN;
    }

    public static boolean isTeamDark() {
        return currentTheme == ThemeMode.TEAM_DARK;
    }

    public static boolean isStarNight() {
        return currentTheme == ThemeMode.STAR_NIGHT;
    }

    public static void setTheme(ThemeMode theme) {
        if (theme != null && currentTheme != theme) {
            currentTheme = theme;
            updateColors();
            for (Runnable listener : themeListeners) {
                try {
                    listener.run();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void setDarkMode(boolean dark) {
        setTheme(dark ? ThemeMode.GAME_SQUAD : ThemeMode.HONEY_PINK);
    }

    public static void toggleTheme() {
        ThemeMode[] values = ThemeMode.values();
        int nextIndex = (currentTheme.ordinal() + 1) % values.length;
        setTheme(values[nextIndex]);
    }

    public static void addThemeListener(Runnable r) {
        if (r != null && !themeListeners.contains(r)) {
            themeListeners.add(r);
        }
    }

    public static void removeThemeListener(Runnable r) {
        if (r != null) {
            themeListeners.remove(r);
        }
    }

    // ==========================================================
    // BẢNG MÀU DÙNG CHUNG (CẬP NHẬT THEO THEME)
    // ==========================================================

    public static Color BACKGROUND_TOP = new Color(255, 241, 242);     // #FFF1F2 hồng phấn dịu
    public static Color BACKGROUND_BOTTOM = new Color(254, 226, 226);  // #FEE2E2 hoa đào
    public static Color CARD_COLOR = new Color(255, 255, 255, 248);
    public static Color INPUT_COLOR = new Color(255, 245, 246);
    public static Color ACCENT_COLOR = new Color(244, 63, 94);         // #F43F5E hồng hoa hồng ngọt ngào
    public static Color ACCENT_ALT = new Color(251, 113, 133);        // #FB7185 san hô đào
    public static Color ACCENT_PINK = new Color(244, 63, 94);
    public static Color ACCENT_LIGHT = new Color(251, 113, 133);
    public static Color SURFACE_COLOR = new Color(255, 241, 242);
    public static Color TEXT_COLOR = new Color(68, 40, 48);            // Nâu socola đậm thanh lịch
    public static Color MUTED_COLOR = new Color(159, 115, 126);        // Hồng đất dịu
    public static Color BORDER_COLOR = new Color(254, 205, 211);       // Hồng phấn viền
    public static Color SUCCESS_COLOR = new Color(16, 185, 129);
    public static Color ERROR_COLOR = new Color(244, 63, 94);
    public static Color BUBBLE_OTHER_COLOR = new Color(255, 255, 255);

    private static void updateColors() {
        switch (currentTheme) {
            case CHILL_BLUE:
                // Theme 2: Chill Talk (Mây Xanh Pastel)
                darkMode = false;
                BACKGROUND_TOP = new Color(236, 246, 255);      // #ECF6FF mây trời ban mai
                BACKGROUND_BOTTOM = new Color(219, 234, 254);   // #DBEAFE xanh baby pastel
                CARD_COLOR = new Color(255, 255, 255, 248);
                INPUT_COLOR = new Color(240, 247, 255);
                ACCENT_COLOR = new Color(59, 130, 246);         // #3B82F6 xanh thiên thanh
                ACCENT_ALT = new Color(56, 189, 248);          // #38BDF8 cyan trời trong
                ACCENT_PINK = new Color(96, 165, 250);          // #60A5FA
                ACCENT_LIGHT = new Color(59, 130, 246);
                SURFACE_COLOR = new Color(239, 246, 255);
                TEXT_COLOR = new Color(30, 41, 59);             // Slate navy
                MUTED_COLOR = new Color(100, 116, 139);
                BORDER_COLOR = new Color(191, 219, 254);
                SUCCESS_COLOR = new Color(16, 185, 129);
                ERROR_COLOR = new Color(239, 68, 68);
                BUBBLE_OTHER_COLOR = new Color(255, 255, 255);
                break;

            case GAME_SQUAD:
                // Theme 3: Game Squad (Tím than trầm, êm mắt, không chói)
                darkMode = true;
                BACKGROUND_TOP = new Color(20, 20, 36);         // Tím than tối dịu mắt
                BACKGROUND_BOTTOM = new Color(15, 14, 28);      // Đen tím sâu thẳm
                CARD_COLOR = new Color(28, 27, 48, 250);        // Card màu tối đồng nhất
                INPUT_COLOR = new Color(36, 35, 60);            // Ô nhập tối trầm
                ACCENT_COLOR = new Color(139, 92, 246);         // Tím dịu vừa phải (Violet), không bị gắt
                ACCENT_ALT = new Color(167, 139, 250);          // Tím lavender sáng
                ACCENT_PINK = new Color(124, 58, 237);
                ACCENT_LIGHT = new Color(196, 181, 253);
                SURFACE_COLOR = new Color(38, 36, 64);
                TEXT_COLOR = new Color(241, 243, 249);          // Chữ trắng kem dễ đọc
                MUTED_COLOR = new Color(148, 153, 184);         // Tím xám dịu
                BORDER_COLOR = new Color(60, 58, 92);           // Viền mảnh tối
                SUCCESS_COLOR = new Color(34, 197, 94);
                ERROR_COLOR = new Color(239, 68, 68);
                BUBBLE_OTHER_COLOR = new Color(36, 35, 62);     // Bong bóng người khác màu tối tiệp màu
                break;

            case DIARY_GREEN:
                // Theme 4: Nhật Ký Chúng Mình (Xanh Lá Thiên Nhiên & Gió Mát)
                darkMode = false;
                BACKGROUND_TOP = new Color(241, 248, 241);      // #F1F8F1 Xanh mầm lá non
                BACKGROUND_BOTTOM = new Color(230, 244, 230);   // #E6F4E6
                CARD_COLOR = new Color(255, 255, 255, 248);
                INPUT_COLOR = new Color(243, 249, 243);
                ACCENT_COLOR = new Color(34, 197, 94);          // #22C55E Xanh lục tươi
                ACCENT_ALT = new Color(74, 222, 128);          // #4ADE80
                ACCENT_PINK = new Color(134, 239, 172);         // #86EFAC Xanh pastel dịu
                ACCENT_LIGHT = new Color(22, 163, 74);
                SURFACE_COLOR = new Color(238, 247, 238);
                TEXT_COLOR = new Color(27, 43, 30);             // Xanh rêu đậm cổ điển
                MUTED_COLOR = new Color(101, 133, 106);         // Xanh xám lá
                BORDER_COLOR = new Color(187, 228, 192);        // Viền xanh lá nhạt
                SUCCESS_COLOR = new Color(22, 163, 74);
                ERROR_COLOR = new Color(239, 68, 68);
                BUBBLE_OTHER_COLOR = new Color(255, 255, 255);
                break;

            case TEAM_DARK:
                // Theme 5: Team Marketing (Đen than chì sang trọng, dịu mắt, chống chói)
                darkMode = true;
                BACKGROUND_TOP = new Color(22, 27, 34);         // Đen xám than mềm mại
                BACKGROUND_BOTTOM = new Color(15, 18, 23);      // Nền đen dịu mắt
                CARD_COLOR = new Color(30, 36, 46, 250);        // Card màu tối dịu
                INPUT_COLOR = new Color(38, 45, 58);            // Ô nhập màu tối
                ACCENT_COLOR = new Color(16, 185, 129);         // Xanh ngọc lục bảo (Emerald) thanh lịch
                ACCENT_ALT = new Color(52, 211, 153);
                ACCENT_PINK = new Color(16, 185, 129);
                ACCENT_LIGHT = new Color(110, 231, 183);
                SURFACE_COLOR = new Color(42, 50, 64);
                TEXT_COLOR = new Color(241, 245, 249);          // Chữ trắng bạc êm mắt
                MUTED_COLOR = new Color(148, 163, 184);         // Slate xám dịu
                BORDER_COLOR = new Color(55, 65, 81);           // Viền xám mảnh
                SUCCESS_COLOR = new Color(16, 185, 129);
                ERROR_COLOR = new Color(248, 113, 113);
                BUBBLE_OTHER_COLOR = new Color(36, 44, 56);     // Bong bóng người khác màu tối
                break;

            case STAR_NIGHT:
                // Theme 6: Bạn Thân (Bầu Trời Đêm Ánh Sao & Trăng Khuyết Huyền Ảo)
                darkMode = true;
                BACKGROUND_TOP = new Color(22, 21, 48);         // #161530 Tím than ngân hà
                BACKGROUND_BOTTOM = new Color(38, 32, 70);      // #262046 Dạ khúc ánh tím
                CARD_COLOR = new Color(34, 30, 64, 245);
                INPUT_COLOR = new Color(48, 42, 85);
                ACCENT_COLOR = new Color(139, 92, 246);         // #8B5CF6 Tím dạ quang
                ACCENT_ALT = new Color(244, 114, 182);         // #F472B6 Hồng sao băng
                ACCENT_PINK = new Color(236, 72, 153);
                ACCENT_LIGHT = new Color(196, 181, 253);
                SURFACE_COLOR = new Color(50, 44, 92);
                TEXT_COLOR = new Color(248, 250, 252);          // Trắng sao tinh tú
                MUTED_COLOR = new Color(167, 161, 206);         // Tím khói nhạt
                BORDER_COLOR = new Color(74, 66, 120);
                SUCCESS_COLOR = new Color(52, 211, 153);
                ERROR_COLOR = new Color(251, 113, 133);
                BUBBLE_OTHER_COLOR = new Color(46, 40, 82);
                break;

            case HONEY_PINK:
            default:
                // Theme 1: Honey Chat (Hồng Mèo Pastel)
                darkMode = false;
                BACKGROUND_TOP = new Color(255, 241, 242);      // #FFF1F2 hồng phấn dịu
                BACKGROUND_BOTTOM = new Color(254, 226, 226);   // #FEE2E2 hoa đào
                CARD_COLOR = new Color(255, 255, 255, 248);
                INPUT_COLOR = new Color(255, 245, 246);
                ACCENT_COLOR = new Color(244, 63, 94);          // #F43F5E hồng hoa hồng ngọt ngào
                ACCENT_ALT = new Color(251, 113, 133);         // #FB7185 san hô đào
                ACCENT_PINK = new Color(244, 63, 94);
                ACCENT_LIGHT = new Color(251, 113, 133);
                SURFACE_COLOR = new Color(255, 241, 242);
                TEXT_COLOR = new Color(68, 40, 48);             // Nâu socola đậm thanh lịch
                MUTED_COLOR = new Color(159, 115, 126);         // Hồng đất dịu
                BORDER_COLOR = new Color(254, 205, 211);        // Hồng phấn viền
                SUCCESS_COLOR = new Color(16, 185, 129);
                ERROR_COLOR = new Color(244, 63, 94);
                BUBBLE_OTHER_COLOR = new Color(255, 255, 255);
                break;
        }
    }

    private static final String FONT_FAMILY = "Segoe UI";

    // Vùng đệm để vẽ bóng đổ quanh card.
    private static final int CARD_SHADOW = 6;

    // Bảng màu avatar: mỗi người dùng nhận một cặp màu cố định đa dạng, rực rỡ
    private static final Color[][] AVATAR_PALETTE = {
        {new Color(139, 92, 246), new Color(236, 72, 153)},  // Tím sang hồng neon
        {new Color(6, 182, 212), new Color(59, 130, 246)},   // Cyan sang xanh biển
        {new Color(245, 158, 11), new Color(239, 68, 68)},   // Cam hoàng hôn sang đỏ
        {new Color(16, 185, 129), new Color(6, 182, 212)},   // Lục ngọc sang cyan
        {new Color(236, 72, 153), new Color(244, 63, 94)},   // Hồng đậm sang đỏ san hô
        {new Color(99, 102, 241), new Color(168, 85, 247)},  // Indigo sang tím tím
        {new Color(20, 184, 166), new Color(16, 185, 129)},  // Teal sang ngọc bích
        {new Color(249, 115, 22), new Color(236, 72, 153)}   // Cam san hô sang hồng
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
    // NỀN GRADIENT + VỆT SÁNG BỪNG (AURORA GLOW MESH)
    // ==========================================================

    public static JPanel createRootPanel() {

        RootPanel panel = new RootPanel();

        panel.setLayout(
                new java.awt.GridBagLayout()
        );

        panel.setOpaque(true);

        return panel;
    }

    public static final class RootPanel extends JPanel {

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

            // Vệt sáng Aurora đa tầng lung linh (Tím, Cyan, Hồng)
            int glowAlpha1 = isDarkMode() ? 50 : 35;
            int glowAlpha2 = isDarkMode() ? 45 : 30;
            int glowAlpha3 = isDarkMode() ? 40 : 25;

            paintGlow(
                    g2,
                    w * 0.15f,
                    h * 0.15f,
                    Math.max(w, h) * 0.65f,
                    alpha(ACCENT_COLOR, glowAlpha1),
                    w,
                    h
            );

            paintGlow(
                    g2,
                    w * 0.85f,
                    h * 0.85f,
                    Math.max(w, h) * 0.60f,
                    alpha(ACCENT_ALT, glowAlpha2),
                    w,
                    h
            );

            paintGlow(
                    g2,
                    w * 0.85f,
                    h * 0.15f,
                    Math.max(w, h) * 0.45f,
                    alpha(ACCENT_PINK, glowAlpha3),
                    w,
                    h
            );

            g2.dispose();
        }
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
    // KHUNG CARD BO GÓC CÓ ĐỔ BÓNG VÀ ĐƯỜNG VIỀN PHÁT SÁNG (GLASS EFFECT)
    // ==========================================================

    public static JPanel createCardPanel() {
        return createCardPanel(28, 26, 28, true);
    }

    public static JPanel createCardPanel(
            final int arc,
            int padV,
            int padH,
            final boolean shadow) {

        final int s = shadow ? CARD_SHADOW : 0;

        CardPanel panel = new CardPanel(arc, s);

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

    public static final class CardPanel extends JPanel {

        private final int arc;
        private final int s;

        public CardPanel(int arc, int s) {
            this.arc = arc;
            this.s = s;
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            applyQuality(g2);

            int x = s;
            int y = s;
            int w = getWidth() - 2 * s;
            int h = getHeight() - 2 * s;

            // Bóng đổ phát sáng mềm mại
            int shadowColorAlpha = isDarkMode() ? 40 : 18;
            for (int i = s; i > 0; i--) {
                g2.setColor(new Color(0, 0, 0, Math.min(255, shadowColorAlpha / i + 2)));
                g2.fillRoundRect(
                        x - i,
                        y - i + 2,
                        w + 2 * i,
                        h + 2 * i,
                        arc + i * 2,
                        arc + i * 2
                );
            }

            // Nền card bán trong suốt (Glassmorphism hiệu ứng mờ sang trọng)
            g2.setPaint(
                    new GradientPaint(
                            0,
                            y,
                            isDarkMode() ? new Color(30, 35, 62, 245) : new Color(255, 255, 255, 245),
                            w,
                            y + h,
                            CARD_COLOR
                    )
            );

            g2.fillRoundRect(x, y, w - 1, h - 1, arc, arc);

            // Đường viền Gradient ánh sáng Neon tinh tế quanh Card
            g2.setPaint(
                    new GradientPaint(
                            x,
                            y,
                            alpha(ACCENT_COLOR, isDarkMode() ? 130 : 90),
                            x + w,
                            y + h,
                            alpha(ACCENT_ALT, isDarkMode() ? 90 : 60)
                    )
            );

            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

            g2.dispose();
        }
    }

    // ==========================================================
    // LABEL CÓ KHẢ NĂNG TỰ ĐỘNG CẬP NHẬT MÀU THEO CHỦ ĐỀ
    // ==========================================================

    public static enum LabelKind {
        TITLE, SUBTITLE, FIELD, BRAND, MUTED, SECURITY
    }

    public static class DynamicLabel extends JLabel {
        private final LabelKind kind;

        public DynamicLabel(String text, LabelKind kind) {
            super(text);
            this.kind = kind;
            updateTextColor();
        }

        public LabelKind getKind() {
            return kind;
        }

        public void updateTextColor() {
            switch (kind) {
                case TITLE:
                    setForeground(TEXT_COLOR);
                    break;
                case SUBTITLE:
                    setForeground(MUTED_COLOR);
                    break;
                case FIELD:
                    setForeground(TEXT_COLOR);
                    break;
                case BRAND:
                    setForeground(ACCENT_COLOR);
                    break;
                case MUTED:
                    setForeground(MUTED_COLOR);
                    break;
                case SECURITY:
                    setForeground(new Color(16, 185, 129));
                    break;
                default:
                    setForeground(TEXT_COLOR);
                    break;
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            updateTextColor();
            super.paintComponent(g);
        }
    }

    public static JLabel title(String text) {

        DynamicLabel label = new DynamicLabel(text, LabelKind.TITLE);

        label.setFont(font(Font.BOLD, 28));

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        return label;
    }

    public static JLabel subtitle(String text) {

        DynamicLabel label = new DynamicLabel(text, LabelKind.SUBTITLE);

        label.setFont(font(Font.PLAIN, 13));

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        return label;
    }

    public static JLabel brand(String text) {

        DynamicLabel label = new DynamicLabel(text, LabelKind.BRAND);

        label.setFont(font(Font.BOLD, 12));

        label.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        return label;
    }

    public static JLabel fieldLabel(String text) {

        DynamicLabel label = new DynamicLabel(text, LabelKind.FIELD);

        label.setFont(font(Font.BOLD, 13));

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

        field.addFocusListener(new InputFocusListener(field, normal, focused));

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

    public static final class InputFocusListener extends FocusAdapter {

        private final JTextField field;
        private final Border normal;
        private final Border focused;

        public InputFocusListener(JTextField field, Border normal, Border focused) {
            this.field = field;
            this.normal = normal;
            this.focused = focused;
        }

        @Override
        public void focusGained(FocusEvent e) {
            field.setBorder(focused);
        }

        @Override
        public void focusLost(FocusEvent e) {
            field.setBorder(normal);
        }
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

            g2.setColor(backdrop != null ? backdrop : CARD_COLOR);
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
            setForeground(primary ? Color.WHITE : TEXT_COLOR);
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

            Color textColor = primary ? Color.WHITE : TEXT_COLOR;

            if (!isEnabled()) {

                g2.setColor(alpha(SURFACE_COLOR, 140));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                textColor = alpha(MUTED_COLOR, 140);

            } else if (primary) {

                Color a = ACCENT_COLOR;
                Color b = ACCENT_PINK;

                if (hover) {
                    a = mix(a, Color.WHITE, 0.20f);
                    b = mix(b, Color.WHITE, 0.20f);
                    // Hiệu ứng phát sáng nhẹ quanh nút khi hover
                    g2.setColor(alpha(ACCENT_COLOR, 60));
                    g2.drawRoundRect(-1, -1, w + 1, h + 1, arc + 2, arc + 2);
                }

                if (pressed) {
                    a = mix(a, Color.BLACK, 0.18f);
                    b = mix(b, Color.BLACK, 0.18f);
                }

                g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Đường phản quang kính bóng bẩy ở nửa trên của nút
                g2.setPaint(new GradientPaint(0, 1, new Color(255, 255, 255, 75), 0, h / 2f, new Color(255, 255, 255, 5)));
                g2.fillRoundRect(2, 2, w - 4, h / 2 - 1, arc - 2, arc - 2);

            } else {

                Color bg = SURFACE_COLOR;

                if (hover) {
                    bg = mix(bg, ACCENT_COLOR, 0.12f);
                }

                if (pressed) {
                    bg = mix(bg, Color.BLACK, 0.15f);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                g2.setColor(
                        hover
                                ? alpha(ACCENT_COLOR, 210)
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

        LogoComponent logo = new LogoComponent(size);

        Dimension d = new Dimension(size, size);

        logo.setPreferredSize(d);
        logo.setMinimumSize(d);
        logo.setMaximumSize(d);

        return logo;
    }

    public static final class LogoComponent extends JComponent {

        private final int size;

        public LogoComponent(int size) {
            this.size = size;
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            applyQuality(g2);

            int arc = Math.round(size * 0.34f);

            // Nền logo gradient tím - cyan sang hồng neon rực rỡ
            g2.setPaint(
                    new GradientPaint(
                            0,
                            0,
                            ACCENT_COLOR,
                            size,
                            size,
                            ACCENT_PINK
                    )
            );

            g2.fillRoundRect(0, 0, size, size, arc, arc);

            // Viền ánh sáng bóng bẩy quanh icon
            g2.setPaint(
                    new GradientPaint(
                            0, 0, new Color(255, 255, 255, 120),
                            size, size, new Color(255, 255, 255, 10)
                    )
            );
            g2.drawRoundRect(0, 0, size - 1, size - 1, arc, arc);

            if (isHoneyPink()) {
                // Logo Honey Chat: Nền hồng phấn mềm mại, vẽ bàn chân mèo (Paw) trắng đáng yêu
                float pawPadW = size * 0.44f;
                float pawPadH = size * 0.36f;
                float pawPadX = (size - pawPadW) / 2f;
                float pawPadY = size * 0.44f;

                g2.setColor(Color.WHITE);
                g2.fillOval(Math.round(pawPadX), Math.round(pawPadY), Math.round(pawPadW), Math.round(pawPadH));

                // 4 ngón đệm nhỏ
                float toeR = size * 0.16f;
                g2.fillOval(Math.round(size * 0.20f), Math.round(size * 0.24f), Math.round(toeR), Math.round(toeR));
                g2.fillOval(Math.round(size * 0.38f), Math.round(size * 0.16f), Math.round(toeR), Math.round(toeR));
                g2.fillOval(Math.round(size * 0.58f), Math.round(size * 0.22f), Math.round(toeR), Math.round(toeR));
                g2.fillOval(Math.round(size * 0.72f), Math.round(size * 0.34f), Math.round(toeR * 0.85f), Math.round(toeR * 0.85f));
            } else if (isChillBlue()) {
                // Logo Chill Talk: Nền xanh mây trời, vẽ đám mây trắng bồng bềnh
                float cw = size * 0.65f;
                float ch = size * 0.38f;
                float cx = (size - cw) / 2f;
                float cy = (size - ch) / 2f + size * 0.04f;

                g2.setColor(Color.WHITE);
                g2.fillOval(Math.round(cx), Math.round(cy + ch * 0.20f), Math.round(cw * 0.45f), Math.round(ch * 0.80f));
                g2.fillOval(Math.round(cx + cw * 0.25f), Math.round(cy), Math.round(cw * 0.52f), Math.round(ch));
                g2.fillOval(Math.round(cx + cw * 0.55f), Math.round(cy + ch * 0.25f), Math.round(cw * 0.45f), Math.round(ch * 0.75f));
                g2.fillRoundRect(Math.round(cx + cw * 0.1f), Math.round(cy + ch * 0.45f), Math.round(cw * 0.8f), Math.round(ch * 0.55f), 8, 8);
            } else if (isGameSquad()) {
                // Logo Game Squad: Tay cầm chơi game (Gamepad) phong cách neon tím
                g2.setColor(Color.WHITE);
                int gw = Math.round(size * 0.65f);
                int gh = Math.round(size * 0.44f);
                int gx = (size - gw) / 2;
                int gy = (size - gh) / 2;
                g2.fillRoundRect(gx, gy, gw, gh, 12, 12);
                // D-pad & nút bấm màu tím
                g2.setColor(ACCENT_COLOR);
                g2.fillRect(gx + 6, gy + gh / 2 - 2, 8, 4);
                g2.fillRect(gx + 8, gy + gh / 2 - 4, 4, 8);
                g2.fillOval(gx + gw - 13, gy + 7, 5, 5);
                g2.fillOval(gx + gw - 8, gy + 13, 5, 5);
            } else if (isDiaryGreen()) {
                // Logo Nhật Ký Chúng Mình: Chiếc lá xanh thiên nhiên
                g2.setColor(Color.WHITE);
                int lx = size / 2;
                int ly = size / 2;
                Path2D leaf = new Path2D.Float();
                leaf.moveTo(lx - size * 0.25f, ly + size * 0.25f);
                leaf.curveTo(lx - size * 0.25f, ly - size * 0.20f, lx + size * 0.10f, ly - size * 0.28f, lx + size * 0.28f, ly - size * 0.28f);
                leaf.curveTo(lx + size * 0.28f, ly + size * 0.10f, lx + size * 0.15f, ly + size * 0.25f, lx - size * 0.25f, ly + size * 0.25f);
                leaf.closePath();
                g2.fill(leaf);
                g2.setColor(ACCENT_COLOR);
                g2.setStroke(new java.awt.BasicStroke(1.4f));
                g2.drawLine(Math.round(lx - size * 0.22f), Math.round(ly + size * 0.22f), Math.round(lx + size * 0.22f), Math.round(ly - size * 0.22f));
            } else if (isTeamDark()) {
                // Logo Team Marketing: Biểu đồ cột phân tích (Analytics Bars)
                g2.setColor(Color.WHITE);
                int bx = Math.round(size * 0.24f);
                int by = Math.round(size * 0.72f);
                int bw = Math.round(size * 0.12f);
                g2.fillRoundRect(bx, by - Math.round(size * 0.22f), bw, Math.round(size * 0.22f), 3, 3);
                g2.fillRoundRect(bx + bw + 3, by - Math.round(size * 0.38f), bw, Math.round(size * 0.38f), 3, 3);
                g2.fillRoundRect(bx + (bw + 3) * 2, by - Math.round(size * 0.50f), bw, Math.round(size * 0.50f), 3, 3);
            } else if (isStarNight()) {
                // Logo Bạn Thân: Trăng lưỡi liềm vàng và ngôi sao lung linh
                g2.setColor(new Color(253, 224, 71));
                int mx = Math.round(size * 0.24f);
                int my = Math.round(size * 0.24f);
                int ms = Math.round(size * 0.52f);
                Area moon = new Area(new java.awt.geom.Ellipse2D.Float(mx, my, ms, ms));
                moon.subtract(new Area(new java.awt.geom.Ellipse2D.Float(mx + ms * 0.35f, my - ms * 0.10f, ms * 0.85f, ms * 0.85f)));
                g2.fill(moon);
                g2.setColor(Color.WHITE);
                g2.fillOval(Math.round(size * 0.70f), Math.round(size * 0.32f), 5, 5);
            }

            g2.dispose();
        }
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

        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new TransparentCorner());
    }

    public static final class TransparentCorner extends JPanel {

        public TransparentCorner() {
            setOpaque(false);
        }
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

    // ==========================================================
    // ÂM THANH THÔNG BÁO TIN NHẮN (GIỐNG FACEBOOK MESSENGER "DING-DONG")
    // ==========================================================

    private static volatile byte[] cachedNotificationAudio = null;
    private static volatile javax.sound.sampled.AudioFormat cachedAudioFormat = null;

    /**
     * Phát âm thanh thông báo tin nhắn êm tai, trong trẻo như Facebook Messenger ("ding-dong" 2 nốt).
     * Tạo trực tiếp bằng PCM Wave synthesis thông qua javax.sound.sampled,
     * hoạt động 100% độc lập không cần file mp3/wav ngoài.
     */
    public static void playMessageNotificationSound() {
        new Thread(() -> {
            try {
                if (cachedNotificationAudio == null) {
                    float sampleRate = 44100f;
                    int totalDurationMs = 280;
                    int totalSamples = (int) (sampleRate * totalDurationMs / 1000f);
                    byte[] audioData = new byte[totalSamples];

                    // Facebook Messenger Sound: 2 nốt tinh tế F#6 (1480Hz) nối tiếp C#7 (2093Hz)
                    // Nốt 1: ~75ms, Nốt 2: ~200ms
                    int note1Samples = (int) (sampleRate * 0.075f);
                    double freq1 = 1480.0;
                    double freq2 = 2093.0;

                    for (int i = 0; i < totalSamples; i++) {
                        double time = i / (double) sampleRate;
                        double sample;

                        if (i < note1Samples) {
                            // Nốt thứ nhất
                            double env = Math.sin(Math.PI * i / note1Samples);
                            sample = Math.sin(2.0 * Math.PI * freq1 * time) * env * 0.75;
                            // Hòa âm nhẹ
                            sample += Math.sin(4.0 * Math.PI * freq1 * time) * env * 0.15;
                        } else {
                            // Nốt thứ hai ngân vang mượt mà
                            int note2Index = i - note1Samples;
                            int note2Length = totalSamples - note1Samples;
                            double progress = (double) note2Index / note2Length;
                            // Attack cực nhanh (10%), Decay mượt dần về 0 theo hàm mũ
                            double env = progress < 0.1
                                    ? (progress / 0.1)
                                    : Math.exp(-4.5 * (progress - 0.1));
                            sample = Math.sin(2.0 * Math.PI * freq2 * time) * env * 0.85;
                            // Hòa âm ngọt
                            sample += Math.sin(4.0 * Math.PI * freq2 * time) * env * 0.20;
                        }

                        // Giới hạn biên độ 8-bit PCM signed (-128 .. 127)
                        int intVal = (int) Math.round(sample * 110.0);
                        audioData[i] = (byte) Math.max(-128, Math.min(127, intVal));
                    }

                    cachedNotificationAudio = audioData;
                    cachedAudioFormat = new javax.sound.sampled.AudioFormat(sampleRate, 8, 1, true, false);
                }

                javax.sound.sampled.DataLine.Info info =
                        new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class, cachedAudioFormat);
                try (javax.sound.sampled.SourceDataLine line = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info)) {
                    line.open(cachedAudioFormat);
                    line.start();
                    line.write(cachedNotificationAudio, 0, cachedNotificationAudio.length);
                    line.drain();
                }
            } catch (Exception ignored) {
                // Nếu hệ điều hành không bật audio hoặc không có soundcard, không báo lỗi
            }
        }, "NotificationSoundThread").start();
    }

    // ==========================================================
    // NÚT CHUYỂN ĐỔI CHỦ ĐỀ: HONEY CHAT (🐾) ↔ CHILL TALK (☁️)
    // ==========================================================

    public static JButton createThemeToggleButton(final JFrame parentFrame) {
        final JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQuality(g2);

                int w = getWidth();
                int h = getHeight();
                int arc = h; // bo tròn dạng pill/capsule

                boolean hover = getModel().isRollover();
                boolean isPink = isHoneyPink();

                // Nền pill bóng bẩy mềm mại
                if (isDarkMode()) {
                    g2.setPaint(new GradientPaint(
                            0, 0, hover ? alpha(SURFACE_COLOR, 230) : alpha(CARD_COLOR, 230),
                            w, h, hover ? alpha(CARD_COLOR, 230) : alpha(INPUT_COLOR, 230)
                    ));
                } else if (isPink) {
                    g2.setPaint(new GradientPaint(
                            0, 0, hover ? new Color(255, 235, 238) : new Color(255, 245, 247),
                            w, h, hover ? new Color(254, 215, 226) : new Color(254, 226, 232)
                    ));
                } else {
                    g2.setPaint(new GradientPaint(
                            0, 0, hover ? new Color(230, 243, 255) : new Color(240, 248, 255),
                            w, h, hover ? new Color(210, 232, 255) : new Color(225, 240, 255)
                    ));
                }

                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Viền phát sáng Gradient (Accent coral rose / sky blue)
                g2.setPaint(new GradientPaint(
                        0, 0, alpha(ACCENT_COLOR, hover ? 230 : 160),
                        w, h, alpha(ACCENT_ALT, hover ? 210 : 130)
                ));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // Đường phản quang kính đỉnh trên
                g2.setPaint(new GradientPaint(0, 1, new Color(255, 255, 255, 120), w, 1, new Color(255, 255, 255, 20)));
                g2.drawLine(arc / 2, 1, w - arc / 2, 1);

                // Vẽ icon theo Theme hiện tại
                int ix = 12;
                int iy = h / 2;

                if (isHoneyPink()) {
                    // Vẽ dấu chân mèo đáng yêu (Paw print)
                    g2.setColor(new Color(244, 63, 94));
                    g2.fillOval(ix, iy - 2, 11, 9);
                    g2.fillOval(ix - 1, iy - 7, 4, 4);
                    g2.fillOval(ix + 3, iy - 9, 4, 4);
                    g2.fillOval(ix + 8, iy - 7, 4, 4);
                } else if (isChillBlue()) {
                    // Vẽ đám mây bồng bềnh xinh xắn (Cloud)
                    g2.setColor(new Color(59, 130, 246));
                    int cx = ix;
                    int cy = iy - 5;
                    g2.fillOval(cx + 2, cy + 2, 8, 8);
                    g2.fillOval(cx + 7, cy, 9, 9);
                    g2.fillOval(cx + 12, cy + 3, 7, 7);
                    g2.fillRoundRect(cx + 2, cy + 4, 16, 7, 4, 4);
                } else if (isGameSquad()) {
                    // Gamepad tím
                    g2.setColor(new Color(168, 85, 247));
                    g2.fillRoundRect(ix, iy - 6, 16, 11, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(ix + 2, iy - 2, 4, 2);
                    g2.fillOval(ix + 11, iy - 3, 3, 3);
                } else if (isDiaryGreen()) {
                    // Chiếc lá xanh
                    g2.setColor(new Color(34, 197, 94));
                    g2.fillOval(ix + 1, iy - 6, 14, 12);
                } else if (isTeamDark()) {
                    // Biểu đồ cột
                    g2.setColor(new Color(16, 185, 129));
                    g2.fillRect(ix + 1, iy - 1, 3, 7);
                    g2.fillRect(ix + 6, iy - 4, 3, 10);
                    g2.fillRect(ix + 11, iy - 7, 3, 13);
                } else if (isStarNight()) {
                    // Trăng sao
                    g2.setColor(new Color(253, 224, 71));
                    Area m = new Area(new java.awt.geom.Ellipse2D.Float(ix, iy - 6, 12, 12));
                    m.subtract(new Area(new java.awt.geom.Ellipse2D.Float(ix + 4, iy - 7, 10, 10)));
                    g2.fill(m);
                }

                // Chữ nhãn hiển thị theme hiện tại
                g2.setFont(font(Font.BOLD, 12));
                g2.setColor(TEXT_COLOR);
                FontMetrics fm = g2.getFontMetrics();
                String text = currentTheme.getTitle();
                if (text.length() > 14) {
                    text = text.substring(0, 12) + "...";
                }
                int tx = ix + 19;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                // Vẽ mũi tên trỏ xuống đẹp mắt (Chevron ▼)
                g2.setColor(MUTED_COLOR);
                int arrowX = w - 16;
                int arrowY = h / 2 - 2;
                g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.drawLine(arrowX, arrowY, arrowX + 4, arrowY + 4);
                g2.drawLine(arrowX + 4, arrowY + 4, arrowX + 8, arrowY);

                g2.dispose();
            }
        };

        button.setPreferredSize(new Dimension(145, 36));
        button.setMinimumSize(new Dimension(135, 36));
        button.setMaximumSize(new Dimension(165, 36));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Bấm để chọn 1 trong 6 chủ đề giao diện");

        button.addActionListener(e -> {
            final boolean isDark = isDarkMode();
            final Color popupBg = isDark ? new Color(26, 25, 42) : new Color(255, 255, 255);
            final Color popupBorder = isDark ? new Color(75, 70, 110) : BORDER_COLOR;
            final Color itemTextNormal = isDark ? new Color(225, 230, 245) : new Color(30, 41, 59);
            final Color itemTextSelected = isDark ? Color.WHITE : ACCENT_COLOR;

            javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    applyQuality(g2);
                    g2.setColor(popupBg);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    g2.setColor(popupBorder);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    g2.dispose();
                }
            };
            menu.setOpaque(false);
            menu.setBorder(new EmptyBorder(8, 8, 8, 8));
            menu.setBackground(popupBg);

            for (ThemeMode tm : ThemeMode.values()) {
                final ThemeMode targetMode = tm;
                final boolean isCurrent = (currentTheme == targetMode);
                final String itemTitle = targetMode.getTitle();

                javax.swing.JMenuItem item = new javax.swing.JMenuItem(itemTitle) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        applyQuality(g2);
                        int w = getWidth();
                        int h = getHeight();

                        boolean hover = isArmed();

                        // Nền hover và active nổi bật
                        if (hover || isCurrent) {
                            if (isDark) {
                                g2.setColor(hover ? new Color(60, 52, 98) : new Color(42, 38, 70));
                            } else {
                                g2.setColor(hover ? alpha(ACCENT_COLOR, 50) : alpha(ACCENT_COLOR, 25));
                            }
                            g2.fillRoundRect(0, 0, w, h, 10, 10);
                        }

                        // Vẽ chữ thủ công để đảm bảo 100% nổi bật, không bị LookAndFeel đè trắng
                        g2.setFont(font(isCurrent ? Font.BOLD : Font.PLAIN, 13));
                        g2.setColor(isCurrent ? itemTextSelected : (hover ? (isDark ? Color.WHITE : TEXT_COLOR) : itemTextNormal));
                        FontMetrics fm = g2.getFontMetrics();
                        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(itemTitle, 14, ty);

                        // Vẽ chấm tròn chỉ báo theme đang chọn ở bên phải
                        if (isCurrent) {
                            g2.setColor(ACCENT_COLOR);
                            g2.fillOval(w - 20, h / 2 - 4, 8, 8);
                        }

                        g2.dispose();
                    }
                };
                item.setFont(font(isCurrent ? Font.BOLD : Font.PLAIN, 13));
                item.setOpaque(false);
                item.setPreferredSize(new Dimension(175, 34));
                item.setBorder(new EmptyBorder(6, 14, 6, 28));
                item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                item.addActionListener(ev -> {
                    setTheme(targetMode);
                    if (parentFrame != null) {
                        refreshContainer(parentFrame);
                    }
                });

                menu.add(item);
            }

            menu.show(button, 0, button.getHeight() + 4);
        });

        return button;
    }

    /**
     * Cập nhật đệ quy màu sắc và vẽ lại toàn bộ cây component khi đổi theme.
     */
    public static void refreshContainer(Component c) {
        if (c == null) return;

        if (c instanceof DynamicLabel) {
            ((DynamicLabel) c).updateTextColor();
        } else if (c instanceof JTextField) {
            JTextField tf = (JTextField) c;
            tf.setBackground(INPUT_COLOR);
            tf.setForeground(TEXT_COLOR);
            tf.setCaretColor(ACCENT_LIGHT);
            tf.setSelectedTextColor(TEXT_COLOR);
        } else if (c instanceof JLabel) {
            JLabel lbl = (JLabel) c;
            Color fg = lbl.getForeground();
            // Nếu là nhãn thông báo thành công hoặc lỗi hoặc bảo mật đặc biệt
            if (fg != null && (fg.equals(SUCCESS_COLOR) || fg.equals(ERROR_COLOR))) {
                // giữ nguyên
            } else if (fg != null && (fg.getRGB() == new Color(110, 231, 183).getRGB() || fg.getRGB() == new Color(16, 185, 129).getRGB())) {
                lbl.setForeground(isDarkMode() ? new Color(110, 231, 183) : new Color(16, 185, 129));
            } else {
                // Các nhãn tĩnh khác: cập nhật theo TEXT_COLOR hoặc MUTED_COLOR
                if (lbl.getFont() != null && lbl.getFont().isBold()) {
                    lbl.setForeground(TEXT_COLOR);
                } else {
                    lbl.setForeground(MUTED_COLOR);
                }
            }
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                refreshContainer(child);
            }
        }
        c.repaint();
    }
}