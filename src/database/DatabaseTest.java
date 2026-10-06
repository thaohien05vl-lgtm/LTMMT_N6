/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author ASUS
 */
public class DatabaseTest {

       public static void main(String[] args) {

        try {

            System.out.println("========================================");
            System.out.println("       KIEM TRA CO SO DU LIEU SQLITE");
            System.out.println("========================================");

            // 1. Nạp SQLite JDBC Driver
            System.out.println("[1] Dang nap SQLite JDBC Driver...");

            Class.forName("org.sqlite.JDBC");

            System.out.println("[THANH CONG] SQLite JDBC Driver da duoc nap.");

            // 2. Tạo thư mục data nếu chưa tồn tại
            Path thuMucData = Paths.get("data");

            if (!Files.exists(thuMucData)) {

                Files.createDirectories(thuMucData);

                System.out.println("[2] Da tao thu muc data.");

            } else {

                System.out.println("[2] Thu muc data da ton tai.");
            }

            // 3. Đường dẫn database
            String url = "jdbc:sqlite:data/test.db";

            System.out.println("[3] Dang ket noi SQLite...");
            System.out.println("[INFO] Duong dan: " + url);

            // 4. Kết nối
            try (Connection connection = DriverManager.getConnection(url)) {

                System.out.println("[THANH CONG] Ket noi SQLite thanh cong!");

                System.out.println("[INFO] Database dang hoat dong.");

            }

            System.out.println("[4] Da dong ket noi.");

        } catch (ClassNotFoundException e) {

            System.out.println("[LOI] Khong tim thay SQLite JDBC Driver.");
            System.out.println("[CHI TIET] " + e.getMessage());

        } catch (Exception e) {

            System.out.println("[LOI] Khong the ket noi SQLite.");
            System.out.println("[CHI TIET] " + e.getMessage());
        }

        System.out.println("========================================");
        System.out.println("          KET THUC KIEM TRA");
        System.out.println("========================================");
    }
}