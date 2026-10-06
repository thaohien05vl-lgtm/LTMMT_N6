/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import model.User;
import security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
 * UserDAO:
 *
 * DAO = Data Access Object.
 *
 * Chịu trách nhiệm:
 *
 * - Tạo tài khoản
 * - Tìm tài khoản
 * - Kiểm tra username
 *
 * Không xử lý Socket.
 * Không xử lý giao diện.
 */
public class UserDAO {
  /**
     * Kiểm tra username đã tồn tại chưa.
     */
    public boolean usernameExists(
            String username)
            throws SQLException {

        String sql =
                """
                SELECT 1
                FROM users
                WHERE username = ?
                LIMIT 1
                """;


        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    username
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                return rs.next();
            }
        }
    }


    /**
     * Tạo tài khoản mới.
     *
     * Password sẽ được hash trước khi lưu.
     */
    public boolean createUser(
            String username,
            String displayName,
            String password)
            throws Exception {

        // Kiểm tra trùng trước.
        if (usernameExists(username)) {

            return false;
        }


        // Hash password.
        PasswordUtil.PasswordRecord record =
                PasswordUtil.hashPassword(
                        password
                );


        String sql =
                """
                INSERT INTO users (
                    username,
                    display_name,
                    password_hash,
                    password_salt,
                    password_iterations,
                    role
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;


        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    username
            );


            statement.setString(
                    2,
                    displayName
            );


            statement.setString(
                    3,
                    record.getHash()
            );


            statement.setString(
                    4,
                    record.getSalt()
            );


            statement.setInt(
                    5,
                    record.getIterations()
            );


            statement.setString(
                    6,
                    "USER"
            );


            statement.executeUpdate();


            return true;
        }
    }


    /**
     * Tìm User theo username.
     */
    public User findByUsername(
            String username)
            throws SQLException {

        String sql =
                """
                SELECT
                    id,
                    username,
                    display_name,
                    password_hash,
                    password_salt,
                    password_iterations,
                    role,
                    created_at
                FROM users
                WHERE username = ?
                LIMIT 1
                """;


        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    username
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (!rs.next()) {

                    return null;
                }


                return new User(

                        rs.getInt("id"),

                        rs.getString("username"),

                        rs.getString("display_name"),

                        rs.getString("password_hash"),

                        rs.getString("password_salt"),

                        rs.getInt(
                                "password_iterations"
                        ),

                        rs.getString("role"),

                        rs.getString("created_at")
                );
            }
        }
    }  
}
