/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.nio.charset.StandardCharsets;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

/**
 * ClientConnection.java
 *
 * Quản lý kết nối TCP giữa giao diện Client và Server.
 *
 * Nhiệm vụ:
 * 1. Kết nối tới Server mà không làm treo giao diện.
 * 2. Gửi dữ liệu bằng UTF-8.
 * 3. Nhận dữ liệu trong một Reader Thread duy nhất.
 * 4. Thông báo cho giao diện khi nhận được dữ liệu.
 * 5. Thông báo khi kết nối thành công, mất kết nối hoặc gặp lỗi.
 *
 * Lưu ý:
 * - Class này không tạo JFrame.
 * - Class này không xử lý trực tiếp LOGIN, REGISTER hay CHAT.
 * - Các màn hình sẽ sử dụng Protocol để tạo lệnh rồi gửi qua đây.
 */
public final class ClientConnection implements AutoCloseable {

    // =====================================================
    // 1. LISTENER
    // =====================================================

    /**
     * Listener giúp GUI nhận thông báo từ lớp kết nối.
     *
     * Các phương thức callback sẽ được gọi trên
     * Swing Event Dispatch Thread (EDT).
     */
    public interface Listener {

        /**
         * Được gọi khi TCP kết nối thành công.
         */
        void onConnected();

        /**
         * Được gọi khi Server gửi một dòng dữ liệu.
         */
        void onMessage(String message);

        /**
         * Được gọi khi kết nối bị đóng.
         */
        void onDisconnected();

        /**
         * Được gọi khi kết nối gặp lỗi.
         */
        void onError(String message);
    }


    // =====================================================
    // 2. CẤU HÌNH KẾT NỐI
    // =====================================================

    private final String serverHost;

    private final int serverPort;

    private final Listener listener;


    // =====================================================
    // 3. SOCKET VÀ STREAM
    // =====================================================

    private volatile Socket socket;

    private volatile BufferedReader reader;

    private volatile PrintWriter writer;


    // =====================================================
    // 4. CÁC THREAD
    // =====================================================

    private volatile Thread connectThread;

    private volatile Thread readerThread;


    // =====================================================
    // 5. TRẠNG THÁI KẾT NỐI
    // =====================================================

    private final AtomicBoolean closeRequested =
            new AtomicBoolean(false);

    private final AtomicBoolean disconnectNotified =
            new AtomicBoolean(false);

    private final AtomicBoolean connectStarted =
            new AtomicBoolean(false);


    // =====================================================
    // 6. CONSTRUCTOR
    // =====================================================

    public ClientConnection(
            String serverHost,
            int serverPort,
            Listener listener) {

        if (serverHost == null || serverHost.isBlank()) {
            throw new IllegalArgumentException(
                    "Server host khong duoc de trong."
            );
        }

        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException(
                    "Server port phai nam trong khoang 1-65535."
            );
        }

        if (listener == null) {
            throw new IllegalArgumentException(
                    "Listener khong duoc null."
            );
        }

        this.serverHost = serverHost;

        this.serverPort = serverPort;

