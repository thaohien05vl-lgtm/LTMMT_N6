/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


package database;

import common.EncodingUtil;
import model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * MessageDAOTest.java
 *
 * Kiểm thử MessageDAO trước khi tích hợp vào Server.
 *
 * Chương trình thực hiện:
 * 1. Khởi tạo cấu trúc database.
 * 2. Tìm tài khoản có sẵn.
 * 3. Tìm phòng chat có sẵn.
 * 4. Tạo và lưu tin nhắn kiểm thử.
 * 5. Đọc lịch sử tin nhắn mới nhất.
 * 6. Đọc một trang tin nhắn cũ hơn ID được chỉ định.
 * 7. Tìm kiếm tin nhắn.
 * 8. Đếm tổng số tin nhắn của phòng.
 */
public class MessageDAOTest {

    public static void main(String[] args) {

        // Bước 0: Thiết lập UTF-8 trước khi in tiếng Việt.
        EncodingUtil.setupUTF8();

        System.out.println(
                "=================================================="
        );
        System.out.println(
                "              KIỂM THỬ MESSAGE DAO"
        );
        System.out.println(
                "=================================================="
        );

        // Kiểm tra hiển thị tiếng Việt.
        System.out.println(
                "KIỂM TRA UTF-8: Tiếng Việt có dấu - Sảnh chính - Lập trình mạng"
        );

        try {

            // Bước 1: Khởi tạo cấu trúc database.
            // Không chủ động xóa dữ liệu đã có.
            DatabaseInitializer.initialize();

            // Bước 2: Hiển thị đường dẫn database.
            System.out.println();
            System.out.println("[THÔNG TIN] Đường dẫn database:");

            System.out.println(
                    DatabaseConnection.getDatabasePath()
            );

            // Bước 3: Tìm một tài khoản có sẵn.
            int userId;
            String username;
            String displayName;

            String userSql = """
                    SELECT id, username, display_name
                    FROM users
                    ORDER BY id
                    LIMIT 1
                    """;

            try (
                    Connection connection =
                            DatabaseConnection.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(userSql);

                    ResultSet rs = statement.executeQuery()
            ) {

                if (!rs.next()) {

                    System.out.println(
                            "[LỖI] Chưa có tài khoản nào trong database."
                    );

                    System.out.println(
                            "Hãy đăng ký tài khoản trước khi kiểm thử."
                    );

                    return;
                }

                userId = rs.getInt("id");
                username = rs.getString("username");
                displayName = rs.getString("display_name");
            }

            System.out.println();
            System.out.println(
                    "[OK] Tài khoản: " + username
            );

            System.out.println(
                    "[OK] Tên hiển thị: " + displayName
            );

            // Bước 4: Tìm một phòng chat có sẵn.
            int roomId;
            String roomName;

            String roomSql = """
                    SELECT id, name
                    FROM rooms
                    ORDER BY id
                    LIMIT 1
                    """;

            try (
                    Connection connection =
                            DatabaseConnection.getConnection();

                    PreparedStatement statement =
                            connection.prepareStatement(roomSql);

                    ResultSet rs = statement.executeQuery()
            ) {

                if (!rs.next()) {

                    System.out.println(
                            "[LỖI] Chưa có phòng chat nào trong database."
                    );

                    return;
                }

                roomId = rs.getInt("id");
                roomName = rs.getString("name");
            }

            System.out.println(
                    "[OK] Phòng chat: "
                    + roomName
                    + " (ID = "
                    + roomId
                    + ")"
            );

            // Bước 5: Tạo một tin nhắn kiểm thử.
            Message message = new Message(
                    roomId,
                    userId,
                    username,
                    displayName,
                    "Tin nhắn kiểm thử MessageDAO"
            );

            // Bước 6: Lưu tin nhắn vào database.
            MessageDAO messageDAO = new MessageDAO();

            Message savedMessage =
                    messageDAO.saveMessage(message);

            System.out.println();
            System.out.println(
                    "[OK] Đã lưu tin nhắn thành công."
            );

            System.out.println(
                    "ID tin nhắn: "
                    + savedMessage.getId()
            );

            System.out.println(
                    "Thời gian gửi: "
                    + savedMessage.getSentAt()
            );

            // Bước 7: Đọc 50 tin nhắn mới nhất của phòng.
            List<Message> history =
                    messageDAO.findLatestMessages(roomId, 50);

            System.out.println();
            System.out.println(
                    "--------------- LỊCH SỬ TIN NHẮN MỚI NHẤT ---------------"
            );

            if (history.isEmpty()) {

                System.out.println(
                        "Phòng chat hiện chưa có tin nhắn."
                );

            } else {

                for (Message item : history) {

                    System.out.println(
                            "["
                            + item.getId()
                            + "] "
                            + item.getSenderDisplayName()
                            + ": "
                            + item.getContent()
                            + " ("
                            + item.getSentAt()
                            + ")"
                    );
                }
            }

            // Bước 8: Lấy một trang tin nhắn cũ hơn ID 51.
            // Tham số 51 là mốc ID.
            // Tham số 50 là số lượng tin nhắn tối đa cần lấy.
            List<Message> olderMessages =
                    messageDAO.findOlderMessages(
                            roomId,
                            51,
                            50
                    );

            System.out.println();
            System.out.println(
                    "--------------- TIN NHẮN CŨ HƠN ID 51 ---------------"
            );

            if (olderMessages.isEmpty()) {

                System.out.println(
                        "Không tìm thấy tin nhắn nào cũ hơn ID 51."
                );

            } else {

                for (Message item : olderMessages) {

                    System.out.println(
                            "["
                            + item.getId()
                            + "] "
                            + item.getSenderDisplayName()
                            + ": "
                            + item.getContent()
                    );
                }
            }

            System.out.println(
                    "Tổng số tin nhắn cũ lấy được: "
                    + olderMessages.size()
            );

            // Bước 9: Tìm kiếm tin nhắn kiểm thử.
            List<Message> searchResults =
                    messageDAO.searchMessages(
                            roomId,
                            "kiểm thử MessageDAO",
                            20
                    );

            System.out.println();
            System.out.println(
                    "--------------- KẾT QUẢ TÌM KIẾM ---------------"
            );

            System.out.println(
                    "[OK] Số kết quả tìm kiếm: "
                    + searchResults.size()
            );

            for (Message item : searchResults) {

                System.out.println(
                        "["
                        + item.getId()
                        + "] "
                        + item.getSenderDisplayName()
                        + ": "
                        + item.getContent()
                );
            }

            // Bước 10: Đếm tổng số tin nhắn trong phòng.
            int totalMessages =
                    messageDAO.countMessagesByRoom(roomId);

            System.out.println();
            System.out.println(
                    "[OK] Tổng số tin nhắn trong phòng: "
                    + totalMessages
            );

            // Kết thúc kiểm thử.
            System.out.println();
            System.out.println(
                    "=================================================="
            );

            System.out.println(
                    "                 KẾT THÚC KIỂM THỬ"
            );

            System.out.println(
                    "=================================================="
            );

        } catch (Exception e) {

            System.err.println();
            System.err.println(
                    "[LỖI] Kiểm thử MessageDAO thất bại."
            );

            e.printStackTrace();
        }
    }
}