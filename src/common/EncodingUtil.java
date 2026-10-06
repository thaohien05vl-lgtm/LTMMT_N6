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
 *
 * @author TUAN HUY
 */
public class EncodingUtil {
    private EncodingUtil() {
    }

    /**
     * Cấu hình Console sử dụng UTF-8.
     */
    public static void setupUTF8() {

        try {

            // =============================================
            // Cấu hình System.out sử dụng UTF-8
            // =============================================

            System.setOut(
                    new PrintStream(
                            new FileOutputStream(
                                    FileDescriptor.out
                            ),
                            true,
                            StandardCharsets.UTF_8
                    )
            );


            // =============================================
            // Cấu hình System.err sử dụng UTF-8
            // =============================================

            System.setErr(
                    new PrintStream(
                            new FileOutputStream(
                                    FileDescriptor.err
                            ),
                            true,
                            StandardCharsets.UTF_8
                    )
            );


        } catch (Exception e) {

            System.err.println(
                    "[ENCODING] Không thể cấu hình UTF-8 cho Console."
            );
        }
    }
}
