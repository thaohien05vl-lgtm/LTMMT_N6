/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author ASUS
 */
/**
 * DatabaseInitializer:
 *
 * Tạo các bảng cần thiết khi Server khởi động.
 *
 * Phase 2 hiện tại chỉ sử dụng bảng:
 *
 * users
 *
 * Phase 3 chúng ta sẽ bổ sung:
 *
 * rooms
 * room_members
 * messages
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }


    /**
     * Khởi tạo Database.
     */
    public static void initialize() {

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    username TEXT NOT NULL
                        COLLATE NOCASE
                        UNIQUE,

                    display_name TEXT NOT NULL,

                    password_hash TEXT NOT NULL,

                    password_salt TEXT NOT NULL,

                    password_iterations INTEGER NOT NULL,

                    role TEXT NOT NULL DEFAULT 'USER',

                    created_at TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                )
                """;


        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(
                    createUsersTable
            );


            System.out.println(
                    "[DATABASE] Đã khởi tạo bảng users."
            );


            System.out.println(
                    "[DATABASE] Database: "
                    + DatabaseConnection.getDatabasePath()
            );


        } catch (SQLException e) {

            throw new RuntimeException(
                    "Không thể khởi tạo Database.",
                    e
            );
        }
    }
}
