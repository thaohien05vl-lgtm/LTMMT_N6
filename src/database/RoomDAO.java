/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.RoomRole;

/**
 * RoomDAO:
 *
 * Truy vấn thông tin phòng chat và quản lý vai trò thành viên
 * trong SQLite.
 *
 * Các chức năng:
 * - Tìm ID phòng theo tên.
 * - Thêm thành viên nếu chưa có bản ghi.
 * - Lấy vai trò của tài khoản trong phòng.
 * - Kiểm tra tài khoản có phải thành viên phòng hay không.
 * - Cập nhật vai trò của thành viên đã tồn tại.
 */
public class RoomDAO {

    /**
     * Tìm ID phòng từ tên phòng.
     *
     * Ví dụ:
     * "Học nhóm" -> ID thực tế trong Database.
     *
     * @param roomName tên phòng cần tìm
     * @return ID của phòng
     * @throws SQLException nếu phòng không tồn tại
     */
    public int findRoomIdByName(String roomName)
            throws SQLException {

        // Chuẩn hóa NFC để khớp đúng tên phòng có dấu tiếng Việt.
        String normalizedName = java.text.Normalizer.normalize(
                roomName.trim(),
                java.text.Normalizer.Form.NFC
        );

        // Lấy toàn bộ id + name để so sánh NFC case-insensitive ở Java.
        // SQLite COLLATE NOCASE chỉ hỗ trợ ASCII, không hỗ trợ tiếng Việt.
        String sql = """
                SELECT id, name
                FROM rooms
                ORDER BY id
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    String existingNfc = java.text.Normalizer.normalize(
                            rs.getString("name"),
                            java.text.Normalizer.Form.NFC
                    );
                    if (existingNfc.equalsIgnoreCase(normalizedName)) {
                        return rs.getInt("id");
                    }
                }
            }
        }

        throw new SQLException(
                "Không tìm thấy phòng trong Database: " + roomName
        );
    }

    // ==========================================================
    // THÊM THÀNH VIÊN NẾU CHƯA CÓ
    // ==========================================================

    /**
     * Ghi nhận tài khoản là thành viên của một phòng.
     *
     * INSERT OR IGNORE không ghi đè bản ghi đã tồn tại.
     *
     * Ví dụ:
     * - Người dùng đã là ADMIN: vẫn giữ ADMIN.
     * - Người dùng đã là OWNER: vẫn giữ OWNER.
     * - Chưa có bản ghi: thêm vai trò được truyền vào.
     *
     * Server vẫn phải kiểm tra quyền trước khi cho phép
     * thực hiện các thao tác quản trị ở những checkpoint sau.
     *
     * @param roomName tên phòng
     * @param userId ID tài khoản
     * @param role vai trò cần gán nếu chưa có bản ghi
     * @throws SQLException nếu thao tác Database thất bại
     */
    public void addMemberIfAbsent(
            String roomName,
            int userId,
            RoomRole role) throws SQLException {

        if (roomName == null || roomName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên phòng không được để trống."
            );
        }

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "ID tài khoản không hợp lệ."
            );
        }

        // Nếu role null thì mặc định thành MEMBER.
        if (role == null) {
            role = RoomRole.MEMBER;
        }

        int roomId = findRoomIdByName(roomName.trim());

        String sql = """
                INSERT OR IGNORE INTO room_members(
                    room_id,
                    user_id,
                    role
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, roomId);
            statement.setInt(2, userId);
            statement.setString(3, role.name());

            statement.executeUpdate();
        }
    }

    // ==========================================================
    // LẤY VAI TRÒ CỦA MỘT TÀI KHOẢN TRONG PHÒNG
    // ==========================================================

    /**
     * Trả về vai trò của tài khoản trong phòng.
     *
     * @param roomName tên phòng
     * @param userId ID tài khoản
     * @return OWNER, ADMIN hoặc MEMBER; null nếu chưa có bản ghi
     * @throws SQLException nếu truy vấn Database thất bại
     */
    public RoomRole findMemberRole(
            String roomName,
            int userId) throws SQLException {

        String sql = """
                SELECT rm.role
                FROM room_members rm
                INNER JOIN rooms r
                    ON r.id = rm.room_id
                WHERE r.name = ?
                  AND rm.user_id = ?
                LIMIT 1
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, roomName);
            statement.setInt(2, userId);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return RoomRole.fromDatabase(
                            rs.getString("role")
                    );
                }
            }
        }

        return null;
    }

    // ==========================================================
    // KIỂM TRA TÀI KHOẢN CÓ BẢN GHI THÀNH VIÊN HAY CHƯA
    // ==========================================================

    /**
     * Kiểm tra tài khoản đã có bản ghi thành viên trong phòng chưa.
     *
     * @param roomName tên phòng
     * @param userId ID tài khoản
     * @return true nếu có bản ghi; ngược lại false
     * @throws SQLException nếu truy vấn Database thất bại
     */
    public boolean isMember(
            String roomName,
            int userId) throws SQLException {

        String sql = """
                SELECT 1
                FROM room_members rm
                INNER JOIN rooms r
                    ON r.id = rm.room_id
                WHERE r.name = ?
                  AND rm.user_id = ?
                LIMIT 1
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, roomName);
            statement.setInt(2, userId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ==========================================================
    // CẬP NHẬT VAI TRÒ ĐÃ CÓ
    // ==========================================================

    /**
     * Cập nhật vai trò của một bản ghi thành viên đã tồn tại.
     *
     * Phương thức này không tự kiểm tra quyền quản trị.
     * Server phải xác minh người gọi có quyền đổi vai trò trước
     * khi gọi phương thức này.
     *
     * @param roomName tên phòng
     * @param userId ID tài khoản cần cập nhật
     * @param role vai trò mới
     * @throws SQLException nếu không tìm thấy thành viên hoặc
     *                      thao tác Database thất bại
     */
    public void setMemberRole(
            String roomName,
            int userId,
            RoomRole role) throws SQLException {

        if (roomName == null || roomName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên phòng không được để trống."
            );
        }

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "ID tài khoản không hợp lệ."
            );
        }

        if (role == null) {
            throw new IllegalArgumentException(
                    "Vai trò không được null."
            );
        }

        String sql = """
                UPDATE room_members
                SET role = ?
                WHERE room_id = (
                    SELECT id
                    FROM rooms
                    WHERE name = ?
                )
                AND user_id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, role.name());
            statement.setString(2, roomName.trim());
            statement.setInt(3, userId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Không tìm thấy thành viên để cập nhật vai trò: "
                        + roomName
                );
            }
        }
    }


    // ==========================================================
    // TẠO PHÒNG MỚI VÀ GÁN OWNER TRONG CÙNG TRANSACTION
    // ==========================================================

    /**
     * Tạo phòng mới và gán người tạo làm OWNER.
     *
     * Hai thao tác INSERT vào rooms và room_members nằm trong cùng
     * một transaction. Nếu thao tác Database thất bại, transaction
     * được rollback để tránh tạo phòng mà không có Owner.
     *
     * @param roomName tên phòng mới
     * @param ownerUserId ID tài khoản lấy từ Session Server
     * @return true nếu tạo thành công; false nếu tên phòng đã tồn tại
     * @throws SQLException nếu thao tác Database thất bại
     */
    public boolean createRoomWithOwner(
            String roomName,
            int ownerUserId) throws SQLException {

        if (roomName == null || roomName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên phòng không được để trống."
            );
        }

        if (ownerUserId <= 0) {
            throw new IllegalArgumentException(
                    "ID Owner không hợp lệ."
            );
        }

        // Chuẩn hóa NFC trước khi lưu vào DB để tránh trùng tên NFD/NFC.
        roomName = java.text.Normalizer.normalize(
                roomName.trim(),
                java.text.Normalizer.Form.NFC
        );

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                // --------------------------------------------------
                // BƯỚC 1: Xác nhận tài khoản Owner tồn tại.
                // ID phải lấy từ Session đã xác thực phía Server.
                // --------------------------------------------------
                String checkUserSql = """
                        SELECT 1
                        FROM users
                        WHERE id = ?
                        LIMIT 1
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(checkUserSql)) {

                    statement.setInt(1, ownerUserId);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Không tìm thấy tài khoản Owner."
                            );
                        }
                    }
                }

                // --------------------------------------------------
                // BƯỚC 2: Kiểm tra tên phòng đã tồn tại chưa.
                // Cột rooms.name nên được khai báo COLLATE NOCASE
                // và có UNIQUE constraint để chống trùng tên.
                // --------------------------------------------------
                String checkRoomSql = """
                        SELECT id
                        FROM rooms
                        WHERE name = ?
                        LIMIT 1
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(checkRoomSql)) {

                    statement.setString(1, roomName);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                // --------------------------------------------------
                // BƯỚC 3: Tạo bản ghi phòng.
                // --------------------------------------------------
                String insertRoomSql = """
                        INSERT INTO rooms(name)
                        VALUES (?)
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(insertRoomSql)) {

                    statement.setString(1, roomName);
                    statement.executeUpdate();
                }

                // --------------------------------------------------
                // BƯỚC 4: Lấy ID phòng vừa tạo.
                // --------------------------------------------------
                int roomId;
                String findCreatedRoomSql = """
                        SELECT id
                        FROM rooms
                        WHERE name = ?
                        LIMIT 1
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(findCreatedRoomSql)) {

                    statement.setString(1, roomName);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Không tìm thấy phòng vừa tạo."
                            );
                        }

                        roomId = rs.getInt("id");
                    }
                }

                // --------------------------------------------------
                // BƯỚC 5: Gán người tạo làm OWNER.
                // --------------------------------------------------
                String insertOwnerSql = """
                        INSERT INTO room_members(
                            room_id,
                            user_id,
                            role
                        )
                        VALUES (?, ?, 'OWNER')
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(insertOwnerSql)) {

                    statement.setInt(1, roomId);
                    statement.setInt(2, ownerUserId);
                    statement.executeUpdate();
                }

                // --------------------------------------------------
                // BƯỚC 6: Xác nhận cả việc tạo phòng và gán Owner.
                // --------------------------------------------------
                connection.commit();

                System.out.println(
                        "[ROOM] Đã tạo phòng: "
                        + roomName
                        + ", OWNER userId="
                        + ownerUserId
                );

                return true;

            } catch (SQLException e) {
                // Nếu có lỗi, hoàn tác cả bản ghi phòng và Owner.
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;

            } finally {
                // Khôi phục chế độ transaction ban đầu.
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    // ==========================================================
    // LẤY TẤT CẢ TÊN PHÒNG ĐÃ LƯU TRONG DATABASE
    // ==========================================================

    /**
     * Lấy danh sách phòng đã lưu trong Database.
     * Có thể được dùng khi Server khởi động để nạp lại phòng
     * đã tạo từ các lần chạy trước.
     *
     * @return danh sách tên phòng, sắp xếp không phân biệt hoa thường
     * @throws SQLException nếu truy vấn Database thất bại
     */
    public List<String> findAllRoomNames() throws SQLException {

        List<String> rooms = new ArrayList<>();

        String sql = """
                SELECT name
                FROM rooms
                ORDER BY name COLLATE NOCASE
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                rooms.add(rs.getString("name"));
            }
        }

        return rooms;
    }

}