        this.listener = listener;
    }


    // =====================================================
    // 7. KẾT NỐI TỚI SERVER
    // =====================================================

    /**
     * Bắt đầu kết nối trong một Thread nền.
     *
     * Không thực hiện thao tác connect() trên EDT,
     * tránh làm treo cửa sổ Swing.
     */
    public void connect() {

        if (closeRequested.get()) {
            reportError(
                    "Ket noi da duoc dong."
            );
            return;
        }

        if (!connectStarted.compareAndSet(false, true)) {
            reportError(
                    "Ket noi da duoc khoi tao."
            );
            return;
        }

        connectThread = new Thread(
                this::connectInternal,
                "GroupChat-Connect"
        );

        connectThread.start();
    }


    /**
     * Thực hiện kết nối TCP ở Thread nền.
     */
    private void connectInternal() {

        Socket newSocket = new Socket();

        try {

            // -------------------------------------------------
            // 1. Kết nối tới Server.
            //
            // Timeout 5 giây để không chờ vô hạn.
            // -------------------------------------------------
            newSocket.connect(
                    new InetSocketAddress(
                            serverHost,
                            serverPort
                    ),
                    5000
            );

            // Gửi gói tin nhỏ ngay khi cần, giảm độ trễ.
            newSocket.setTcpNoDelay(true);

            if (closeRequested.get()) {
                newSocket.close();
                return;
            }

            // -------------------------------------------------
            // 2. Tạo luồng đọc dữ liệu UTF-8.
            // -------------------------------------------------
            BufferedReader newReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    newSocket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            // -------------------------------------------------
            // 3. Tạo luồng ghi dữ liệu UTF-8.
            // -------------------------------------------------
            PrintWriter newWriter =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    newSocket.getOutputStream(),
                                    StandardCharsets.UTF_8
                            ),
                            true
                    );

            // Lưu Socket và stream vào các trường của class.
            this.socket = newSocket;
            this.reader = newReader;
            this.writer = newWriter;

            // -------------------------------------------------
            // 4. Báo kết nối thành công cho GUI.
            //
            // Callback được đưa về EDT.
            // -------------------------------------------------
            SwingUtilities.invokeLater(
                    () -> listener.onConnected()
            );

            // -------------------------------------------------
            // 5. Tạo đúng một Reader Thread.
            // -------------------------------------------------
            readerThread = new Thread(
                    this::readLoop,
                    "GroupChat-Reader"
            );

            readerThread.start();

        } catch (IOException e) {

            try {
                newSocket.close();
            } catch (IOException ignored) {
            }

            if (!closeRequested.get()) {
                reportError(
                        "Khong the ket noi Server: "
                        + e.getMessage()
                );
            }

            closeRequested.set(true);

            notifyDisconnected();
        }
    }


    // =====================================================
    // 8. NHẬN DỮ LIỆU — CHỈ MỘT READER THREAD
    // =====================================================

    /**
     * Đọc dữ liệu liên tục từ Server.
     *
     * Không được tạo thêm Reader Thread khác để đọc
     * cùng BufferedReader.
     */
    private void readLoop() {

        try {

            String message;

            while (
                    !closeRequested.get()
                    && (message = reader.readLine()) != null
            ) {

                // Mỗi dòng Server gửi là một protocol message.
                // Chuyển callback về EDT để GUI cập nhật an toàn.
                final String receivedMessage = message;

                SwingUtilities.invokeLater(
                        () -> listener.onMessage(
                                receivedMessage
                        )
                );
            }

        } catch (IOException e) {

            if (!closeRequested.get()) {
                reportError(
                        "Loi khi nhan du lieu tu Server: "
                        + e.getMessage()
                );
            }

        } finally {

            closeRequested.set(true);

            closeResources();

            notifyDisconnected();
        }
    }


    // =====================================================
    // 9. GỬI DỮ LIỆU TỚI SERVER
    // =====================================================

    /**
     * Gửi một dòng protocol đến Server.
     *
     * Ví dụ:
     * LOGIN|phuc123|encodedPassword
     *
     * @param message một dòng dữ liệu protocol
     * @return true nếu PrintWriter chưa phát hiện lỗi ghi
     */
    public synchronized boolean send(String message) {

        if (message == null || message.isEmpty()) {
            reportError(
                    "Du lieu gui khong duoc de trong."
            );
            return false;
        }

        if (
                closeRequested.get()
                || writer == null
                || socket == null
                || socket.isClosed()
        ) {

            reportError(
                    "Chua co ket noi toi Server."
            );

            return false;
        }

        try {

            // Một dòng dữ liệu tương ứng một protocol message.
            writer.println(message);

            // PrintWriter không tự ném IOException
            // trong phần lớn thao tác ghi; phải kiểm tra checkError().
            if (writer.checkError()) {

                reportError(
                        "Khong the gui du lieu toi Server."
                );

                close();

                return false;
            }

            return true;

        } catch (Exception e) {

            reportError(
                    "Loi khi gui du lieu: "
                    + e.getMessage()
            );

            close();

            return false;
        }
    }


    // =====================================================
    // 10. KIỂM TRA TRẠNG THÁI
    // =====================================================

    /**
     * Cho biết kết nối có thể sử dụng hay không.
     */
    public boolean isConnected() {

        Socket currentSocket = socket;

        return !closeRequested.get()
                && currentSocket != null
                && currentSocket.isConnected()
                && !currentSocket.isClosed()
                && writer != null;
    }


    // =====================================================
    // 11. THÔNG BÁO LỖI TRÊN EDT
    // =====================================================

    private void reportError(String message) {

        SwingUtilities.invokeLater(
                () -> listener.onError(message)
        );
    }


    // =====================================================
    // 12. THÔNG BÁO NGẮT KẾT NỐI
    // =====================================================

    private void notifyDisconnected() {

        // Tránh thông báo ngắt kết nối nhiều lần.
        if (
                disconnectNotified.compareAndSet(
                        false,
                        true
                )
        ) {

            SwingUtilities.invokeLater(
                    () -> listener.onDisconnected()
            );
        }
    }


    // =====================================================
    // 13. ĐÓNG SOCKET VÀ STREAM
    // =====================================================

    /**
     * Đóng tài nguyên mạng.
     *
     * Khi đóng Socket, thao tác readLine() đang chờ
     * thường sẽ được giải phóng, giúp Reader Thread kết thúc.
     */
    private void closeResources() {

        Socket currentSocket = socket;

        if (
                currentSocket != null
                && !currentSocket.isClosed()
        ) {

            try {
                currentSocket.close();
            } catch (IOException ignored) {
            }
        }

        // Socket đã đóng thì các stream gắn với nó
        // cũng được giải phóng.
    }


    /**
     * Đóng kết nối thủ công.
     *
     * Có thể gọi từ nút đăng xuất hoặc lúc đóng ứng dụng.
     */
    @Override
    public void close() {

        closeRequested.set(true);

        closeResources();

        notifyDisconnected();
    }
}
