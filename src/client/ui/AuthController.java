/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import common.Protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/**
 * AuthController:
 *
 * 1. Quản lý một ClientConnection duy nhất.
 * 2. Gửi lệnh LOGIN và REGISTER.
 * 3. Tiếp nhận phản hồi từ Server.
 * 4. Điều hướng kết quả đến LoginFrame / RegisterFrame.
 * 5. Chuẩn bị cơ chế chuyển dữ liệu cho ChatFrame ở Checkpoint 4.4.
 *
 * Không tạo BufferedReader hoặc Reader Thread tại đây.
 * Việc đọc dữ liệu TCP do ClientConnection đảm nhiệm.
 */
public class AuthController
        implements ClientConnection.Listener {

    // ==========================================================
    // KẾT NỐI DÙNG CHUNG
    // ==========================================================

    // Thông tin Server để có thể kết nối lại sau khi đăng xuất.
    private final String serverHost;
    private final int serverPort;

    // Kết nối TCP đang dùng chung cho ứng dụng.
    private ClientConnection connection;

    // Cửa sổ chat sau khi đăng nhập thành công.
    private ChatFrame chatFrame;

    // Username hiện đang đăng nhập.
    private String currentUsername = "";

    // Phân biệt thao tác đăng xuất chủ động với lỗi mạng.
    private boolean logoutRequested = false;

    // Các cửa sổ giao diện.
    private final LoginFrame loginFrame;

    private RegisterFrame registerFrame;

    // Trạng thái đăng nhập phía Client.
    private boolean authenticated = false;

    // Lưu thông tin đăng nhập thành công để tự động kết nối lại khi mất mạng
    private String savedUsername = "";
    private String savedEncodedPassword = "";
    private javax.swing.Timer reconnectTimer;
    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;

    // Khi chưa có ChatFrame, tạm giữ dữ liệu nhận được sau login.
    private final List<String> pendingMessages =
            new ArrayList<>();

    // Checkpoint 4.4 sẽ gắn ChatFrame vào đây.
    private Consumer<String> chatMessageHandler;

    private boolean shuttingDown = false;

    // ==========================================================
    // CONSTRUCTOR
    // ==========================================================

    public AuthController(
            String serverHost,
            int serverPort) {

        this.serverHost = serverHost;
        this.serverPort = serverPort;

        // Vẫn chỉ tạo một kết nối dùng chung cho các cửa sổ.
        connection = new ClientConnection(
                serverHost,
                serverPort,
                this
        );

        loginFrame = new LoginFrame(this);
    }

    // ==========================================================
    // KHỞI ĐỘNG ỨNG DỤNG
    // ==========================================================

    public void start() {

        loginFrame.setVisible(true);

        // connect() sử dụng cơ chế kết nối nền
        // của ClientConnection hiện tại.
        connection.connect();
    }

    // ==========================================================
    // ĐĂNG NHẬP
    // ==========================================================

    public void login(
            String username,
            char[] password) {

        username = username.trim();

        // Kiểm tra dữ liệu trước khi gửi lên Server.
        if (!username.matches("[A-Za-z0-9_]{3,20}")) {

            erasePassword(password);

            loginFrame.setBusy(false);

            loginFrame.showStatus(
                    "Username phải gồm 3–20 ký tự: "
                    + "A-Z, a-z, 0-9 hoặc dấu gạch dưới.",
                    false
            );

            return;
        }

        if (password == null
                || password.length < 8
                || password.length > 128) {

            erasePassword(password);

            loginFrame.setBusy(false);

            loginFrame.showStatus(
                    "Password phải từ 8 đến 128 ký tự.",
                    false
            );

            return;
        }

        if (!connection.isConnected()) {

            erasePassword(password);

            loginFrame.setBusy(false);

            loginFrame.showStatus(
                    "Chưa kết nối được đến Server. "
                    + "Hãy kiểm tra Server và thử lại.",
                    false
            );

            return;
        }

        try {

            currentUsername = username;

            loginFrame.setBusy(true);

            loginFrame.showStatus(
                    "Đang xác thực tài khoản...",
                    null
            );

            // Base64 để tương thích với giao thức Server hiện tại.
            String encodedPassword =
                    encodePassword(password);

            savedUsername = username;
            savedEncodedPassword = encodedPassword;

            /*
             * Cú pháp chính xác từ ClientHandler:
             *
             * LOGIN|username|encodedPassword
             */
            String request = Protocol.build(
                    Protocol.LOGIN,
                    username,
                    encodedPassword
            );

            connection.send(request);

        } catch (RuntimeException e) {

            erasePassword(password);

            loginFrame.setBusy(false);

            loginFrame.showStatus(
                    "Không thể gửi yêu cầu đăng nhập.",
                    false
            );
        }
    }

    // ==========================================================
    // ĐĂNG KÝ
    // ==========================================================

    public void register(
            String username,
            String displayName,
            char[] password) {

        username = username.trim();

        displayName = displayName.trim();

        if (!username.matches("[A-Za-z0-9_]{3,20}")) {

            erasePassword(password);

            registerFrame.setBusy(false);

            registerFrame.showStatus(
                    "Username phải gồm 3–20 ký tự: "
                    + "A-Z, a-z, 0-9 hoặc dấu gạch dưới.",
                    false
            );

            return;
        }

        if (displayName.isEmpty()) {

            erasePassword(password);

            registerFrame.setBusy(false);

            registerFrame.showStatus(
                    "Vui lòng nhập tên hiển thị.",
                    false
            );

            return;
        }

        /*
         * Dấu | là dấu phân cách các trường của Protocol.
         * Không cho tên hiển thị chứa dấu này.
         */
        if (displayName.contains("|")
                || displayName.contains("\n")
                || displayName.contains("\r")) {

            erasePassword(password);

            registerFrame.setBusy(false);

            registerFrame.showStatus(
                    "Tên hiển thị chứa ký tự không hợp lệ.",
                    false
            );

            return;
        }

        if (password == null
                || password.length < 8
                || password.length > 128) {

            erasePassword(password);

            registerFrame.setBusy(false);

            registerFrame.showStatus(
                    "Password phải từ 8 đến 128 ký tự.",
                    false
            );

            return;
        }

        if (!connection.isConnected()) {

            erasePassword(password);

            registerFrame.setBusy(false);

            registerFrame.showStatus(
                    "Chưa kết nối được đến Server. "
                    + "Hãy kiểm tra Server và thử lại.",
                    false
            );

            return;
        }

        try {

            registerFrame.setBusy(true);

            registerFrame.showStatus(
                    "Đang gửi yêu cầu đăng ký...",
                    null
            );

            String encodedPassword =
                    encodePassword(password);

            /*
             * Cú pháp chính xác từ ClientHandler:
             *
             * REGISTER|username|displayName|encodedPassword
             */
            String request = Protocol.build(
                    Protocol.REGISTER,
                    username,
                    displayName,
                    encodedPassword
            );

            connection.send(request);

        } catch (RuntimeException e) {

            erasePassword(password);

            registerFrame.setBusy(false);

            registerFrame.showStatus(
                    "Không thể gửi yêu cầu đăng ký.",
                    false
            );
        }
    }

    // ==========================================================
    // ĐĂNG XUẤT
    // ==========================================================

    public void logout() {

        if (!authenticated || logoutRequested) {
            return;
        }

        logoutRequested = true;

        if (chatFrame != null) {
            chatFrame.setBusy(true);

            chatFrame.showConnectionNotice(
                    "Đang đăng xuất...",
                    true
            );
        }

        if (connection.isConnected()) {

            // Server sẽ kết thúc phiên và đóng kết nối hiện tại.
            connection.send(
                    Protocol.build(Protocol.LOGOUT)
            );

        } else {

            // Nếu kết nối đã mất, đóng phiên phía giao diện
            // và chuẩn bị tạo kết nối mới.
            finishLogout();
        }
    }

    // ==========================================================
    // HOÀN TẤT ĐĂNG XUẤT
    // Được gọi khi kết nối cũ đã ngắt.
    // ==========================================================

    private void finishLogout() {

        stopReconnectTimer();
        savedUsername = "";
        savedEncodedPassword = "";
        isReconnecting = false;
        reconnectAttempts = 0;

        authenticated = false;
        logoutRequested = false;

        chatMessageHandler = null;
        pendingMessages.clear();

        if (chatFrame != null) {
            chatFrame.dispose();
            chatFrame = null;
        }

        if (connection != null) {
            connection.close();
        }

        // Đặt lại trạng thái LoginFrame để có thể đăng nhập lại.
        loginFrame.resetAfterLogout();
        loginFrame.setVisible(true);

        /*
         * Tạo một kết nối mới sau khi phiên cũ kết thúc.
         * Không giữ hai phiên TCP hoạt động đồng thời.
         */
        connection = new ClientConnection(
                serverHost,
                serverPort,
                this
        );

        connection.connect();
    }

    // ==========================================================
    // MÃ HÓA BIỂU DIỄN PASSWORD THÀNH BASE64
    // ==========================================================

    private static String encodePassword(char[] password) {

        if (password == null) {
            return "";
        }

        try {

            byte[] bytes =
                    new String(password)
                            .getBytes(StandardCharsets.UTF_8);

            try {

                return Base64.getEncoder()
                        .encodeToString(bytes);

            } finally {

                // Xóa nội dung byte sau khi đã mã hóa biểu diễn.
                Arrays.fill(bytes, (byte) 0);
            }

        } finally {

            // Không giữ lại mảng password nhận từ ô nhập.
            Arrays.fill(password, '\0');
        }
    }

    private static void erasePassword(char[] password) {

        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    // ==========================================================
    // ĐIỀU HƯỚNG CỬA SỔ
    // ==========================================================

    public void openRegister() {

        if (authenticated || shuttingDown) {
            return;
        }

        loginFrame.setVisible(false);

        if (registerFrame != null) {
            registerFrame.dispose();
        }

        registerFrame = new RegisterFrame(this);

        registerFrame.setVisible(true);
    }

    public void returnToLogin() {

        if (registerFrame != null) {

            RegisterFrame oldFrame = registerFrame;

            registerFrame = null;

            oldFrame.dispose();
        }

        if (!shuttingDown && !authenticated) {
            loginFrame.setVisible(true);

            loginFrame.toFront();
        }
    }

    // ==========================================================
    // XỬ LÝ PHẢN HỒI TỪ CLIENTCONNECTION
    // ==========================================================

    @Override
    public void onConnected() {

        if (shuttingDown) {
            return;
        }

        if (isReconnecting && !savedUsername.isEmpty() && !savedEncodedPassword.isEmpty()) {
            if (chatFrame != null) {
                chatFrame.showConnectionNotice("Đã kết nối lại Server. Đang tự động xác thực phiên...", true);
            }
            // Tự động đăng nhập lại bằng thông tin phiên đã lưu
            String request = Protocol.build(
                    Protocol.LOGIN,
                    savedUsername,
                    savedEncodedPassword
            );
            connection.send(request);
            return;
        }

        showAuthStatus(
                "Đã kết nối đến Server.",
                true
        );
    }

    @Override
    public void onMessage(String message) {

        if (message == null || message.isEmpty()) {
            return;
        }

        /*
         * Chỉ tách ở dấu | đầu tiên.
         * Phần nội dung sau đó được giữ nguyên.
         */
        String[] parts = message.split("\\|", 2);

        String type = parts[0];

        String payload =
                parts.length > 1
                        ? parts[1]
                        : "";

        // ------------------------------------------
        // ĐĂNG KÝ THÀNH CÔNG
        // ------------------------------------------

        if (type.equals(Protocol.REGISTER_SUCCESS)) {

            if (registerFrame != null) {

                registerFrame.showResult(
                        payload,
                        true
                );
            }

            return;
        }

        // ------------------------------------------
        // ĐĂNG KÝ THẤT BẠI
        // ------------------------------------------

        if (type.equals(Protocol.REGISTER_FAILED)) {

            if (registerFrame != null) {

                registerFrame.showResult(
                        payload,
                        false
                );
            }

            return;
        }

        // ------------------------------------------
        // ĐĂNG NHẬP THÀNH CÔNG
        // ------------------------------------------

        if (type.equals(Protocol.LOGIN_SUCCESS)) {

            authenticated = true;
            logoutRequested = false;

            if (isReconnecting && chatFrame != null) {
                stopReconnectTimer();
                isReconnecting = false;
                reconnectAttempts = 0;

                chatFrame.setBusy(false);
                chatFrame.showConnectionNotice("Đã kết nối lại thành công! Bạn có thể tải lại lịch sử phòng.", true);
                chatFrame.onReconnected();

                // Đồng bộ lại danh sách phòng, thành viên và phòng hiện tại
                sendProtocol(Protocol.build(Protocol.GET_ROOMS));
                sendProtocol(Protocol.build(Protocol.JOIN_ROOM, chatFrame.getCurrentRoom()));
                sendProtocol(Protocol.build(Protocol.GET_USERS));
                sendProtocol(Protocol.build(Protocol.GET_MY_ROLE));
                return;
            }

            stopReconnectTimer();
            isReconnecting = false;
            reconnectAttempts = 0;

            // Ẩn màn hình đăng nhập sau khi xác thực thành công.
            loginFrame.loginSucceeded(payload);
            loginFrame.setVisible(false);

            // Đóng màn hình đăng ký nếu nó còn tồn tại.
            if (registerFrame != null) {
                registerFrame.dispose();
                registerFrame = null;
            }

            // Tạo ChatFrame cho phiên đăng nhập hiện tại.
            chatFrame = new ChatFrame(
                    this,
                    currentUsername
            );

            /*
             * Gắn ChatFrame vào đầu nhận thông điệp.
             * Hàm này cũng chuyển các thông điệp đang chờ
             * sang ChatFrame nếu có.
             */
            setAuthenticatedMessageHandler(
                    chatFrame::handleServerMessage
            );

            chatFrame.setVisible(true);

            chatFrame.showConnectionNotice(
                    payload,
                    true
            );

            /*
             * Yêu cầu Server gửi danh sách phòng và thành viên.
             * Đây là các lệnh đi qua ClientConnection hiện tại,
             * không mở thêm Socket.
             */
            sendProtocol(
                    Protocol.build(Protocol.GET_ROOMS)
            );

            sendProtocol(
                    Protocol.build(Protocol.GET_USERS)
                    
            );
            
            // Yêu cầu vai trò của tài khoản trong phòng mặc định.
            sendProtocol(
                   Protocol.build(Protocol.GET_MY_ROLE)
            );
            return;
        }

        // ------------------------------------------
        // ĐĂNG NHẬP THẤT BẠI
        // ------------------------------------------

        if (type.equals(Protocol.LOGIN_FAILED)) {

            loginFrame.setBusy(false);

            loginFrame.showStatus(
                    payload,
                    false
            );

            return;
        }

        // ------------------------------------------
        // THÔNG BÁO SYSTEM
        // ------------------------------------------

        if (type.equals(Protocol.SYSTEM)) {

            if (authenticated) {

                dispatchAuthenticatedMessage(message);

            } else {

                showAuthStatus(
                        payload,
                        true
                );
            }

            return;
        }

        // ------------------------------------------
        // LỖI TỪ SERVER
        // ------------------------------------------

        if (type.equals(Protocol.ERROR)) {

            if (authenticated) {

                dispatchAuthenticatedMessage(message);

            } else {

                showAuthStatus(
                        payload,
                        false
                );
            }

            return;
        }

        // ------------------------------------------
        // LỊCH SỬ VÀ CÁC THÔNG ĐIỆP KHÁC
        // ------------------------------------------

        if (authenticated) {

            dispatchAuthenticatedMessage(message);
        }
    }

    @Override
    public void onDisconnected() {

        if (shuttingDown) {
            return;
        }

        // Trường hợp người dùng chủ động đăng xuất.
        if (logoutRequested) {

            finishLogout();

            return;
        }

        // Trường hợp mất mạng bất ngờ trong lúc chat.
        if (authenticated && chatFrame != null) {

            chatFrame.setBusy(true);

            chatFrame.showConnectionNotice(
                    "Mất kết nối mạng. Đang tự động kết nối lại...",
                    false
            );

            startReconnectTimer();
            return;
        }

        // Trường hợp mất kết nối khi đang ở màn hình đăng nhập.
        showAuthStatus(
                "Kết nối đến Server đã bị ngắt.",
                false
        );

        loginFrame.setBusy(false);

        if (registerFrame != null) {
            registerFrame.setBusy(false);
        }
    }

    private void startReconnectTimer() {
        if (reconnectTimer != null && reconnectTimer.isRunning()) {
            return;
        }
        isReconnecting = true;
        reconnectAttempts = 0;

        reconnectTimer = new javax.swing.Timer(3000, e -> attemptReconnect());
        reconnectTimer.setRepeats(true);
        reconnectTimer.start();
    }

    private void attemptReconnect() {
        if (shuttingDown || logoutRequested || !authenticated) {
            stopReconnectTimer();
            return;
        }

        reconnectAttempts++;
        if (chatFrame != null) {
            chatFrame.showConnectionNotice(
                    "Mất kết nối mạng. Đang tự động kết nối lại (lần " + reconnectAttempts + ")...",
                    false
            );
        }

        try {
            if (connection != null) {
                connection.close();
            }
            connection = new ClientConnection(serverHost, serverPort, this);
            connection.connect();
        } catch (Exception ex) {
            // Sẽ thử lại ở nhịp Timer tiếp theo
        }
    }

    private void stopReconnectTimer() {
        if (reconnectTimer != null) {
            reconnectTimer.stop();
            reconnectTimer = null;
        }
    }

    @Override
    public void onError(String message) {

        if (shuttingDown) {
            return;
        }

        showAuthStatus(
                "Lỗi kết nối: " + message,
                false
        );
    }

    // ==========================================================
    // HIỂN THỊ THÔNG BÁO Ở CỬA SỔ ĐANG MỞ
    // ==========================================================

    private void showAuthStatus(
            String message,
            boolean success) {

        if (authenticated && chatFrame != null) {

            chatFrame.showConnectionNotice(
                    message,
                    success
            );

            return;
        }

        if (registerFrame != null
                && registerFrame.isVisible()) {

            registerFrame.showStatus(
                    message,
                    success
            );

        } else {

            loginFrame.showStatus(
                    message,
                    success
            );
        }
    }

    // ==========================================================
    // CHUYỂN THÔNG ĐIỆP ĐÃ ĐĂNG NHẬP ĐẾN CHATFRAME
    // ==========================================================

    private void dispatchAuthenticatedMessage(
            String message) {

        if (chatMessageHandler != null) {

            chatMessageHandler.accept(message);

        } else {

            /*
             * Tại Checkpoint 4.3, ChatFrame chưa tồn tại.
             * Vì vậy tạm giữ HISTORY/SYSTEM và thông điệp khác.
             *
             * Đến Checkpoint 4.4, ChatFrame sẽ nhận lại hàng đợi
             * thông qua setAuthenticatedMessageHandler().
             */
            pendingMessages.add(message);
        }
    }

    public void setAuthenticatedMessageHandler(
            Consumer<String> handler) {

        chatMessageHandler = handler;

        if (handler != null) {

            List<String> queued =
                    new ArrayList<>(pendingMessages);

            pendingMessages.clear();

            for (String message : queued) {
                handler.accept(message);
            }
        }
    }

    /**
     * Cho phép ChatFrame ở Checkpoint 4.4 gửi lệnh
     * thông qua đúng ClientConnection đang dùng.
     *
     * Không tạo kết nối TCP thứ hai.
     */
    public void sendProtocol(String message) {

        if (connection.isConnected()) {

            connection.send(message);

        } else {

            showAuthStatus(
                    "Chưa kết nối được đến Server.",
                    false
            );
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    // ==========================================================
    // ĐÓNG ỨNG DỤNG
    // ==========================================================

    public void shutdown() {

        if (shuttingDown) {
            return;
        }

        shuttingDown = true;

        /*
         * Nếu đã đăng nhập thì yêu cầu Server xử lý LOGOUT
         * trước khi đóng kết nối.
         */
        if (connection.isConnected()) {

            if (authenticated) {

                connection.send(
                        Protocol.build(
                                Protocol.LOGOUT
                        )
                );
            }

            connection.close();
        }

        if (registerFrame != null) {
            registerFrame.dispose();
            registerFrame = null;
        }

        if (chatFrame != null) {
            chatFrame.dispose();
            chatFrame = null;
        }

        loginFrame.dispose();
    }

    // ==========================================================
    // ĐIỂM BẮT ĐẦU CHẠY ỨNG DỤNG
    // ==========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            UITheme.install();

            // Mặc định kết nối tới Server qua IP ZeroTier: 10.148.92.193
            // Có thể truyền IP qua tham số dòng lệnh args[0] hoặc -Dserver.host=...
            String host = (args != null && args.length > 0 && !args[0].isBlank())
                    ? args[0].trim()
                    : System.getProperty("server.host", "10.148.92.193");

            int port = 5000;
            if (args != null && args.length > 1) {
                try {
                    port = Integer.parseInt(args[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }

            AuthController controller =
                    new AuthController(
                            host,
                            port
                    );

            controller.start();
        });
    }
}