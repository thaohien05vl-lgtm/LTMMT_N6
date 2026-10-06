/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 * DatabaseConnection:
 *
 * Chịu trách nhiệm tạo kết nối tới SQLite Database.
 *
 * Database của project:
 *
 * data/groupchat.db
 */
public final class DatabaseConnection {

    // =========================================================
    // Đường dẫn Database
    // =========================================================

    private static final String DB_FOLDER =
            "data";

    private static final String DB_FILE =
            DB_FOLDER + File.separator + "groupchat.db";

    private static final String DB_URL =
            "jdbc:sqlite:" + DB_FILE;


    // =========================================================
    // Constructor private
    // Không cho tạo đối tượng.
    // =========================================================

    private DatabaseConnection() {
    }


    // =========================================================
    // Nạp SQLite JDBC Driver
    // =========================================================

    static {

        try {

            Class.forName(
                    "org.sqlite.JDBC"
            );

        } catch (ClassNotFoundException e) {

            throw new RuntimeException(
                    "Không tìm thấy SQLite JDBC Driver.",
                    e
            );
        }
    }


    /**
     * Tạo một Connection mới.
     *
     * Mỗi DAO sẽ mở connection khi cần
     * và đóng connection sau khi hoàn thành.
     *
     * @return Connection SQLite
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection()
            throws SQLException {

        // Tạo thư mục data nếu chưa tồn tại.
        File folder =
                new File(DB_FOLDER);

        if (!folder.exists()) {

            folder.mkdirs();
        }


        Connection connection =
                DriverManager.getConnection(
                        DB_URL
                );


        // Bật kiểm tra Foreign Key.
        try (
                var statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );

            // Nếu nhiều thao tác cùng lúc,
            // SQLite đợi tối đa 5 giây khi database đang bận.
            statement.execute(
                    "PRAGMA busy_timeout = 5000"
            );
        }


        return connection;
    }


    /**
     * Lấy đường dẫn tuyệt đối của Database.
     *
     * Dùng để debug và hiển thị cho nhóm.
     */
    public static String getDatabasePath() {

        return new File(
                DB_FILE
        ).getAbsolutePath();
    }
}