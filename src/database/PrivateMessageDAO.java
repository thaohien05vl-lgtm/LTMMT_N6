/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import model.PrivateMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * PrivateMessageDAO:
 *
 * Chịu trách nhiệm quản lý và lưu trữ tin nhắn riêng tư 1-1
 * vào bảng `private_messages` trong SQLite.
 *
 * Tất cả truy vấn đều dùng PreparedStatement để đảm bảo an toàn.
 */
public class PrivateMessageDAO {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    /**
     * Lưu tin nhắn riêng vào Database.
     *
     * @param message đối tượng PrivateMessage cần lưu
     * @return đối tượng PrivateMessage đã có ID và sentAt từ DB
     * @throws SQLException nếu lưu thất bại
     */
    public PrivateMessage save(PrivateMessage message) throws SQLException {
        if (message == null) {
            throw new IllegalArgumentException("Tin nhắn riêng không được null.");
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được rỗng.");
        }
        if (message.getContent().length() > 500) {
            throw new IllegalArgumentException("Nội dung tin nhắn tối đa 500 ký tự.");
        }

        String sql = """
                INSERT INTO private_messages (
                    sender_id,
                    sender_username,
                    sender_display_name,
                    receiver_id,
                    receiver_username,
                    receiver_display_name,
                    content
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS)) {

                statement.setInt(1, message.getSenderId());
                statement.setString(2, message.getSenderUsername());
                statement.setString(3, message.getSenderDisplayName());
                statement.setInt(4, message.getReceiverId());
                statement.setString(5, message.getReceiverUsername());
                statement.setString(6, message.getReceiverDisplayName());
                statement.setString(7, message.getContent());

                int affected = statement.executeUpdate();
                if (affected != 1) {
                    throw new SQLException("Không thể lưu tin nhắn riêng tư.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("Không lấy được ID tin nhắn riêng vừa tạo.");
                    }
                }
            }

            // Đọc lại sent_at vừa được SQLite tạo
            String timeSql = """
                    SELECT sent_at
                    FROM private_messages
                    WHERE id = ?
                    """;

            try (PreparedStatement timeStatement = connection.prepareStatement(timeSql)) {
                timeStatement.setInt(1, message.getId());

                try (ResultSet rs = timeStatement.executeQuery()) {
                    if (rs.next()) {
                        message.setSentAt(rs.getString("sent_at"));
                    }
                }
            }
        }

        return message;
    }

    /**
     * Lấy lịch sử tin nhắn riêng tư giữa hai người dùng (xếp theo thứ tự thời gian tăng dần).
     *
     * @param username1 tên đăng nhập người thứ nhất
     * @param username2 tên đăng nhập người thứ hai
     * @param limit số lượng tin nhắn tối đa cần lấy
     * @return danh sách các tin nhắn đã gửi giữa hai người
     */
    public List<PrivateMessage> getHistory(String username1, String username2, int limit) throws SQLException {
        if (username1 == null || username2 == null) {
            return new ArrayList<>();
        }

        int fetchLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        String sql = """
                SELECT
                    id,
                    sender_id,
                    sender_username,
                    sender_display_name,
                    receiver_id,
                    receiver_username,
                    receiver_display_name,
                    content,
                    sent_at
                FROM (
                    SELECT
                        id,
                        sender_id,
                        sender_username,
                        sender_display_name,
                        receiver_id,
                        receiver_username,
                        receiver_display_name,
                        content,
                        sent_at
                    FROM private_messages
                    WHERE (sender_username = ? COLLATE NOCASE AND receiver_username = ? COLLATE NOCASE)
                       OR (sender_username = ? COLLATE NOCASE AND receiver_username = ? COLLATE NOCASE)
                    ORDER BY id DESC
                    LIMIT ?
                )
                ORDER BY id ASC
                """;

        List<PrivateMessage> result = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username1.trim());
            statement.setString(2, username2.trim());
            statement.setString(3, username2.trim());
            statement.setString(4, username1.trim());
            statement.setInt(5, fetchLimit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new PrivateMessage(
                            rs.getInt("id"),
                            rs.getInt("sender_id"),
                            rs.getString("sender_username"),
                            rs.getString("sender_display_name"),
                            rs.getInt("receiver_id"),
                            rs.getString("receiver_username"),
                            rs.getString("receiver_display_name"),
                            rs.getString("content"),
                            rs.getString("sent_at")
                    ));
                }
            }
        }

        return result;
    }

    /**
     * Đếm tổng số tin nhắn riêng giữa hai người.
     */
    public int countMessages(String username1, String username2) throws SQLException {
        if (username1 == null || username2 == null) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS total
                FROM private_messages
                WHERE (sender_username = ? COLLATE NOCASE AND receiver_username = ? COLLATE NOCASE)
                   OR (sender_username = ? COLLATE NOCASE AND receiver_username = ? COLLATE NOCASE)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username1.trim());
            statement.setString(2, username2.trim());
            statement.setString(3, username2.trim());
            statement.setString(4, username1.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }
}
