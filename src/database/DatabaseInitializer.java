/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInitializer:
 *
 * Khởi tạo cấu trúc Database cho ứng dụng Group Chat.
 *
 * Phase 2:
 * - users
 *
 * Phase 3:
 * - rooms
 * - messages
 *
 * Lưu ý:
 * CREATE TABLE IF NOT EXISTS không xóa dữ liệu
 * trong bảng đã tồn tại.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
        // Không cho phép tạo đối tượng.
    }

    /**
     * Khởi tạo toàn bộ bảng cần thiết.
     */
    public static void initialize() {

        // =====================================================
        // 1. BẢNG USERS
        //
        // Giữ nguyên cấu trúc tài khoản của Phase 2.
        // =====================================================

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    display_name TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    password_iterations INTEGER NOT NULL,
                    role TEXT NOT NULL DEFAULT 'USER',
                    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
                )
                """;


        // =====================================================
        // 2. BẢNG ROOMS
        //
        // Lưu thông tin phòng trong Database.
        // =====================================================

        String createRoomsTable = """
                CREATE TABLE IF NOT EXISTS rooms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
                )
                """;

        // =====================================================
        // 3. BẢNG ROOM_MEMBERS
        //
        // Lưu thành viên và vai trò của họ trong từng phòng.
        // Không dùng users.role để thay thế vai trò theo phòng.
        // =====================================================

        String createRoomMembersTable = """
                CREATE TABLE IF NOT EXISTS room_members (
        
                    room_id INTEGER NOT NULL,
        
                    user_id INTEGER NOT NULL,
        
                    role TEXT NOT NULL DEFAULT 'MEMBER'
                        CHECK(role IN ('OWNER', 'ADMIN', 'MEMBER')),
        
                    joined_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
        
                    PRIMARY KEY (room_id, user_id),
        
                    FOREIGN KEY (room_id)
                        REFERENCES rooms(id)
                        ON DELETE CASCADE,
        
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                        ON DELETE CASCADE
                )
                """;


        // =====================================================
        // 4. BẢNG MESSAGES
        //
        // Mỗi bản ghi đại diện cho một tin nhắn.
        // =====================================================

        String createMessagesTable = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
        
                    room_id INTEGER NOT NULL,
        
                    sender_id INTEGER NOT NULL,
        
                    sender_username TEXT NOT NULL,
        
                    sender_display_name TEXT NOT NULL,
        
                    content TEXT NOT NULL
                        CHECK(length(content) BETWEEN 1 AND 500),
        
                    reply_to_id INTEGER,

                    sent_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
        
                    FOREIGN KEY (room_id)
                        REFERENCES rooms(id)
                        ON DELETE CASCADE,
        
                    FOREIGN KEY (sender_id)
                        REFERENCES users(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (reply_to_id)
                        REFERENCES messages(id)
                        ON DELETE SET NULL
                )
                """;


        // =====================================================
        // 5. BẢNG MESSAGE_REACTIONS
        //
        // Mỗi bản ghi: ai (user_id) thả emoji nào (emoji)
        // cho tin nhắn nào (message_id).
        // PRIMARY KEY (message_id, user_id) đảm bảo mỗi người
        // chỉ có 1 reaction trên mỗi tin; thả lại sẽ thay emoji.
        // =====================================================

        String createReactionsTable = """
                CREATE TABLE IF NOT EXISTS message_reactions (
                    message_id INTEGER NOT NULL,
                    user_id    INTEGER NOT NULL,
                    emoji      TEXT    NOT NULL
                        CHECK(emoji IN ('\u2764\uFE0F', '\ud83d\ude02', '\ud83d\udc4d')),
                    reacted_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
                    PRIMARY KEY (message_id, user_id),
                    FOREIGN KEY (message_id)
                        REFERENCES messages(id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                        ON DELETE CASCADE
                )
                """;

        // =====================================================
        // 6. CHỈ MỤC PHỤC VỤ TRUY VẤN LỊCH SỬ
        // =====================================================

        String createMessageHistoryIndex = """
                CREATE INDEX IF NOT EXISTS idx_messages_room_id_id
                ON messages(room_id, id)
                """;


        String createMessageSenderIndex = """
                CREATE INDEX IF NOT EXISTS idx_messages_sender_id
                ON messages(sender_id)
                """;

        // Tìm kiếm reaction theo tin nhắn.
        String createReactionIndex = """
                CREATE INDEX IF NOT EXISTS idx_reactions_message_id
                ON message_reactions(message_id)
                """;

        // =====================================================
        // CHỈ MỤC PHỤC VỤ TRA CỨU THÀNH VIÊN THEO TÀI KHOẢN
        // =====================================================
        String createRoomMemberUserIndex = """
                CREATE INDEX IF NOT EXISTS idx_room_members_user_id
                ON room_members(user_id, room_id)
                """;

        // =====================================================
        // 7. BẢNG TIN NHẮN RIÊNG TƯ (PRIVATE MESSAGES 1-1)
        // =====================================================
        String createPrivateMessagesTable = """
                CREATE TABLE IF NOT EXISTS private_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_id INTEGER NOT NULL,
                    sender_username TEXT NOT NULL COLLATE NOCASE,
                    sender_display_name TEXT NOT NULL,
                    receiver_id INTEGER NOT NULL,
                    receiver_username TEXT NOT NULL COLLATE NOCASE,
                    receiver_display_name TEXT NOT NULL,
                    content TEXT NOT NULL
                        CHECK(length(content) BETWEEN 1 AND 500),
                    sent_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (sender_id)
                        REFERENCES users(id)
                        ON DELETE RESTRICT,
                    FOREIGN KEY (receiver_id)
                        REFERENCES users(id)
                        ON DELETE RESTRICT
                )
                """;

        String createPrivateMessagesIndex = """
                CREATE INDEX IF NOT EXISTS idx_private_messages_pair
                ON private_messages(sender_username, receiver_username, id)
                """;

        String createMessageReplyIndex = """
                CREATE INDEX IF NOT EXISTS idx_messages_reply_to
                ON messages(reply_to_id)
                """;

        // =====================================================
        // 8. THỰC HIỆN KHỞI TẠO
        // =====================================================

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            // Bảo đảm các thao tác khởi tạo chạy cùng transaction.
            connection.setAutoCommit(false);

            try (Statement statement =
                         connection.createStatement()) {

                statement.executeUpdate(createUsersTable);
                statement.executeUpdate(createRoomsTable);
                // Bảng vai trò theo từng phòng.
                statement.executeUpdate(createRoomMembersTable);
                statement.executeUpdate(createMessagesTable);
                // Đảm bảo cột reply_to_id tồn tại nếu database đã được tạo trước đó
                try {
                    statement.executeUpdate("ALTER TABLE messages ADD COLUMN reply_to_id INTEGER REFERENCES messages(id)");
                } catch (SQLException ignored) {
                }
                // Bảng cảm xúc tin nhắn (reactions).
                statement.executeUpdate(createReactionsTable);
                // Bảng tin nhắn riêng tư 1-1.
                statement.executeUpdate(createPrivateMessagesTable);
                // Các chỉ mục phục vụ lịch sử tin nhắn.
                statement.executeUpdate(createMessageHistoryIndex);
                statement.executeUpdate(createMessageSenderIndex);
                statement.executeUpdate(createMessageReplyIndex);
                // Chỉ mục tìm kiếm reaction theo tin nhắn.
                statement.executeUpdate(createReactionIndex);
                // Chỉ mục tra cứu thành viên theo tài khoản.
                statement.executeUpdate(createRoomMemberUserIndex);
                // Chỉ mục tra cứu tin nhắn riêng 1-1.
                statement.executeUpdate(createPrivateMessagesIndex);
            }

            // Nếu cấu trúc bảng được tạo thành công,
            // thêm các phòng mặc định nếu chưa có.
            seedDefaultRooms(connection);

            // Xác nhận các thay đổi.
            connection.commit();

            System.out.println(
                    "[DATABASE] Khoi tao users thanh cong."
            );

            System.out.println(
                    "[DATABASE] Khoi tao rooms thanh cong."
            );
            
            System.out.println(
                    "[DATABASE] Da khoi tao bang room_members."
            );

            System.out.println(
                    "[DATABASE] Khoi tao messages thanh cong."
            );

            System.out.println(
                    "[DATABASE] Da khoi tao chi muc lich su."
            );

            System.out.println(
                    "[DATABASE] Da khoi tao bang message_reactions."
            );

            System.out.println(
                    "[DATABASE] Da khoi tao bang private_messages."
            );

            System.out.println(
                    "[DATABASE] Database: "
                    + DatabaseConnection.getDatabasePath()
            );

        } catch (SQLException e) {

            System.err.println(
                    "[DATABASE] Khong the khoi tao Database."
            );

            throw new RuntimeException(
                    "Loi khoi tao Database.",
                    e
            );
        }
    }


    /**
     * Thêm các phòng mặc định vào Database.
     *
     * INSERT OR IGNORE giúp chạy initialize() nhiều lần
     * mà không tạo các bản ghi phòng bị trùng.
     */
    private static void seedDefaultRooms(
            Connection connection) throws SQLException {

        String sql = """
                INSERT OR IGNORE INTO rooms(name)
                VALUES (?)
                """;

        String[] defaultRooms = {
            "Sảnh chính",
            "Lập trình mạng",
            "Giải trí",
            "Học nhóm"
        };

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (String roomName : defaultRooms) {

                statement.setString(1, roomName);

                statement.executeUpdate();
            }
        }
    }
}