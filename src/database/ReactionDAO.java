package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ReactionDAO:
 *
 * Quản lý bảng message_reactions trong SQLite.
 *
 * Chức năng:
 * 1. Thả / thay / bỏ reaction.
 * 2. Lấy số lượng reaction theo từng emoji của một tin nhắn.
 * 3. Lấy danh sách display_name của người đã thả một emoji.
 *
 * Emoji hợp lệ: ❤️  😂  👍
 */
public class ReactionDAO {

    /** Các emoji được chấp nhận (theo thứ tự hiển thị). */
    public static final String[] ALLOWED_EMOJIS = { "❤️", "😂", "👍" };

    // =========================================================
    // 1. TOGGLE REACTION
    // =========================================================

    /**
     * Thả hoặc thay reaction.
     *
     * - Nếu người dùng chưa thả gì → INSERT mới.
     * - Nếu đã thả cùng emoji → xóa (toggle off).
     * - Nếu đã thả emoji khác → cập nhật sang emoji mới.
     *
     * @param messageId ID tin nhắn
     * @param userId    ID người dùng (từ session, không nhận từ client)
     * @param emoji     Emoji muốn thả
     * @return emoji hiện tại sau thao tác, hoặc null nếu đã bỏ reaction
     * @throws SQLException nếu lỗi DB
     * @throws IllegalArgumentException nếu emoji không hợp lệ
     */
    public String toggleReaction(
            int messageId,
            int userId,
            String emoji) throws SQLException {

        validateEmoji(emoji);
        validateId(messageId, "messageId");
        validateId(userId, "userId");

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                // Đọc reaction hiện tại của user này trên tin đó.
                String existing = findExisting(conn, messageId, userId);

                if (existing == null) {
                    // Chưa có → thêm mới.
                    insert(conn, messageId, userId, emoji);
                    conn.commit();
                    return emoji;

                } else if (existing.equals(emoji)) {
                    // Cùng emoji → toggle off.
                    delete(conn, messageId, userId);
                    conn.commit();
                    return null;

                } else {
                    // Khác emoji → cập nhật.
                    update(conn, messageId, userId, emoji);
                    conn.commit();
                    return emoji;
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }


    // =========================================================
    // 2. ĐẾM REACTION THEO EMOJI
    // =========================================================

    /**
     * Trả về map emoji → số lượt thả cho một tin nhắn.
     *
     * Chỉ trả về các emoji có ít nhất 1 lượt thả.
     *
     * @param messageId ID tin nhắn
     * @return Map theo thứ tự emoji chuẩn (❤️, 😂, 👍)
     */
    public Map<String, Integer> countByEmoji(int messageId)
            throws SQLException {

        validateId(messageId, "messageId");

        String sql = """
                SELECT emoji, COUNT(*) AS cnt
                FROM message_reactions
                WHERE message_id = ?
                GROUP BY emoji
                """;

        Map<String, Integer> result = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("emoji"), rs.getInt("cnt"));
                }
            }
        }

        return result;
    }


    // =========================================================
    // 3. LẤY DANH SÁCH NGƯỜI DÙNG ĐÃ THẢ MỘT EMOJI
    // =========================================================

    /**
     * Trả về chuỗi display_name của những người đã thả
     * một emoji cụ thể trên một tin nhắn.
     *
     * Tối đa 10 người để tránh tooltip quá dài.
     *
     * @param messageId ID tin nhắn
     * @param emoji     Emoji cần tra
     * @return chuỗi "A, B, C" (có thể rỗng)
     */
    public String getUsersWhoReacted(int messageId, String emoji)
            throws SQLException {

        validateId(messageId, "messageId");
        validateEmoji(emoji);

        String sql = """
                SELECT u.display_name
                FROM message_reactions r
                JOIN users u ON u.id = r.user_id
                WHERE r.message_id = ?
                  AND r.emoji = ?
                ORDER BY r.reacted_at
                LIMIT 10
                """;

        StringBuilder sb = new StringBuilder();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageId);
            ps.setString(2, emoji);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(rs.getString("display_name"));
                }
            }
        }

        return sb.toString();
    }


    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String findExisting(Connection conn, int messageId, int userId)
            throws SQLException {

        String sql = """
                SELECT emoji
                FROM message_reactions
                WHERE message_id = ? AND user_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("emoji") : null;
            }
        }
    }

    private void insert(Connection conn, int messageId, int userId, String emoji)
            throws SQLException {

        String sql = """
                INSERT INTO message_reactions(message_id, user_id, emoji)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.setString(3, emoji);
            ps.executeUpdate();
        }
    }

    private void update(Connection conn, int messageId, int userId, String emoji)
            throws SQLException {

        String sql = """
                UPDATE message_reactions
                SET emoji = ?, reacted_at = (datetime('now', 'localtime'))
                WHERE message_id = ? AND user_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emoji);
            ps.setInt(2, messageId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    private void delete(Connection conn, int messageId, int userId)
            throws SQLException {

        String sql = """
                DELETE FROM message_reactions
                WHERE message_id = ? AND user_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void validateEmoji(String emoji) {
        if (emoji == null) {
            throw new IllegalArgumentException("Emoji khong duoc null.");
        }
        for (String allowed : ALLOWED_EMOJIS) {
            if (allowed.equals(emoji)) {
                return;
            }
        }
        throw new IllegalArgumentException(
                "Emoji khong hop le: " + emoji
        );
    }

    private void validateId(int id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    name + " phai lon hon 0."
            );
        }
    }
}
