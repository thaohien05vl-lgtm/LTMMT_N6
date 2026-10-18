/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

/**
 * ValidationUtil:
 *
 * Kiểm tra dữ liệu đầu vào trước khi đưa
 * vào nghiệp vụ hoặc Database.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }


    /**
     * Username:
     *
     * 3 → 20 ký tự
     * Chỉ gồm A-Z, a-z, 0-9, _
     */
    public static boolean isValidUsername(
            String username) {

        if (username == null) {
            return false;
        }


        return username.matches(
                "^[A-Za-z0-9_]{3,20}$"
        );
    }


    /**
     * Display name:
     *
     * 2 → 40 ký tự.
     *
     * Cho phép tiếng Việt và Unicode.
     */
    public static boolean isValidDisplayName(
            String displayName) {

        if (displayName == null) {
            return false;
        }


        String value =
                displayName.trim();


        if (
                value.length() < 2
                || value.length() > 40
        ) {

            return false;
        }


        // Không được chứa ký tự protocol.
        if (value.contains("|")) {

            return false;
        }


        // Không cho CR/LF.
        if (
                value.contains("\r")
                || value.contains("\n")
        ) {

            return false;
        }


        return true;
    }


    /**
     * Password:
     *
     * 8 → 128 ký tự.
     *
     * Không ép buộc:
     *
     * phải có chữ hoa
     * phải có số
     * phải có ký tự đặc biệt
     *
     * vì password Unicode vẫn được cho phép.
     */
    public static boolean isValidPassword(
            String password) {

        if (password == null) {
            return false;
        }


        return password.length() >= 8
                && password.length() <= 128;
    }
}
