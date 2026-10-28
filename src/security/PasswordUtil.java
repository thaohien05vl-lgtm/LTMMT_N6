/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/**
 * PasswordUtil:
 *
 * Xử lý password:
 *
 * Password
 *      ↓
 * PBKDF2 + HMAC-SHA256
 *      ↓
 * Salt ngẫu nhiên
 *      ↓
 * Password Hash
 *
 * Password gốc KHÔNG được lưu vào Database.
 */
public final class PasswordUtil {

    // ==========================================================
    // Cấu hình PBKDF2
    // ==========================================================

    private static final String ALGORITHM =
            "PBKDF2WithHmacSHA256";


    /**
     * 600.000 iterations.
     *
     * Đây là mức khuyến nghị hiện tại của OWASP
     * cho PBKDF2-HMAC-SHA256.
     */
    private static final int DEFAULT_ITERATIONS =
            600_000;


    /**
     * Salt 16 bytes.
     */
    private static final int SALT_LENGTH =
            16;


    /**
     * Hash 256 bits = 32 bytes.
     */
    private static final int HASH_LENGTH =
            256;


    private static final SecureRandom RANDOM =
            new SecureRandom();


    private PasswordUtil() {
    }


    /**
     * Class chứa kết quả sau khi hash password.
     */
    public static class PasswordRecord {

        private final String hash;

        private final String salt;

        private final int iterations;


        public PasswordRecord(
                String hash,
                String salt,
                int iterations) {

            this.hash = hash;

            this.salt = salt;

            this.iterations = iterations;
        }


        public String getHash() {
            return hash;
        }


        public String getSalt() {
            return salt;
        }


        public int getIterations() {
            return iterations;
        }
    }


    /**
     * Hash password bằng SHA-256 kết hợp Salt ngẫu nhiên (FIPS 180-4).
     * Tạo ra mã băm Hex 64 ký tự chuẩn mực và salt Base64.
     */
    public static PasswordRecord hashPasswordSHA256(
            String password) {

        String salt = SHA256Util.generateSalt();
        String hash = SHA256Util.hashWithSalt(password, salt);

        return new PasswordRecord(
                hash,
                salt,
                1
        );
    }

    /**
     * Hash password mặc định cho tài khoản mới bằng SHA-256 + Salt.
     */
    public static PasswordRecord hashPassword(
            String password)
            throws GeneralSecurityException {

        return hashPasswordSHA256(password);
    }


    /**
     * Kiểm tra password người dùng nhập
     * có khớp với password trong Database hay không.
     * Tự động tương thích cả tài khoản SHA-256 (iterations = 1, 64 ký tự hex)
     * lẫn tài khoản PBKDF2-HMAC-SHA256 (nhiều iterations).
     */
    public static boolean verifyPassword(
            String password,
            String storedHash,
            String storedSalt,
            int iterations)
            throws GeneralSecurityException {

        if (password == null || storedHash == null || storedSalt == null) {
            return false;
        }

        // Nhận diện chuẩn SHA-256 + Salt (iterations <= 1 hoặc chuỗi hex 64 ký tự)
        if (iterations <= 1 || (storedHash.length() == 64 && isHex(storedHash))) {
            return SHA256Util.verifyWithSalt(password, storedSalt, storedHash);
        }

        // Ngược lại, xác thực theo chuẩn PBKDF2WithHmacSHA256
        try {
            byte[] salt =
                    Base64.getDecoder()
                            .decode(storedSalt);

            byte[] expectedHash =
                    Base64.getDecoder()
                            .decode(storedHash);

            byte[] actualHash =
                    pbkdf2(
                            password,
                            salt,
                            iterations
                    );

            return MessageDigest.isEqual(
                    expectedHash,
                    actualHash
            );
        } catch (IllegalArgumentException e) {
            // Dự phòng nếu storedHash hoặc salt là dạng chuỗi thường
            return SHA256Util.verifyWithSalt(password, storedSalt, storedHash);
        }
    }

    /**
     * Kiểm tra một chuỗi có phải mã Hexadecimal hợp lệ hay không.
     */
    private static boolean isHex(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean isHexDigit = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!isHexDigit) {
                return false;
            }
        }
        return true;
    }


    /**
     * Thực hiện PBKDF2.
     */
    private static byte[] pbkdf2(
            String password,
            byte[] salt,
            int iterations)
            throws GeneralSecurityException {

        PBEKeySpec spec =
                new PBEKeySpec(
                        password.toCharArray(),
                        salt,
                        iterations,
                        HASH_LENGTH
                );


        try {

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            ALGORITHM
                    );


            return factory
                    .generateSecret(spec)
                    .getEncoded();

        } finally {

            // Xóa password khỏi PBEKeySpec
            // càng sớm càng tốt.
            spec.clearPassword();
        }
    }
}