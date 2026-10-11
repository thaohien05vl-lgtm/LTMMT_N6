/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

/**
 * RoundedButton.java
 *
 * Nút Swing tùy chỉnh:
 * - Góc bo tròn.
 * - Màu thay đổi khi rê chuột.
 * - Con trỏ chuột dạng bàn tay.
 * - Hỗ trợ màu riêng cho từng loại nút.
 */
public class RoundedButton extends JButton {

    private static final long serialVersionUID = 1L;

    // Màu nền mặc định.
    private final Color normalColor;

    // Màu khi rê chuột.
    private final Color hoverColor;

    // Màu khi nhấn.
    private final Color pressedColor;

    // Bán kính bo góc.
    private final int cornerRadius;

    // Theo dõi con trỏ chuột.
    private boolean hovered = false;


    /**
     * Constructor đầy đủ.
     */
    public RoundedButton(
            String text,
            Color normalColor,
            Color hoverColor,
            Color pressedColor,
            Color foregroundColor) {

        super(text);

        this.normalColor = normalColor;
        this.hoverColor = hoverColor;
        this.pressedColor = pressedColor;

        this.cornerRadius = 14;

        setForeground(foregroundColor);

        setFont(UITheme.FONT_MEDIUM);

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);

        setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        setBorder(
                javax.swing.BorderFactory.createEmptyBorder(
                        12, 18, 12, 18
                )
        );

        setRolloverEnabled(true);

        // Theo dõi chuột để đổi màu.
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {

                hovered = true;

                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {

                hovered = false;

                repaint();
            }
        });
    }


    /**
     * Constructor tiện dụng cho nút chính.
     */
    public RoundedButton(String text) {

        this(
                text,
                UITheme.ACCENT,
                UITheme.ACCENT_HOVER,
                UITheme.ACCENT_PRESSED,
                Color.WHITE
        );
    }


    /**
     * Vẽ nền bo tròn trước khi Swing vẽ chữ.
     */
    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 =
                (Graphics2D) g.create();

        // Khử răng cưa để góc bo mượt hơn.
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color background = normalColor;

        if (!isEnabled()) {

            background = UITheme.BORDER;

        } else if (getModel().isPressed()) {

            background = pressedColor;

        } else if (
                hovered
                || getModel().isRollover()
        ) {

            background = hoverColor;
        }

        g2.setColor(background);

        g2.fillRoundRect(
                0,
                0,
                getWidth(),
                getHeight(),
                cornerRadius,
                cornerRadius
        );

        g2.dispose();

        // Để JButton tự vẽ nội dung chữ.
        super.paintComponent(g);
    }
}
