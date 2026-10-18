package security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SHA256Util:
 *
 * Tiện ích bảo mật chuẩn SHA-256 (Secure Hash Algorithm 256-bit / FIPS 180-4).
 *
 * Cung cấp:
 * 1. Băm dữ liệu sang chuỗi Hex 64 ký tự tiêu chuẩn.
 * 2. Băm mật khẩu kết hợp Salt ngẫu nhiên chống tấn công Rainbow Table / Dictionary.
 * 3. So sánh mã băm an toàn theo thời gian hằng số (Time-Constant Equality)
 *    chống tấn công kênh kề (Timing Attack).
 * 4. Tính toán mã băm kiểm tra toàn vẹn (Integrity Checksum) cho gói tin và dữ liệu.
 */
public final class SHA256Util {

    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_SALT_BYTES = 16;

    private SHA256Util() {
        // Lớp tiện ích không khởi tạo đối tượng.
    }

    /**
     * Băm một chuỗi văn bản bằng thuật toán SHA-256.
     *
     * @param input chuỗi đầu vào (UTF-8)
     * @return chuỗi Hex 64 ký tự viết thường
     */
    public static String hash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Dữ liệu đầu vào không được null.");
        }
        byte[] hashBytes = hashRaw(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hashBytes);
    }

    /**
     * Băm mảng byte bằng SHA-256.
     *
     * @param data mảng byte đầu vào
     * @return mảng byte kết quả (32 bytes = 256 bits)
     */
    public static byte[] hashRaw(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Dữ liệu mảng byte không được null.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hệ thống không hỗ trợ thuật toán " + ALGORITHM, e);
        }
    }

    /**
     * Sinh Salt ngẫu nhiên có độ dài mặc định (16 bytes) được mã hóa Base64.
     *
     * @return chuỗi Salt Base64
     */
    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_BYTES);
    }

    /**
     * Sinh Salt ngẫu nhiên với số byte tùy chọn được mã hóa Base64.
     *
     * @param byteLength số byte của salt (khuyến nghị >= 16)
     * @return chuỗi Salt Base64
     */
    public static String generateSalt(int byteLength) {
        if (byteLength <= 0) {
            byteLength = DEFAULT_SALT_BYTES;
        }
        byte[] salt = new byte[byteLength];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Băm mật khẩu kết hợp Salt bằng SHA-256.
     * Công thức: SHA-256(Salt + ":" + Password)
     *
     * @param password mật khẩu gốc
     * @param salt chuỗi salt (Base64 hoặc chuỗi bất kỳ)
     * @return chuỗi Hex 64 ký tự
     */
    public static String hashWithSalt(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Mật khẩu và Salt không được null.");
        }
        String combined = salt + ":" + password;
        return hash(combined);
    }

    /**
     * Kiểm tra đối chiếu mật khẩu nhập vào với mã hash SHA-256 có Salt đã lưu.
     * Sử dụng so sánh mảng byte thời gian hằng số để chống Timing Attack.
     *
     * @param password mật khẩu nhập vào
     * @param salt chuỗi salt đã lưu
     * @param expectedHash mã hash mong đợi đã lưu trong Database
     * @return true nếu mật khẩu khớp, ngược lại false
     */
    public static boolean verifyWithSalt(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        String actualHash = hashWithSalt(password, salt);
        return constantTimeEquals(actualHash, expectedHash);
    }

    /**
     * Kiểm tra đối chiếu chuỗi băm trực tiếp (không Salt).
     *
     * @param input chuỗi đầu vào
     * @param expectedHash chuỗi băm mong đợi
     * @return true nếu khớp
     */
    public static boolean verify(String input, String expectedHash) {
        if (input == null || expectedHash == null) {
            return false;
        }
        String actualHash = hash(input);
        return constantTimeEquals(actualHash, expectedHash);
    }

    /**
     * Tính toán mã băm kiểm tra tính toàn vẹn (Checksum) cho một thông điệp.
     *
     * @param message nội dung thông điệp
     * @return chuỗi Hex 64 ký tự
     */
    public static String calculateChecksum(String message) {
        return hash(message == null ? "" : message);
    }

    /**
     * So sánh 2 chuỗi an toàn chống Timing Attack bằng MessageDigest.isEqual.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] bytesA = a.getBytes(StandardCharsets.UTF_8);
        byte[] bytesB = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(bytesA, bytesB);
    }

    /**
     * Chuyển mảng byte sang chuỗi Hex 16 tiến.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
