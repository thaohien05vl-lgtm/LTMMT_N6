/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package common;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * EncodingUtil.java
 *
 * Cấu hình UTF-8 cho đầu ra và đầu ra lỗi của chương trình.
 * Sử dụng để hạn chế lỗi hiển thị tiếng Việt khi chạy Java.
 */

public final class EncodingUtil {

    private EncodingUtil() {
        // Không cho phép tạo đối tượng từ lớp tiện ích.
    }

    /**
     * Thiết lập UTF-8 cho System.out và System.err.
     *
     * Phải gọi phương thức này trước khi in nội dung
     * tiếng Việt ra cửa sổ Output của NetBeans.
     */
    public static void setupUTF8() {

        System.setOut(
                new PrintStream(
                        new FileOutputStream(FileDescriptor.out),
                        true,
                        StandardCharsets.UTF_8
                )
        );

        System.setErr(
                new PrintStream(
                        new FileOutputStream(FileDescriptor.err),
                        true,
                        StandardCharsets.UTF_8
                )
        );
    }
}