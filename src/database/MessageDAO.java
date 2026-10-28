/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;
import model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MessageDAO.java
 *
 * DAO quản lý dữ liệu tin nhắn trong SQLite.
 *
 * Chức năng:
 * 1. Lưu tin nhắn.
 * 2. Lấy các tin nhắn mới nhất.
 * 3. Phân trang lịch sử theo ID.
 * 4. Tìm kiếm tin nhắn theo từ khóa.
 * 5. Đếm tin nhắn trong một phòng.
 *
 * Các truy vấn đều sử dụng PreparedStatement
 * để tránh nối trực tiếp dữ liệu người dùng vào SQL.
 */

public class MessageDAO {
    // Nội dung hiển thị sau khi tin nhắn được thu hồi.
    public static final String RECALLED_CONTENT = "[Tin nhắn đã được thu hồi]";

   // Số tin nhắn tối đa trong một trang.
    private static final int DEFAULT_PAGE_SIZE = 50;

    // Giới hạn số tin nhắn tối đa cho một lần truy vấn.
    private static final int MAX_PAGE_SIZE = 100;


    // =====================================================
    // 1. LƯU TIN NHẮN
    // =====================================================

    /**
     * Lưu tin nhắn vào Database.
     *
     * @param message tin nhắn cần lưu
     * @return đối tượng tin nhắn đã có ID và thời gian
     * @throws SQLException nếu không thể lưu
     */
    public Message saveMessage(Message message)
            throws SQLException {

        // Kiểm tra dữ liệu trước khi ghi Database.
        validateMessage(message);

        String sql = """
                INSERT INTO messages (
                    room_id,
                    sender_id,
                    sender_username,
                    sender_display_name,
                    content,
                    reply_to_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        // Dùng cùng một Connection cho thao tác INSERT
        // và đọc lại ID vừa được SQLite tạo.
        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 sql,
                                 Statement.RETURN_GENERATED_KEYS)) {

                statement.setInt(1, message.getRoomId());
                statement.setInt(2, message.getSenderId());
                statement.setString(
                        3,
                        message.getSenderUsername()
                );
                statement.setString(
                        4,
                        message.getSenderDisplayName()
                );
                statement.setString(
                        5,
                        message.getContent()
                );
                if (message.getReplyToId() > 0) {
                    statement.setInt(6, message.getReplyToId());
                } else {
                    statement.setNull(6, java.sql.Types.INTEGER);
                }

                int affectedRows = statement.executeUpdate();

                if (affectedRows != 1) {
                    throw new SQLException(
                            "Khong the luu tin nhan."
                    );
                }

                // Đọc ID vừa được SQLite tạo.
                try (ResultSet keys =
                             statement.getGeneratedKeys()) {

                    if (keys.next()) {
                        message.setId(keys.getInt(1));
                    } else {
                        // Phương án dự phòng dành cho SQLite JDBC.
                        try (Statement idStatement =
                                     connection.createStatement();
                             ResultSet idResult =
                                     idStatement.executeQuery(
                                             "SELECT last_insert_rowid()")) {

                            if (idResult.next()) {
                                message.setId(
                                        idResult.getInt(1)
                                );
                            } else {
                                throw new SQLException(
                                        "Khong lay duoc ID tin nhan."
                                );
                            }
                        }
                    }
                }
            }

            // Đọc lại thời gian do Database tạo.
            String timeSql = """
                    SELECT sent_at
                    FROM messages
                    WHERE id = ?
                    """;

            try (PreparedStatement timeStatement =
                         connection.prepareStatement(timeSql)) {

                timeStatement.setInt(1, message.getId());

                try (ResultSet rs =
                             timeStatement.executeQuery()) {

                    if (rs.next()) {
                        message.setSentAt(
                                rs.getString("sent_at")
                        );
                    }
                }
            }
        }

        return message;
    }


    // =====================================================
    // 2. LẤY CÁC TIN NHẮN MỚI NHẤT
    // =====================================================

    /**
     * Lấy các tin nhắn mới nhất của một phòng.
     *
     * Ví dụ:
     * - Database có 200 tin.
     * - Lấy 50 tin có ID lớn nhất.
     * - Sau đó sắp xếp 50 tin theo ID tăng dần
     *   để giao diện hiển thị từ cũ đến mới.
     *
     * @param roomId ID phòng cần đọc
     * @return danh sách tin nhắn theo thứ tự cũ đến mới
     * @throws SQLException nếu truy vấn lỗi
     */
    public List<Message> findLatestMessages(int roomId)
            throws SQLException {

        return findLatestMessages(
                roomId,
                DEFAULT_PAGE_SIZE
        );
    }


    /**
     * Lấy số lượng tin nhắn mới nhất theo yêu cầu.
     *
     * @param roomId ID phòng
     * @param pageSize số tin cần lấy
     * @return danh sách tin nhắn
     */
    public List<Message> findLatestMessages(
            int roomId,
            int pageSize) throws SQLException {

        validateRoomId(roomId);
        validatePageSize(pageSize);

        String sql = """
                SELECT
                    m.id,
                    m.room_id,
                    m.sender_id,
                    m.sender_username,
                    m.sender_display_name,
                    m.content,
                    m.sent_at,
                    m.reply_to_id,
                    orig.sender_display_name AS reply_sender_display_name,
                    orig.content AS reply_snippet
                FROM (
                    SELECT
                        id,
                        room_id,
                        sender_id,
                        sender_username,
                        sender_display_name,
                        content,
                        sent_at,
                        reply_to_id
                    FROM messages
                    WHERE room_id = ?
                    ORDER BY id DESC
                    LIMIT ?
                ) m
                LEFT JOIN messages orig ON m.reply_to_id = orig.id
                ORDER BY m.id ASC
                """;

        List<Message> messages = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, roomId);
            statement.setInt(2, pageSize);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {
                    messages.add(mapRowToMessage(rs));
                }
            }
        }

        return messages;
    }


    // =====================================================
    // 3. PHÂN TRANG: LẤY CÁC TIN CŨ HƠN
    // =====================================================

    /**
     * Lấy một trang tin nhắn cũ hơn một ID cho trước.
     *
     * Ví dụ:
     * - Tin đang hiển thị có ID nhỏ nhất là 101.
     * - Gọi findOlderMessages(roomId, 101, 50).
     * - DAO lấy tối đa 50 tin có ID < 101.
     *
     * Các tin trả về được sắp xếp từ cũ đến mới.
     *
     * @param roomId ID phòng
     * @param beforeMessageId ID mốc
     * @param pageSize số tin cần lấy
     * @return một trang tin nhắn cũ hơn
     */
    public List<Message> findOlderMessages(
            int roomId,
            int beforeMessageId,
            int pageSize) throws SQLException {

        validateRoomId(roomId);

        if (beforeMessageId <= 0) {
            throw new IllegalArgumentException(
                    "beforeMessageId phai lon hon 0."
            );
        }

        validatePageSize(pageSize);

        String sql = """
                SELECT
                    m.id,
                    m.room_id,
                    m.sender_id,
                    m.sender_username,
                    m.sender_display_name,
                    m.content,
                    m.sent_at,
                    m.reply_to_id,
                    orig.sender_display_name AS reply_sender_display_name,
                    orig.content AS reply_snippet
                FROM (
                    SELECT
                        id,
                        room_id,
                        sender_id,
                        sender_username,
                        sender_display_name,
                        content,
                        sent_at,
                        reply_to_id
                    FROM messages
                    WHERE room_id = ?
                      AND id < ?
                    ORDER BY id DESC
                    LIMIT ?
                ) m
                LEFT JOIN messages orig ON m.reply_to_id = orig.id
                ORDER BY m.id ASC
                """;

        List<Message> messages = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, roomId);
            statement.setInt(2, beforeMessageId);
            statement.setInt(3, pageSize);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {
                    messages.add(mapRowToMessage(rs));
                }
            }
        }

        return messages;
    }


    // =====================================================
    // 4. TÌM KIẾM TIN NHẮN
    // =====================================================

    /**
     * Tìm tin nhắn theo từ khóa trong một phòng.
     *
     * Tìm không phân biệt chữ hoa/chữ thường theo
     * hành vi LIKE mặc định của SQLite đối với ASCII.
     *
     * Các ký tự %, _ và \ được escape để chúng được
     * hiểu là ký tự cần tìm, không phải wildcard SQL.
     *
     * @param roomId ID phòng
     * @param keyword từ khóa
     * @param pageSize số kết quả tối đa
     * @return danh sách tin nhắn phù hợp
     */
    public List<Message> searchMessages(
            int roomId,
            String keyword,
            int pageSize) throws SQLException {

        validateRoomId(roomId);
        validatePageSize(pageSize);

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "Tu khoa tim kiem khong duoc de trong."
            );
        }

        // Escape các ký tự đặc biệt của LIKE.
        String escapedKeyword = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        String searchPattern = "%" + escapedKeyword + "%";

        String sql = """
                SELECT
                    id,
                    room_id,
                    sender_id,
                    sender_username,
                    sender_display_name,
                    content,
                    sent_at
                FROM messages
                WHERE room_id = ?
                  AND content LIKE ? ESCAPE '\\'
                ORDER BY id DESC
                LIMIT ?
                """;

        List<Message> messages = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, roomId);
            statement.setString(2, searchPattern);
            statement.setInt(3, pageSize);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {
                    messages.add(mapRowToMessage(rs));
                }
            }
        }

        // Truy vấn theo ID giảm dần để lấy kết quả mới nhất.
        // Đảo danh sách để hiển thị theo thứ tự cũ đến mới.
        Collections.reverse(messages);

        return messages;
    }


    // =====================================================
    // 5. ĐẾM TIN NHẮN TRONG MỘT PHÒNG
    // =====================================================

    /**
     * Đếm số tin nhắn hiện có trong phòng.
     *
     * @param roomId ID phòng
     * @return tổng số tin nhắn
     */
    public int countMessagesByRoom(int roomId)
            throws SQLException {

        validateRoomId(roomId);

        String sql = """
                SELECT COUNT(*)
                FROM messages
                WHERE room_id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, roomId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // =====================================================
    // 5.2. TÌM TIN NHẮN THEO ID & THU HỒI TIN NHẮN
    // =====================================================

    /**
     * Tìm tin nhắn theo ID.
     */
    public Message findById(int id) throws SQLException {
        String sql = """
                SELECT
                    m.id,
                    m.room_id,
                    m.sender_id,
                    m.sender_username,
                    m.sender_display_name,
                    m.content,
                    m.sent_at,
                    m.reply_to_id,
                    orig.sender_display_name AS reply_sender_display_name,
                    orig.content AS reply_snippet
                FROM messages m
                LEFT JOIN messages orig ON m.reply_to_id = orig.id
                WHERE m.id = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMessage(rs);
                }
            }
        }

        return null;
    }

    /**
     * Lấy số giây đã trôi qua kể từ khi tin nhắn được gửi (tính chuẩn UTC theo SQLite).
     */
    public int getMessageAgeSeconds(int messageId) throws SQLException {
        String sql = """
                SELECT (strftime('%s', 'now') - strftime('%s', sent_at)) AS diff_sec
                FROM messages
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, messageId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("diff_sec");
                }
            }
        }

        return -1;
    }

    /**
     * Thu hồi tin nhắn trong cơ sở dữ liệu.
     * - Cập nhật nội dung thành "[Tin nhắn đã được thu hồi]"
     * - Xóa toàn bộ cảm xúc liên quan trong message_reactions
     * - Sử dụng Transaction để đảm bảo tính toàn vẹn.
     *
     * @param messageId ID tin nhắn cần thu hồi
     * @param senderId  ID người gửi (phải trùng khớp để đảm bảo chính chủ)
     * @return true nếu thu hồi thành công, false nếu không hợp lệ
     */
    public boolean recallMessage(int messageId, int senderId) throws SQLException {
        String updateSql = """
                UPDATE messages
                SET content = ?
                WHERE id = ? AND sender_id = ? AND content <> ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            int updated = 0;
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, RECALLED_CONTENT);
                statement.setInt(2, messageId);
                statement.setInt(3, senderId);
                statement.setString(4, RECALLED_CONTENT);

                updated = statement.executeUpdate();
            }

            if (updated > 0) {
                // Xóa cảm xúc của tin nhắn đã thu hồi
                String deleteReactionsSql = """
                        DELETE FROM message_reactions
                        WHERE message_id = ?
                        """;

                try (PreparedStatement rxStmt = connection.prepareStatement(deleteReactionsSql)) {
                    rxStmt.setInt(1, messageId);
                    rxStmt.executeUpdate();
                }

                connection.commit();
                return true;
            } else {
                connection.rollback();
                return false;
            }
        }
    }


    // =====================================================
    // 6. CHUYỂN MỘT DÒNG SQL THÀNH ĐỐI TƯỢNG MESSAGE
    // =====================================================

    /**
     * Chuyển dữ liệu của ResultSet thành Message.
     *
     * Tập trung việc ánh xạ tại một nơi giúp tránh
     * lặp code trong các phương thức truy vấn.
     */
    private Message mapRowToMessage(ResultSet rs)
            throws SQLException {

        Message msg = new Message(
                rs.getInt("id"),
                rs.getInt("room_id"),
                rs.getInt("sender_id"),
                rs.getString("sender_username"),
                rs.getString("sender_display_name"),
                rs.getString("content"),
                rs.getString("sent_at")
        );

        try {
            int replyTo = rs.getInt("reply_to_id");
            if (!rs.wasNull() && replyTo > 0) {
                msg.setReplyToId(replyTo);
                try {
                    msg.setReplySenderDisplayName(rs.getString("reply_sender_display_name"));
                    msg.setReplySnippet(rs.getString("reply_snippet"));
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException ignored) {
        }

        return msg;
    }


    // =====================================================
    // 7. KIỂM TRA DỮ LIỆU ĐẦU VÀO
    // =====================================================

    /**
     * Kiểm tra dữ liệu tin nhắn trước khi lưu.
     *
     * Server vẫn cần kiểm tra dữ liệu đầu vào riêng.
     * Việc kiểm tra tại DAO là lớp bảo vệ bổ sung.
     */
    private void validateMessage(Message message) {

        if (message == null) {
            throw new IllegalArgumentException(
                    "Message khong duoc null."
            );
        }

        validateRoomId(message.getRoomId());

        if (message.getSenderId() <= 0) {
            throw new IllegalArgumentException(
                    "senderId phai lon hon 0."
            );
        }

        if (message.getSenderUsername() == null
                || message.getSenderUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "Username nguoi gui khong hop le."
            );
        }

        if (message.getSenderDisplayName() == null
                || message.getSenderDisplayName().isBlank()) {

            throw new IllegalArgumentException(
                    "Ten hien thi khong hop le."
            );
        }

        if (message.getContent() == null
                || message.getContent().isBlank()) {

            throw new IllegalArgumentException(
                    "Noi dung tin nhan khong duoc de trong."
            );
        }

        if (message.getContent().length() > 500) {
            throw new IllegalArgumentException(
                    "Tin nhan khong duoc vuot qua 500 ky tu."
            );
        }
    }


    private void validateRoomId(int roomId) {

        if (roomId <= 0) {
            throw new IllegalArgumentException(
                    "roomId phai lon hon 0."
            );
        }
    }


    private void validatePageSize(int pageSize) {

        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize phai tu 1 den "
                    + MAX_PAGE_SIZE + "."
            );
        }
    }
}
