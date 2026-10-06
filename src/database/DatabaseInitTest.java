package database;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class DatabaseInitTest {

    public static void main(String[] args) {

        // Ép Console sử dụng UTF-8
        try {
            System.setOut(new PrintStream(
                    new FileOutputStream(FileDescriptor.out),
                    true,
                    StandardCharsets.UTF_8
            ));

            System.setErr(new PrintStream(
                    new FileOutputStream(FileDescriptor.err),
                    true,
                    StandardCharsets.UTF_8
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("========================================");
        System.out.println("      KIỂM TRA KHỞI TẠO CƠ SỞ DỮ LIỆU");
        System.out.println("========================================");

        try {

            System.out.println("[1] Đang khởi tạo cơ sở dữ liệu...");

            DatabaseInitializer.initialize();

            System.out.println("[THÀNH CÔNG] Khởi tạo cơ sở dữ liệu thành công!");

        } catch (Exception e) {

            System.out.println("[THẤT BẠI] Khởi tạo cơ sở dữ liệu thất bại.");
            System.out.println("[CHI TIẾT] " + e.getMessage());

            e.printStackTrace();
        }

        System.out.println("========================================");
        System.out.println("        KẾT THÚC KIỂM TRA");
        System.out.println("========================================");
    }
}