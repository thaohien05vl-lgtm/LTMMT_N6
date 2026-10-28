/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

/**
 * UITheme.java
 *
 * Chứa màu sắc, font chữ và cấu hình giao diện dùng chung.
 *
 * Mọi màn hình trong Phase 4 sử dụng cùng bộ theme,
 * giúp ứng dụng có giao diện đồng nhất.
 */
public final class UITheme {

    // =====================================================
    // MÀU NỀN
    // =====================================================

    // Nền tổng thể của ứng dụng.
    public static final Color APP_BG =
            new Color(16, 21, 37);

    // Nền sidebar.
    public static final Color SIDEBAR_BG =
            new Color(20, 27, 46);

    // Nền các panel và card.
    public static final Color CARD_BG =
            new Color(27, 35, 57);

    // Nền ô nhập liệu.
    public static final Color INPUT_BG =
            new Color(35, 44, 67);


    // =====================================================
    // MÀU CHỮ
    // =====================================================

    // Chữ chính.
    public static final Color TEXT_PRIMARY =
            new Color(244, 246, 255);

    // Chữ phụ, mô tả.
    public static final Color TEXT_SECONDARY =
            new Color(159, 170, 197);

    // Chữ mờ, gợi ý.
    public static final Color TEXT_MUTED =
            new Color(130, 144, 172);


    // =====================================================
    // MÀU NHẤN
    // =====================================================

    // Tím chính.
    public static final Color ACCENT =
            new Color(119, 103, 248);

    // Tím khi rê chuột lên nút.
    public static final Color ACCENT_HOVER =
            new Color(140, 126, 255);

    // Tím khi nút được nhấn.
    public static final Color ACCENT_PRESSED =
            new Color(91, 77, 215);

    // Màu báo thành công / đang online.
    public static final Color SUCCESS =
            new Color(52, 211, 153);

    // Màu báo lỗi.
    public static final Color DANGER =
            new Color(255, 107, 107);

    // Màu đường viền.
    public static final Color BORDER =
            new Color(52, 63, 89);


    // =====================================================
    // FONT CHỮ
    // =====================================================

    // Font Segoe UI thường có sẵn trên Windows
    // và hỗ trợ tiếng Việt tốt.
    public static final String FONT_FAMILY =
            "Segoe UI";

    public static final Font FONT_SMALL =
            new Font(FONT_FAMILY, Font.PLAIN, 12);

    public static final Font FONT_NORMAL =
            new Font(FONT_FAMILY, Font.PLAIN, 14);

    public static final Font FONT_MEDIUM =
            new Font(FONT_FAMILY, Font.BOLD, 14);

    public static final Font FONT_SUBTITLE =
            new Font(FONT_FAMILY, Font.BOLD, 18);

    public static final Font FONT_TITLE =
            new Font(FONT_FAMILY, Font.BOLD, 28);


    // Không cho tạo đối tượng UITheme.
    private UITheme() {
    }


    /**
     * Cấu hình Look and Feel cơ bản.
     *
     * Gọi một lần khi ứng dụng bắt đầu,
     * trước khi tạo JFrame.
     */
    public static void install() {

        try {

            // Dùng Look and Feel có sẵn trong Java.
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );

            // Font mặc định cho các component Swing.
            UIManager.put(
                    "Label.font",
                    FONT_NORMAL
            );

            UIManager.put(
                    "Button.font",
                    FONT_MEDIUM
            );

            UIManager.put(
                    "TextField.font",
                    FONT_NORMAL
            );

            UIManager.put(
                    "PasswordField.font",
                    FONT_NORMAL
            );

            UIManager.put(
                    "TextArea.font",
                    FONT_NORMAL
            );

            UIManager.put(
                    "ComboBox.font",
                    FONT_NORMAL
            );

        } catch (Exception e) {

            System.err.println(
                    "[UI] Khong the cai dat Look and Feel."
            );

            e.printStackTrace();
        }
    }
}
