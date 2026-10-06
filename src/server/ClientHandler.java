/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
/**
 *
 * @author DELL
 */
    public class ClientHandler implements Runnable {

    private final Socket socket;

    private final ClientManager clientManager;

    private final RoomManager roomManager;

    private BufferedReader reader;

    private PrintWriter writer;

    // Username sau khi đăng nhập.
    private String username;

    // Phòng hiện tại.
    private String currentRoom = "Lobby";

    // Trạng thái hoạt động của ClientHandler.
    private boolean running = true;


    public ClientHandler(
            Socket socket,
            ClientManager clientManager,
            RoomManager roomManager) {

        this.socket = socket;

        this.clientManager = clientManager;

        this.roomManager = roomManager;
    }


    /**
     * Hàm run() được gọi khi Thread bắt đầu.
     */
    @Override
    public void run() {

        try {

            // =================================================
            // LUỒNG ĐỌC DỮ LIỆU TỪ CLIENT
            //
            // Sử dụng UTF-8 một cách tường minh.
            // =================================================

            reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );


            // =================================================
            // LUỒNG GỬI DỮ LIỆU TỚI CLIENT
            //
            // Cũng sử dụng UTF-8.
            // =================================================

            writer = new PrintWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    ),
                    true
            );


            System.out.println(
                    "[SERVER] Client connected: "
                    + socket.getInetAddress()
                    + ":"
                    + socket.getPort()
            );


            send(
                    Protocol.SYSTEM,
                    "Kết nối Server thành công!"
            );


            send(
                    Protocol.SYSTEM,
                    "Vui lòng đăng nhập bằng LOGIN|TênCủaBạn"
            );


            String message;


            /**
             * Liên tục chờ dữ liệu từ Client.
             */
            while (
                    running
                    && (message = reader.readLine()) != null
            ) {

                System.out.println(
                        "[RECEIVE] "
                        + socket.getPort()
                        + " -> "
                        + message
                );

                handleMessage(message);
            }


        } catch (IOException e) {

            System.out.println(
                    "[SERVER] Client mất kết nối: "
                    + socket.getPort()
            );


        } finally {

            disconnect();
        }
    }


    /**
     * Phân tích message nhận từ Client.
     */
    private void handleMessage(String message) {

        if (message == null || message.isEmpty()) {
            return;
        }


        String[] parts =
                message.split("\\|", 2);


        String command = parts[0];


        switch (command) {

            case Protocol.LOGIN:

                handleLogin(parts);

                break;


            case Protocol.JOIN_ROOM:

                handleJoinRoom(parts);

                break;


            case Protocol.GET_ROOMS:

                handleGetRooms();

                break;


            case Protocol.GET_USERS:

                handleGetUsers();

                break;


            case Protocol.SEND_MESSAGE:

                handleSendMessage(parts);

                break;


            case Protocol.LOGOUT:

                handleLogout();

                break;


            default:

                sendError(
                        "Lệnh không hợp lệ."
                );
        }
    }


    /**
     * LOGIN|username
     */
    private void handleLogin(
            String[] parts) {

        if (username != null) {

            sendError(
                    "Bạn đã đăng nhập."
            );

            return;
        }


        if (parts.length < 2) {

            sendError(
                    "Sai cú pháp. Ví dụ: LOGIN|Phúc"
            );

            return;
        }


        String requestedUsername =
                parts[1].trim();


        if (
                requestedUsername.length() < 3
                || requestedUsername.length() > 20
        ) {

            sendError(
                    "Username phải từ 3 đến 20 ký tự."
            );

            return;
        }


        if (requestedUsername.contains("|")) {

            sendError(
                    "Username không được chứa ký tự |."
            );

            return;
        }


        if (!clientManager.register(
                requestedUsername,
                this)) {

            sendError(
                    "Username này đang được sử dụng."
            );

            return;
        }


        username = requestedUsername;


        // Cho người dùng vào Lobby.
        roomManager.addClient(
                "Lobby",
                this
        );


        send(
                Protocol.LOGIN_SUCCESS,
                "Xin chào " + username
        );


        send(
                Protocol.SYSTEM,
                "Bạn đang ở phòng Lobby."
        );


        roomManager.broadcast(
                "Lobby",
                Protocol.build(
                        Protocol.SYSTEM,
                        username
                        + " đã tham gia Lobby."
                )
        );


        System.out.println(
                "[LOGIN] "
                + username
                + " login thành công."
        );
    }


    /**
     * JOIN_ROOM|TênPhòng
     */
    private void handleJoinRoom(
            String[] parts) {

        if (!checkLogin()) {
            return;
        }


        if (parts.length < 2) {

            sendError(
                    "Sai cú pháp. Ví dụ: JOIN_ROOM|Java"
            );

            return;
        }


        String roomName =
                parts[1].trim();


        if (!roomManager.exists(roomName)) {

            sendError(
                    "Phòng '"
                    + roomName
                    + "' không tồn tại."
            );

            return;
        }


        if (roomName.equals(currentRoom)) {

            sendError(
                    "Bạn đang ở phòng này rồi."
            );

            return;
        }


        String oldRoom =
                currentRoom;


        // Rời phòng cũ.
        roomManager.removeClient(
                oldRoom,
                this
        );


        // Chuyển sang phòng mới.
        currentRoom =
                roomName;


        roomManager.addClient(
                currentRoom,
                this
        );


        // Thông báo cho phòng cũ.
        roomManager.broadcast(
                oldRoom,
                Protocol.build(
                        Protocol.SYSTEM,
                        username
                        + " đã rời phòng."
                )
        );


        // Thông báo cho phòng mới.
        roomManager.broadcast(
                currentRoom,
                Protocol.build(
                        Protocol.SYSTEM,
                        username
                        + " đã tham gia phòng."
                )
        );


        send(
                Protocol.SYSTEM,
                "Bạn đã vào phòng "
                + currentRoom
        );
    }


    /**
     * GET_ROOMS
     */
    private void handleGetRooms() {

        if (!checkLogin()) {
            return;
        }


        String rooms =
                String.join(
                        ", ",
                        roomManager.getRoomNames()
                );


        send(
                Protocol.ROOM_LIST,
                rooms
        );
    }


    /**
     * GET_USERS
     */
    private void handleGetUsers() {

        if (!checkLogin()) {
            return;
        }


        String users =
                String.join(
                        ", ",
                        roomManager.getUserNames(
                                currentRoom
                        )
                );


        send(
                Protocol.USER_LIST,
                currentRoom,
                users
        );
    }


    /**
     * SEND_MESSAGE|Nội dung
     */
    private void handleSendMessage(
            String[] parts) {

        if (!checkLogin()) {
            return;
        }


        if (parts.length < 2) {

            sendError(
                    "Tin nhắn không được rỗng."
            );

            return;
        }


        String content =
                parts[1].trim();


        if (content.isEmpty()) {

            sendError(
                    "Tin nhắn không được rỗng."
            );

            return;
        }


        if (content.length() > 500) {

            sendError(
                    "Tin nhắn tối đa 500 ký tự."
            );

            return;
        }


        /**
         * Broadcast chỉ tới thành viên
         * của phòng hiện tại.
         */
        roomManager.broadcast(
                currentRoom,
                Protocol.build(
                        Protocol.CHAT,
                        currentRoom,
                        username,
                        content
                )
        );
    }


    /**
     * LOGOUT
     */
    private void handleLogout() {

        send(
                Protocol.SYSTEM,
                "Đang đăng xuất..."
        );


        running = false;
    }


    /**
     * Kiểm tra Client đã đăng nhập chưa.
     */
    private boolean checkLogin() {

        if (username == null) {

            sendError(
                    "Bạn chưa đăng nhập."
            );

            return false;
        }


        return true;
    }


    /**
     * Gửi thông báo lỗi.
     */
    private void sendError(
            String message) {

        send(
                Protocol.ERROR,
                message
        );
    }


    /**
     * Gửi dữ liệu tới Client.
     *
     * synchronized:
     *
     * Có thể nhiều Thread cùng gọi send().
     * synchronized giúp tránh việc nhiều luồng
     * ghi dữ liệu lẫn vào nhau.
     */
    public synchronized void send(
            String type,
            String... data) {

        if (writer == null) {
            return;
        }


        writer.println(
                Protocol.build(
                        type,
                        data
                )
        );
    }


    /**
     * Cleanup khi Client ngắt kết nối.
     */
    private void disconnect() {

        if (username != null) {

            String name =
                    username;


            roomManager.removeFromAllRooms(
                    this
            );


            clientManager.remove(
                    name,
                    this
            );


            System.out.println(
                    "[DISCONNECT] "
                    + name
            );
        }


        try {

            if (reader != null) {
                reader.close();
            }

        } catch (IOException ignored) {
        }


        if (writer != null) {
            writer.close();
        }


        try {

            if (
                    socket != null
                    && !socket.isClosed()
            ) {

                socket.close();
            }

        } catch (IOException ignored) {
        }
    }


    public String getUsername() {

        return username;
    }


    public String getCurrentRoom() {

        return currentRoom;
    }
}

