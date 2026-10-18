/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
import database.MessageDAO;
import database.PrivateMessageDAO;
import database.ReactionDAO;
import database.RoomDAO;
import model.Message;
import model.PrivateMessage;
import model.RoomRole;

import common.Protocol;
import model.User;
import security.ValidationUtil;
import security.PasswordUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import database.UserDAO;

/**
 * ClientHandler:
 *
 * Mỗi Client có một ClientHandler + Thread riêng.
 *
 * Phase 2 bổ sung:
 *
 * - REGISTER
 * - LOGIN
 * - SQLite
 * - Password verification
 * - Session user
 */
public class ClientHandler
        implements Runnable {

    private final Socket socket;

    private final ClientManager clientManager;

    private final RoomManager roomManager;
    private final UserDAO userDAO;

    // DAO truy cập lịch sử tin nhắn.
    private final MessageDAO messageDAO;

    // DAO tra cứu ID phòng trong Database.
    private final RoomDAO roomDAO;

    // DAO lưu / lấy cảm xúc tin nhắn.
    private final ReactionDAO reactionDAO;

    // DAO lưu / đọc tin nhắn riêng tư 1-1.
    private final PrivateMessageDAO privateMessageDAO;

    // ID tài khoản đã đăng nhập.
    // Không lấy ID này từ dữ liệu Client gửi lên.
    private int userId;
    private BufferedReader reader;

    private PrintWriter writer;


    // ==========================================================
    // Session
    // ==========================================================

    private String username;

    private String displayName;

    private String currentRoom =
            "Sảnh chính";


    private boolean running =
            true;


    // ==========================================================
    // TIMESTAMP HELPER
    // ==========================================================

    /**
     * Trả về thời gian hiện tại theo định dạng [HH:mm:ss]
     * để in kèm mọi log server.
     */
    private static String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("[yyyy-MM-dd HH:mm:ss]"));
    }


    public ClientHandler(
            Socket socket,
            ClientManager clientManager,
            RoomManager roomManager,
            UserDAO userDAO,
            RoomDAO roomDAO,
            MessageDAO messageDAO,
            ReactionDAO reactionDAO) {

        this(socket, clientManager, roomManager, userDAO, roomDAO, messageDAO, reactionDAO, new PrivateMessageDAO());
    }

    public ClientHandler(
            Socket socket,
            ClientManager clientManager,
            RoomManager roomManager,
            UserDAO userDAO,
            RoomDAO roomDAO,
            MessageDAO messageDAO,
            ReactionDAO reactionDAO,
            PrivateMessageDAO privateMessageDAO) {

        this.socket = socket;
        this.clientManager = clientManager;
        this.roomManager = roomManager;
        this.userDAO = userDAO;

        this.roomDAO = roomDAO;
        this.messageDAO = messageDAO;
        this.reactionDAO = reactionDAO;
        this.privateMessageDAO = privateMessageDAO;
    }


    // ==========================================================
    // RUN
    // ==========================================================

    @Override
    public void run() {

        try {

            // =================================================
            // Client → Server
            // UTF-8
            // =================================================

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );


            // =================================================
            // Server → Client
            // UTF-8
            // =================================================

            writer =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(),
                                    StandardCharsets.UTF_8
                            ),
                            true
                    );


            System.out.println(
                    now() + " [SERVER] Client connected: "
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
                    "Vui lòng đăng ký hoặc đăng nhập."
            );


            String message;


            while (
                    running
                    && (message =
                            reader.readLine()) != null
            ) {

                System.out.println(
                        now() + " [RECEIVE] "
                        + socket.getPort()
                        + " -> "
                        + message
                );


                handleMessage(
                        message
                );
            }


        } catch (IOException e) {

            System.out.println(
                    now() + " [SERVER] Client mất kết nối đột ngột: "
                    + socket.getPort()
            );


        } finally {

            disconnect();
        }
    }


    // ==========================================================
    // XỬ LÝ MESSAGE
    // ==========================================================

    private void handleMessage(
            String message) {

        if (
                message == null
                || message.isEmpty()
        ) {

            return;
        }


        String[] parts =
                message.split("\\|", 4);


        String command =
                parts[0];


        switch (command) {

            case Protocol.REGISTER:

                handleRegister(parts);

                break;


            case Protocol.LOGIN:

                handleLogin(parts);

                break;


            case Protocol.JOIN_ROOM:

                handleJoinRoom(parts);

                break;


            // Checkpoint 5.3: quản lý phòng và vai trò.
            case Protocol.CREATE_ROOM:

                handleCreateRoom(parts);

                break;


            case Protocol.GET_MY_ROLE:

                handleGetMyRole();

                break;


            case Protocol.PROMOTE_ADMIN:

                handlePromoteAdmin(parts);

                break;


            case Protocol.KICK_USER:

                handleKickUser(parts);

                break;


            case Protocol.DELETE_ROOM:

                handleDeleteRoom(parts);

                break;


            case Protocol.GET_ROOMS:

                handleGetRooms();

                break;


            case Protocol.GET_USERS:

                handleGetUsers();

                break;


            case Protocol.SEND_MESSAGE:

                // Chỉ tách tại dấu | đầu tiên để giữ nguyên nội dung tin nhắn.
                handleSendMessage(message.split("\\|", 2));

                break;

            case Protocol.REACT:

                handleReact(parts);

                break;

            case Protocol.SEARCH_MESSAGES:

                // Chỉ tách tại dấu | đầu tiên để giữ nguyên từ khóa.
                handleSearchMessages(message.split("\\|", 2));

                break;

            case Protocol.SEND_PRIVATE:

                // Tách 3 phần: SEND_PRIVATE | targetUsername | content
                handleSendPrivate(message.split("\\|", 3));

                break;

            case Protocol.GET_PRIVATE_HISTORY:

                handleGetPrivateHistory(parts);

                break;

            case Protocol.RECALL_MESSAGE:

                handleRecallMessage(parts);

                break;

            case Protocol.REPLY_MESSAGE:

                // Tách 3 phần: REPLY_MESSAGE | replyToId | content
                handleReplyMessage(message.split("\\|", 3));

                break;

            case Protocol.TYPING:

                handleTyping(parts);

                break;

            case Protocol.SEEN_MESSAGE:

                handleSeenMessage();

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


    // ==========================================================
    // REGISTER
    // REGISTER|username|displayName|encodedPassword
    // ==========================================================

    private void handleRegister(
            String[] parts) {

        // Đã login rồi thì không cho register.
        if (username != null) {

            send(
                    Protocol.REGISTER_FAILED,
                    "Bạn đã đăng nhập."
            );

            return;
        }


        if (parts.length < 4) {

            send(
                    Protocol.REGISTER_FAILED,
                    "Sai cú pháp đăng ký."
            );

            return;
        }


        String requestedUsername =
                parts[1].trim();


        String requestedDisplayName =
                parts[2].trim();


        String encodedPassword =
                parts[3];


        // =================================================
        // Validation username
        // =================================================

        if (
                !ValidationUtil.isValidUsername(
                        requestedUsername
                )
        ) {

            send(
                    Protocol.REGISTER_FAILED,
                    "Username phải gồm 3-20 ký tự "
                    + "A-Z, a-z, 0-9 hoặc _."
            );

            return;
        }


        // =================================================
        // Validation display name
        // =================================================

        if (
                !ValidationUtil.isValidDisplayName(
                        requestedDisplayName
                )
        ) {

            send(
                    Protocol.REGISTER_FAILED,
                    "Tên hiển thị không hợp lệ."
            );

            return;
        }


        // =================================================
        // Decode password
        // =================================================

        String password;

        try {

            byte[] passwordBytes =
                    Base64.getDecoder()
                            .decode(
                                    encodedPassword
                            );


            password =
                    new String(
                            passwordBytes,
                            StandardCharsets.UTF_8
                    );

        } catch (IllegalArgumentException e) {

            send(
                    Protocol.REGISTER_FAILED,
                    "Password không hợp lệ."
            );

            return;
        }


        // =================================================
        // Validation password
        // =================================================

        if (
                !ValidationUtil.isValidPassword(
                        password
                )
        ) {

            send(
                    Protocol.REGISTER_FAILED,
                    "Password phải từ 8 đến 128 ký tự."
            );

            return;
        }


        try {

            // =================================================
            // Kiểm tra username tồn tại
            // =================================================

            if (
                    userDAO.usernameExists(
                            requestedUsername
                    )
            ) {

                send(
                        Protocol.REGISTER_FAILED,
                        "Username đã tồn tại."
                );

                return;
            }


            // =================================================
            // Tạo User
            // =================================================

            boolean created =
                    userDAO.createUser(
                            requestedUsername,
                            requestedDisplayName,
                            password
                    );


            if (created) {

                send(
                        Protocol.REGISTER_SUCCESS,
                        "Đăng ký thành công! "
                        + "Bạn có thể đăng nhập."
                );

                System.out.println(
                        now() + " [REGISTER] "
                        + requestedUsername
                        + " / "
                        + requestedDisplayName
                        + " đăng ký thành công."
                );

            } else {

                send(
                        Protocol.REGISTER_FAILED,
                        "Không thể tạo tài khoản."
                );
            }


        } catch (Exception e) {

            System.out.println(
                    "[DATABASE] Lỗi REGISTER:"
            );

            e.printStackTrace();


            send(
                    Protocol.REGISTER_FAILED,
                    "Lỗi hệ thống Database."
            );
        }
    }


    // ==========================================================
    // LOGIN
    // LOGIN|username|encodedPassword
    // ==========================================================

    private void handleLogin(
            String[] parts) {

        if (username != null) {

            send(
                    Protocol.LOGIN_FAILED,
                    "Bạn đã đăng nhập."
            );

            return;
        }


        if (parts.length < 3) {

            send(
                    Protocol.LOGIN_FAILED,
                    "Sai cú pháp đăng nhập."
            );

            return;
        }


        String requestedUsername =
                parts[1].trim();


        String encodedPassword =
                parts[2];


        if (
                !ValidationUtil.isValidUsername(
                        requestedUsername
                )
        ) {

            send(
                    Protocol.LOGIN_FAILED,
                    "Username không hợp lệ."
            );

            return;
        }


        String password;


        try {

            byte[] bytes =
                    Base64.getDecoder()
                            .decode(
                                    encodedPassword
                            );


            password =
                    new String(
                            bytes,
                            StandardCharsets.UTF_8
                    );


        } catch (IllegalArgumentException e) {

            send(
                    Protocol.LOGIN_FAILED,
                    "Password không hợp lệ."
            );

            return;
        }


        if (
                !ValidationUtil.isValidPassword(
                        password
                )
        ) {

            send(
                    Protocol.LOGIN_FAILED,
                    "Password không hợp lệ."
            );

            return;
        }


        try {

            User user =
                    userDAO.findByUsername(
                            requestedUsername
                    );


            // =================================================
            // Không tìm thấy User
            // =================================================

            if (user == null) {

                send(
                        Protocol.LOGIN_FAILED,
                        "Username hoặc password không đúng."
                );

                return;
            }


            // =================================================
            // Kiểm tra password
            // =================================================

            boolean passwordCorrect =
                    PasswordUtil.verifyPassword(
                            password,
                            user.getPasswordHash(),
                            user.getPasswordSalt(),
                            user.getPasswordIterations()
                    );


            if (!passwordCorrect) {

                send(
                        Protocol.LOGIN_FAILED,
                        "Username hoặc password không đúng."
                );

                return;
            }


            // =================================================
            // GHI NHẬN VAI TRÒ TRONG PHÒNG MẶC ĐỊNH
            // =================================================

            // Nếu chưa có bản ghi thì tạo MEMBER.
            // Không ghi đè OWNER/ADMIN đã tồn tại.
            roomDAO.addMemberIfAbsent(
                    "Sảnh chính",
                    user.getId(),
                    RoomRole.MEMBER
            );

            // =================================================
            // Kiểm tra tài khoản đang online
            // =================================================

            if (
                    !clientManager.register(
                            user.getUsername(),
                            this
                    )
            ) {

                send(
                        Protocol.LOGIN_FAILED,
                        "Tài khoản này đang được đăng nhập."
                );

                return;
            }


            // =================================================
            // Tạo session
            // =================================================

            username =
                    user.getUsername();


            displayName =
                    user.getDisplayName();

            // ID lấy từ User đã xác thực trong Database.
            // Không nhận userId từ dữ liệu Client gửi lên.
            userId =
                    user.getId();


            currentRoom =
                    "Sảnh chính";


            roomManager.addClient(
                    currentRoom,
                    this
            );


            // =================================================
            // Login thành công
            // =================================================

            send(
                    Protocol.LOGIN_SUCCESS,
                    "Xin chào "
                    + displayName
                    + "!"
            );


            send(
                    Protocol.SYSTEM,
                    "Bạn đang ở phòng "
                    + currentRoom
                    + "."
            );


            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.SYSTEM,
                            displayName
                            + " đã tham gia phòng."
                    )
            );

            // Cập nhật danh sách thành viên cho mọi Client trong phòng.
            broadcastUserList(currentRoom);

            // Tải lịch sử phòng mặc định sau khi đăng nhập thành công.
            sendRoomHistory(currentRoom);


            System.out.println(
                    now() + " [LOGIN] "
                    + username
                    + " / "
                    + displayName
                    + " đăng nhập thành công → Phòng: "
                    + currentRoom
            );


        } catch (Exception e) {

            System.out.println(
                    "[DATABASE] Lỗi LOGIN:"
            );

            e.printStackTrace();


            send(
                    Protocol.LOGIN_FAILED,
                    "Lỗi hệ thống Database."
            );
        }
    }


    /**
     * Tải lịch sử của một phòng và gửi riêng cho Client hiện tại.
     *
     * Chỉ gọi sau khi Client đã đăng nhập và được thêm vào phòng.
     */
    private void sendRoomHistory(String roomName) {

        if (username == null) {
            return;
        }

        try {

            // Bước 1: Chuyển tên phòng thành ID trong Database.
            int roomId = roomDAO.findRoomIdByName(roomName);

            // Bước 2: Lấy tối đa 50 tin nhắn mới nhất của phòng.
            // MessageDAO chịu trách nhiệm về thứ tự kết quả.
            java.util.List<Message> history =
                    messageDAO.findLatestMessages(roomId, 50);

            // Bước 3: Gửi riêng từng tin lịch sử cho Client hiện tại.
            for (Message message : history) {

                send(
                        Protocol.HISTORY,
                        roomName,
                        String.valueOf(message.getId()),
                        String.valueOf(message.getSentAt()),
                        message.getSenderDisplayName(),
                        message.getContent()
                );

                // Gửi kèm thông tin trả lời / trích dẫn nếu là tin reply
                if (message.getReplyToId() > 0) {
                    String snippet = message.getReplySnippet();
                    String replySender = message.getReplySenderDisplayName();
                    if (replySender == null || replySender.isEmpty()) {
                        replySender = "Người dùng";
                    }
                    if (snippet != null && snippet.length() > 60) {
                        snippet = snippet.substring(0, 57) + "...";
                    }
                    send(
                            Protocol.REPLY_INFO,
                            String.valueOf(message.getId()),
                            String.valueOf(message.getReplyToId()),
                            replySender,
                            snippet == null ? "" : snippet
                    );
                }

                // Gửi kèm cảm xúc của tin nhắn lịch sử (nếu có)
                try {
                    String rxPayload = buildReactionPayload(message.getId());
                    if (!rxPayload.isEmpty()) {
                        send(
                                Protocol.REACTION_UPDATE,
                                String.valueOf(message.getId()),
                                rxPayload
                        );
                    }
                } catch (Exception ignored) {
                }
            }

            // Bước 4: Báo đã kết thúc tải lịch sử.
            send(
                    Protocol.HISTORY_END,
                    roomName
            );

            System.out.println(
                    now() + " [HISTORY] Đã gửi "
                    + history.size()
                    + " tin nhắn cho "
                    + username
                    + " trong phòng "
                    + roomName
            );

        } catch (Exception e) {

            System.err.println(
                    "[HISTORY] Không thể tải lịch sử phòng "
                    + roomName
            );

            e.printStackTrace();

            sendError(
                    "Không thể tải lịch sử phòng " + roomName
            );
        }
    }


    // ==========================================================
    // JOIN ROOM
    // ==========================================================

    private void handleJoinRoom(
            String[] parts) {

        if (!checkLogin()) {
            return;
        }


        if (parts.length < 2) {

            sendError(
                    "Sai cú pháp. Ví dụ: JOIN_ROOM|Học nhóm"
            );

            return;
        }


        // Chuẩn hóa NFC để khớp đúng tên phòng khi bàn phím gõ dạng NFD.
        String roomName = java.text.Normalizer.normalize(
                parts[1].trim(),
                java.text.Normalizer.Form.NFC
        );


        if (
                !roomManager.exists(
                        roomName
                )
        ) {

            sendError(
                    "Phòng '"
                    + roomName
                    + "' không tồn tại."
            );

            return;
        }


        if (
                roomName.equals(
                        currentRoom
                )
        ) {

            sendError(
                    "Bạn đang ở phòng này rồi."
            );

            return;
        }


        // =================================================
        // GHI NHẬN THÀNH VIÊN KHI VÀO PHÒNG
        // =================================================

        try {

            // Nếu chưa tồn tại, tạo bản ghi MEMBER.
            // Nếu đã có OWNER/ADMIN thì giữ nguyên vai trò đó.
            roomDAO.addMemberIfAbsent(
                    roomName,
                    userId,
                    RoomRole.MEMBER
            );

        } catch (SQLException e) {

            System.err.println(
                    "[DATABASE] Không thể ghi nhận thành viên trong phòng "
                    + roomName
            );

            e.printStackTrace();

            sendError(
                    "Không thể cập nhật dữ liệu thành viên của phòng."
            );

            return;
        }

        String oldRoom =
                currentRoom;


        roomManager.removeClient(
                oldRoom,
                this
        );


        currentRoom =
                roomName;


        roomManager.addClient(
                currentRoom,
                this
        );


        roomManager.broadcast(
                oldRoom,
                Protocol.build(
                        Protocol.SYSTEM,
                        displayName
                        + " đã rời phòng."
                )
        );

        // Hủy trạng thái typing ở phòng cũ nếu có
        roomManager.broadcastExcept(
                oldRoom,
                Protocol.build(Protocol.TYPING, oldRoom, username, displayName, "0"),
                this
        );


        roomManager.broadcast(
                currentRoom,
                Protocol.build(
                        Protocol.SYSTEM,
                        displayName
                        + " đã tham gia phòng."
                )
        );

        // Báo cho các thành viên trong phòng mới biết người này đã vào xem tin
        roomManager.broadcastExcept(
                currentRoom,
                Protocol.build(
                        Protocol.SEEN_MESSAGE,
                        currentRoom,
                        username,
                        displayName
                ),
                this
        );

        // Cập nhật danh sách thành viên của cả hai phòng.
        broadcastUserList(oldRoom);
        broadcastUserList(currentRoom);

        send(
                Protocol.SYSTEM,
                "Bạn đã vào phòng "
                + currentRoom
        );

        // Ghi log máy chủ: OUT ROOM (phòng cũ nếu có) và JOIN ROOM (phòng mới).
        if (oldRoom != null && !oldRoom.isEmpty()) {
            System.out.println(
                    now() + " [OUT ROOM] "
                    + username
                    + " / "
                    + displayName
                    + " ← rời phòng: '"
                    + oldRoom
                    + "'"
            );
        }

        System.out.println(
                now() + " [JOIN ROOM] "
                + username
                + " / "
                + displayName
                + " → vào phòng: '"
                + currentRoom
                + "'"
        );

        // Tải lịch sử của phòng vừa tham gia.
        sendRoomHistory(currentRoom);
    }


    // ==========================================================
    // PHASE 5.3 — CREATE ROOM
    // CREATE_ROOM|roomName
    // ==========================================================

    private void handleCreateRoom(String[] parts) {

        // Chỉ người đăng nhập thành công mới được tạo phòng.
        if (!checkLogin()) {
            return;
        }

        // Từ chối lệnh có cấu trúc không chính xác.
        if (parts.length != 2) {
            send(
                    Protocol.CREATE_ROOM_FAILED,
                    "Cú pháp tạo phòng không hợp lệ."
            );
            return;
        }

        // Chuẩn hóa NFC: xử lý bàn phím UniKey/IME gõ dạng NFD (combining marks).
        String roomName = java.text.Normalizer.normalize(
                parts[1].trim(),
                java.text.Normalizer.Form.NFC
        );

        /*
         * Chấp nhận chữ, số, dấu cách, gạch dưới và gạch ngang.
         * Hỗ trợ Unicode, bao gồm tiếng Việt có dấu.
         * \p{M} để chấp nhận combining diacritical marks (NFD từ bàn phím IME).
         * Không cho dấu phẩy vì ROOM_LIST đang dùng dấu phẩy phân cách phòng.
         * Không cho dấu | vì đây là dấu phân cách Protocol.
         */
        if (!roomName.matches("[\\p{L}\\p{M}\\p{N}_ -]{3,40}")) {
            send(
                    Protocol.CREATE_ROOM_FAILED,
                    "Tên phòng phải dài 3-40 ký tự, "
                    + "chỉ gồm chữ, số, dấu cách, _ hoặc -."
            );
            return;
        }

        try {
            // userId lấy từ session đã xác thực, không lấy từ Client.
            boolean created = roomDAO.createRoomWithOwner(
                    roomName,
                    userId
            );

            if (!created) {
                send(
                        Protocol.CREATE_ROOM_FAILED,
                        "Tên phòng đã tồn tại."
                );
                return;
            }

            // Cập nhật danh sách phòng trong bộ nhớ Server.
            if (!roomManager.exists(roomName)) {
                if (!roomManager.createRoom(roomName)) {
                    System.err.println(
                            "[ROOM] Không thể thêm phòng vào bộ nhớ: "
                            + roomName
                    );
                    send(
                            Protocol.CREATE_ROOM_FAILED,
                            "Database đã tạo phòng nhưng "
                            + "Server chưa cập nhật được phòng."
                    );
                    return;
                }
            }

            // Chuyển Owner từ phòng hiện tại sang phòng vừa tạo.
            String oldRoom = currentRoom;

            roomManager.removeClient(
                    oldRoom,
                    this
            );

            currentRoom = roomName;

            roomManager.addClient(
                    currentRoom,
                    this
            );

            // Báo kết quả cho Client tạo phòng.
            send(
                    Protocol.CREATE_ROOM_SUCCESS,
                    roomName,
                    "Đã tạo phòng. Bạn là OWNER."
            );

            // Cập nhật danh sách phòng cho các Client đang online.
            String roomListMessage = Protocol.build(
                    Protocol.ROOM_LIST,
                    String.join(", ", roomManager.getRoomNames())
            );
            clientManager.broadcast(roomListMessage);

            // Thông báo rời phòng cũ và tham gia phòng mới.
            roomManager.broadcast(
                    oldRoom,
                    Protocol.build(
                            Protocol.SYSTEM,
                            displayName + " đã rời phòng."
                    )
            );

            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.SYSTEM,
                            displayName + " đã tạo phòng và tham gia."
                    )
            );

            // Cập nhật danh sách thành viên của phòng cũ và phòng mới.
            broadcastUserList(oldRoom);
            broadcastUserList(currentRoom);

            send(
                    Protocol.SYSTEM,
                    "Bạn đang ở phòng " + currentRoom + "."
            );

            // Tải lịch sử phòng mới (nếu chưa có tin nhắn thì danh sách rỗng).
            sendRoomHistory(currentRoom);

            System.out.println(
                    now() + " [ROOM] User=" + username
                    + " tạo phòng '" + roomName
                    + "', Role=OWNER"
            );

        } catch (Exception e) {
            System.err.println(
                    "[ROOM] Không thể tạo phòng: " + roomName
            );
            e.printStackTrace();
            send(
                    Protocol.CREATE_ROOM_FAILED,
                    "Không thể tạo phòng do lỗi xử lý dữ liệu."
            );
        }
    }


    // ==========================================================
    // LẤY VAI TRÒ CỦA TÀI KHOẢN TRONG PHÒNG HIỆN TẠI
    // ==========================================================

    private void handleGetMyRole() {

        if (!checkLogin()) {
            return;
        }

        try {
            RoomRole role = roomDAO.findMemberRole(
                    currentRoom,
                    userId
            );

            // Nếu dữ liệu cũ chưa có bản ghi, chỉ gán MEMBER mặc định.
            if (role == null) {
                roomDAO.addMemberIfAbsent(
                        currentRoom,
                        userId,
                        RoomRole.MEMBER
                );

                role = roomDAO.findMemberRole(
                        currentRoom,
                        userId
                );
            }

            if (role == null) {
                role = RoomRole.MEMBER;
            }

            send(
                    Protocol.MY_ROLE,
                    currentRoom,
                    role.name()
            );

        } catch (Exception e) {
            System.err.println(
                    "[ROLE] Không thể đọc vai trò của " + username
            );
            e.printStackTrace();
            sendError("Không thể đọc vai trò trong phòng.");
        }
    }


    // ==========================================================
    // PHASE 5.3 — OWNER BỔ NHIỆM ADMIN
    // PROMOTE_ADMIN|targetUsername
    // ==========================================================

    private void handlePromoteAdmin(String[] parts) {

        if (!checkLogin()) {
            return;
        }

        if (parts.length != 2 || parts[1].trim().isEmpty()) {
            send(
                    Protocol.PROMOTE_ADMIN_FAILED,
                    "Cú pháp: PROMOTE_ADMIN|username"
            );
            return;
        }

        String targetUsername = parts[1].trim();

        try {
            // Luôn đọc vai trò của người gửi từ Database phía Server.
            RoomRole actorRole = roomDAO.findMemberRole(
                    currentRoom,
                    userId
            );

            if (actorRole == null || !actorRole.canPromoteAdmin()) {
                send(
                        Protocol.PROMOTE_ADMIN_FAILED,
                        "Chỉ OWNER của phòng mới được bổ nhiệm Admin."
                );
                return;
            }

            User target = userDAO.findByUsername(targetUsername);

            if (target == null) {
                send(
                        Protocol.PROMOTE_ADMIN_FAILED,
                        "Không tìm thấy tài khoản cần bổ nhiệm."
                );
                return;
            }

            if (target.getId() == userId) {
                send(
                        Protocol.PROMOTE_ADMIN_FAILED,
                        "Bạn không cần bổ nhiệm chính mình."
                );
                return;
            }

            if (!roomDAO.isMember(currentRoom, target.getId())) {
                send(
                        Protocol.PROMOTE_ADMIN_FAILED,
                        "Tài khoản này chưa phải thành viên của phòng."
                );
                return;
            }

            RoomRole targetRole = roomDAO.findMemberRole(
                    currentRoom,
                    target.getId()
            );

            // Không cho thay đổi hoặc hạ cấp Owner hiện tại.
            if (targetRole == RoomRole.OWNER) {
                send(
                        Protocol.PROMOTE_ADMIN_FAILED,
                        "Không thể thay đổi vai trò OWNER."
                );
                return;
            }

            roomDAO.setMemberRole(
                    currentRoom,
                    target.getId(),
                    RoomRole.ADMIN
            );

            send(
                    Protocol.PROMOTE_ADMIN_SUCCESS,
                    target.getUsername(),
                    currentRoom
            );

            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.SYSTEM,
                            target.getDisplayName()
                            + " đã được bổ nhiệm làm Admin."
                    )
            );

            System.out.println(
                    now() + " [ROLE] Owner=" + username
                    + " bổ nhiệm " + target.getUsername()
                    + " làm ADMIN trong '" + currentRoom + "'"
            );

        } catch (Exception e) {
            System.err.println("[ROLE] Không thể bổ nhiệm Admin.");
            e.printStackTrace();
            send(
                    Protocol.PROMOTE_ADMIN_FAILED,
                    "Không thể cập nhật vai trò do lỗi xử lý dữ liệu."
            );
        }
    }


    // ==========================================================
    // QUYỀN ADMIN / OWNER: KICK THÀNH VIÊN RA KHỎI PHÒNG
    // KICK_USER|targetUsername
    // ==========================================================

    private void handleKickUser(String[] parts) {

        if (!checkLogin()) {
            return;
        }

        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            send(
                    Protocol.KICK_USER_FAILED,
                    "Cú pháp: KICK_USER|username"
            );
            return;
        }

        String targetUsername = parts[1].trim();

        // Không cho phép kick ở phòng mặc định "Sảnh chính"
        if ("Sảnh chính".equalsIgnoreCase(currentRoom)) {
            send(
                    Protocol.KICK_USER_FAILED,
                    "Không thể kick thành viên ra khỏi 'Sảnh chính'."
            );
            return;
        }

        try {
            // 1. Luôn đọc vai trò của người thực hiện kick từ Database
            RoomRole actorRole = roomDAO.findMemberRole(
                    currentRoom,
                    userId
            );

            if (actorRole == null || (!actorRole.canKick(RoomRole.MEMBER) && !actorRole.canKick(RoomRole.ADMIN))) {
                send(
                        Protocol.KICK_USER_FAILED,
                        "Bạn không có quyền kick thành viên trong phòng này."
                );
                return;
            }

            // 2. Tra cứu tài khoản bị kick
            User target = userDAO.findByUsername(targetUsername);

            if (target == null) {
                send(
                        Protocol.KICK_USER_FAILED,
                        "Không tìm thấy tài khoản '" + targetUsername + "'."
                );
                return;
            }

            // 3. Không cho tự kick chính mình
            if (target.getId() == userId) {
                send(
                        Protocol.KICK_USER_FAILED,
                        "Bạn không thể tự kick chính mình ra khỏi phòng."
                );
                return;
            }

            // 4. Kiểm tra target có là thành viên phòng không
            boolean isMember = roomDAO.isMember(currentRoom, target.getId());
            ClientHandler targetHandler = clientManager.getClient(targetUsername);
            boolean isOnlineInRoom = (targetHandler != null && currentRoom.equalsIgnoreCase(targetHandler.getCurrentRoom()));

            if (!isMember && !isOnlineInRoom) {
                send(
                        Protocol.KICK_USER_FAILED,
                        "Tài khoản @" + targetUsername + " không có trong phòng này."
                );
                return;
            }

            // 5. Đọc vai trò của người bị kick
            RoomRole targetRole = roomDAO.findMemberRole(
                    currentRoom,
                    target.getId()
            );

            if (targetRole == null) {
                targetRole = RoomRole.MEMBER;
            }

            // 6. Kiểm tra quy tắc phân quyền kick (dùng RoomRole.canKick)
            if (!actorRole.canKick(targetRole)) {
                if (targetRole == RoomRole.OWNER) {
                    send(
                            Protocol.KICK_USER_FAILED,
                            "Không thể kick Chủ phòng (Owner)."
                    );
                } else if (actorRole == RoomRole.ADMIN && targetRole == RoomRole.ADMIN) {
                    send(
                            Protocol.KICK_USER_FAILED,
                            "Admin không thể kick Admin khác. Chỉ Chủ phòng mới có quyền này."
                    );
                } else {
                    send(
                            Protocol.KICK_USER_FAILED,
                            "Bạn không có quyền kick người dùng này."
                    );
                }
                return;
            }

            // 7. Xóa tư cách thành viên khỏi phòng trong Database SQLite
            roomDAO.removeMember(currentRoom, target.getId());

            String kickedRoomName = currentRoom;
            String actorTitle = (actorRole == RoomRole.OWNER) ? "Chủ phòng " : "Quản trị viên ";

            // 8. Nếu người bị kick đang online trong phòng này, đưa về "Sảnh chính"
            if (targetHandler != null && currentRoom.equalsIgnoreCase(targetHandler.getCurrentRoom())) {
                targetHandler.forceLeaveRoomToDefault(this.displayName, kickedRoomName);
            }

            // 9. Broadcast thông báo vào phòng chat
            roomManager.broadcast(
                    kickedRoomName,
                    Protocol.build(
                            Protocol.SYSTEM,
                            target.getDisplayName() + " (@" + target.getUsername() + ") đã bị "
                            + actorTitle + displayName + " kick ra khỏi phòng."
                    )
            );

            // 10. Cập nhật danh sách thành viên trong phòng
            broadcastUserList(kickedRoomName);

            // 11. Phản hồi cho người kick thành công
            send(
                    Protocol.KICK_USER_SUCCESS,
                    target.getUsername(),
                    kickedRoomName
            );

            System.out.println(
                    now() + " [KICK] " + actorRole + "=" + username
                    + " đã KICK " + target.getUsername() + " (" + targetRole + ") khỏi phòng '" + kickedRoomName + "'"
            );

        } catch (Exception e) {
            System.err.println(now() + " [KICK ERROR] Lỗi khi xử lý kick user: " + targetUsername);
            e.printStackTrace();
            send(
                    Protocol.KICK_USER_FAILED,
                    "Lỗi hệ thống khi thực hiện kick thành viên."
            );
        }
    }

    /**
     * Cưỡng chế đưa Client rời khỏi phòng hiện tại về "Sảnh chính" khi bị kick.
     *
     * @param kickerDisplayName tên hiển thị của người ra lệnh kick
     * @param kickedFromRoom tên phòng bị kick
     */
    public void forceLeaveRoomToDefault(String kickerDisplayName, String kickedFromRoom) {
        try {
            // Xóa client khỏi phòng cũ
            roomManager.removeClient(kickedFromRoom, this);

            // Ghi nhận vào Sảnh chính
            currentRoom = "Sảnh chính";
            roomManager.addClient(currentRoom, this);
            roomDAO.addMemberIfAbsent(currentRoom, userId, RoomRole.MEMBER);

            // Gửi gói tin KICKED riêng cho client
            send(
                    Protocol.KICKED,
                    kickedFromRoom,
                    kickerDisplayName
            );

            send(
                    Protocol.SYSTEM,
                    "Bạn đã bị " + kickerDisplayName + " kick ra khỏi phòng '" + kickedFromRoom + "'. Bạn đã được chuyển về 'Sảnh chính'."
            );

            // Cập nhật vai trò và tải lịch sử của Sảnh chính
            handleGetMyRole();
            sendRoomHistory(currentRoom);

            // Thông báo Sảnh chính có người vào
            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.SYSTEM,
                            displayName + " đã tham gia phòng."
                    )
            );

            // Đồng bộ danh sách user cho Sảnh chính
            broadcastUserList(currentRoom);

            System.out.println(
                    now() + " [KICKED REDIRECT] " + username
                    + " bị kick khỏi '" + kickedFromRoom + "' → Chuyển về 'Sảnh chính'"
            );

        } catch (Exception e) {
            System.err.println(now() + " [KICK ERROR] Lỗi khi chuyển phòng cho user bị kick: " + username);
            e.printStackTrace();
        }
    }

    // ==========================================================
    // XÓA PHÒNG (CHỈ OWNER CỦA PHÒNG MỚI ĐƯỢC XÓA)
    // DELETE_ROOM|roomName
    // ==========================================================

    private void handleDeleteRoom(String[] parts) {
        if (!checkLogin()) {
            return;
        }

        if (parts.length != 2) {
            send(
                    Protocol.DELETE_ROOM_FAILED,
                    "Cú pháp xóa phòng không hợp lệ: DELETE_ROOM|roomName"
            );
            return;
        }

        String targetRoom = java.text.Normalizer.normalize(
                parts[1].trim(),
                java.text.Normalizer.Form.NFC
        );

        if (targetRoom.isEmpty()) {
            send(
                    Protocol.DELETE_ROOM_FAILED,
                    "Tên phòng không được để trống."
            );
            return;
        }

        // Không cho phép xóa các phòng hệ thống mặc định
        if ("Sảnh chính".equalsIgnoreCase(targetRoom)
                || "Lập trình mạng".equalsIgnoreCase(targetRoom)
                || "Giải trí".equalsIgnoreCase(targetRoom)
                || "Học nhóm".equalsIgnoreCase(targetRoom)) {
            send(
                    Protocol.DELETE_ROOM_FAILED,
                    "Không thể xóa phòng mặc định của hệ thống (" + targetRoom + ")."
            );
            return;
        }

        try {
            // Kiểm tra phòng có tồn tại không
            if (!roomManager.exists(targetRoom)) {
                send(
                        Protocol.DELETE_ROOM_FAILED,
                        "Phòng '" + targetRoom + "' không tồn tại."
                );
                return;
            }

            // Kiểm tra vai trò của người yêu cầu xóa trong phòng đó
            RoomRole myRoleInRoom = roomDAO.findMemberRole(targetRoom, this.userId);
            if (myRoleInRoom != RoomRole.OWNER) {
                send(
                        Protocol.DELETE_ROOM_FAILED,
                        "Bạn không phải là Người tạo phòng (Owner) của phòng '" + targetRoom + "'. Chỉ người tạo phòng mới được xóa phòng này!"
                );
                return;
            }

            // Lấy danh sách client đang có mặt trong phòng bị xóa
            java.util.Set<ClientHandler> clientsInRoom = roomManager.getClients(targetRoom);

            // Chuyển tất cả client đang ở phòng đó (ngoại trừ người xóa) về Sảnh chính
            for (ClientHandler ch : clientsInRoom) {
                if (ch != this) {
                    try {
                        ch.roomManager.removeClient(targetRoom, ch);
                        ch.currentRoom = "Sảnh chính";
                        ch.roomManager.addClient("Sảnh chính", ch);
                        ch.roomDAO.addMemberIfAbsent("Sảnh chính", ch.userId, RoomRole.MEMBER);

                        ch.send(Protocol.ROOM_DELETED, targetRoom, this.displayName);
                        ch.send(Protocol.SYSTEM, "Phòng '" + targetRoom + "' đã bị chủ phòng (" + this.displayName + ") xóa. Bạn đã được chuyển về 'Sảnh chính'.");
                        ch.handleGetMyRole();
                        ch.sendRoomHistory("Sảnh chính");
                        ch.broadcastUserList("Sảnh chính");
                    } catch (Exception ex) {
                        System.err.println("[ROOM DELETE] Lỗi khi chuyển client " + ch.getUsername() + " về Sảnh chính: " + ex.getMessage());
                    }
                }
            }

            // Nếu chính người xóa phòng đang đứng trong phòng đó, chuyển về Sảnh chính
            if (targetRoom.equalsIgnoreCase(this.currentRoom)) {
                roomManager.removeClient(targetRoom, this);
                this.currentRoom = "Sảnh chính";
                roomManager.addClient("Sảnh chính", this);
                roomDAO.addMemberIfAbsent("Sảnh chính", this.userId, RoomRole.MEMBER);

                handleGetMyRole();
                sendRoomHistory("Sảnh chính");
                broadcastUserList("Sảnh chính");
            }

            // Xóa phòng khỏi bộ nhớ RoomManager
            roomManager.removeRoom(targetRoom);

            // Xóa phòng khỏi CSDL SQLite
            roomDAO.deleteRoom(targetRoom);

            // Gửi thông báo thành công cho Owner
            send(
                    Protocol.DELETE_ROOM_SUCCESS,
                    targetRoom
            );

            send(
                    Protocol.SYSTEM,
                    "Bạn đã xóa thành công phòng '" + targetRoom + "'."
            );

            // Broadcast danh sách phòng cập nhật cho toàn bộ Client online
            String roomListMessage = Protocol.build(
                    Protocol.ROOM_LIST,
                    String.join(", ", roomManager.getRoomNames())
            );
            clientManager.broadcast(roomListMessage);

            System.out.println(
                    now() + " [ROOM DELETED] User=" + username + " (OWNER) đã xóa phòng '" + targetRoom + "'"
            );

        } catch (Exception e) {
            System.err.println(now() + " [DELETE ROOM ERROR] Lỗi khi xóa phòng: " + targetRoom);
            e.printStackTrace();
            send(
                    Protocol.DELETE_ROOM_FAILED,
                    "Lỗi hệ thống khi xóa phòng."
            );
        }
    }


    // ==========================================================
    // GET ROOMS
    // ==========================================================

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


    // ==========================================================
    // GET USERS
    // ==========================================================

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
     * Xử lý yêu cầu tìm kiếm lịch sử tin nhắn.
     *
     * Client gửi:
     * SEARCH_MESSAGES|từ khóa
     *
     * Chỉ tìm kiếm trong phòng hiện tại của người dùng đã đăng nhập.
     */
    private void handleSearchMessages(String[] parts) {

        // 1. Chỉ tài khoản đã đăng nhập mới được tìm kiếm.
        if (!checkLogin()) {
            return;
        }

        // 2. Kiểm tra cấu trúc yêu cầu.
        if (parts.length < 2) {
            sendError("Cú pháp: /search từ khóa");
            return;
        }

        // 3. Lấy và kiểm tra từ khóa.
        String keyword = parts[1].trim();

        if (keyword.isEmpty()) {
            sendError("Từ khóa tìm kiếm không được để trống.");
            return;
        }

        if (keyword.length() > 100) {
            sendError("Từ khóa tìm kiếm tối đa 100 ký tự.");
            return;
        }

        try {

            // 4. Xác định ID phòng hiện tại trong Database.
            int roomId = roomDAO.findRoomIdByName(currentRoom);

            // 5. Tìm tối đa 50 kết quả trong phòng hiện tại.
            java.util.List<Message> results =
                    messageDAO.searchMessages(
                            roomId,
                            keyword,
                            50
                    );

            // 6. Gửi từng kết quả riêng cho Client hiện tại.
            for (Message message : results) {

                send(
                        Protocol.SEARCH_RESULT,
                        currentRoom,
                        String.valueOf(message.getId()),
                        String.valueOf(message.getSentAt()),
                        message.getSenderDisplayName(),
                        message.getContent()
                );
            }

            // 7. Báo hoàn thành, kể cả khi không tìm thấy kết quả.
            send(
                    Protocol.SEARCH_END,
                    currentRoom,
                    String.valueOf(results.size())
            );

            System.out.println(
                    now() + " [SEARCH] User=" + username
                    + ", Room=" + currentRoom
                    + ", Keyword=" + keyword
                    + ", Results=" + results.size()
            );

        } catch (Exception e) {

            System.err.println(
                    "[SEARCH ERROR] Không thể tìm kiếm tin nhắn."
            );

            e.printStackTrace();

            sendError(
                    "Không thể tìm kiếm tin nhắn. Vui lòng thử lại."
            );
        }
    }


// ==========================================================
    // SEND MESSAGE
    // ==========================================================

    private void handleSendMessage(String[] parts) {

        if (!checkLogin()) {
            return;
        }

        if (parts.length < 2) {
            sendError("Tin nhắn không được rỗng.");
            return;
        }

        // Nội dung đã được tách từ dấu | đầu tiên nên có thể chứa thêm dấu |.
        String content = parts[1].trim();

        if (content.isEmpty()) {
            sendError("Tin nhắn không được rỗng.");
            return;
        }

        if (content.length() > 500) {
            sendError("Tin nhắn tối đa 500 ký tự.");
            return;
        }

        try {

            // Bước 1: Tra cứu ID phòng trong Database.
            int roomId = roomDAO.findRoomIdByName(currentRoom);
            // Log dữ liệu đầu vào để kiểm tra trước khi lưu.
            System.out.println(
                    now() + " [MESSAGE] Room=" + currentRoom
                    + ", user=" + username
                    + ", content=" + content
            );


            // Bước 2: Xây dựng tin nhắn bằng danh tính từ Session đã xác thực.
            Message message = new Message(
                    roomId,
                    userId,
                    username,
                    displayName,
                    content
            );

            // Bước 3: Lưu tin nhắn xuống Database trước khi broadcast.
            Message savedMessage = messageDAO.saveMessage(message);
            // Xác nhận tin nhắn đã được lưu thành công.
            System.out.println(
                    now() + " [MESSAGE SAVED] id=" + savedMessage.getId()
                    + ", roomId=" + savedMessage.getRoomId()
                    + ", content=" + savedMessage.getContent()
            );


            // Tắt trạng thái typing của người này sau khi gửi tin
            roomManager.broadcastExcept(
                    currentRoom,
                    Protocol.build(Protocol.TYPING, currentRoom, username, displayName, "0"),
                    this
            );

            // Bước 4: Phát tin nhắn tới tất cả client đang online để hiển thị chat & thông báo tin chưa đọc
            // Format: CHAT|room|messageId|username|displayName|content
            clientManager.broadcast(
                    Protocol.build(
                            Protocol.CHAT,
                            currentRoom,
                            String.valueOf(savedMessage.getId()),
                            username,
                            displayName,
                            savedMessage.getContent()
                    )
            );

        } catch (Exception e) {

            System.err.println(
                    "[MESSAGE] Không thể lưu tin nhắn."
            );

            e.printStackTrace();

            sendError(
                    "Không thể lưu tin nhắn. Vui lòng thử lại."
            );
        }
    }


    // ==========================================================
    // REACT — CẢM XÚC TIN NHẮN
    // REACT|messageId|emoji
    // ==========================================================

    private void handleReact(String[] parts) {

        if (!checkLogin()) {
            return;
        }

        if (parts.length < 3) {
            sendError("Cú pháp: REACT|messageId|emoji");
            return;
        }

        int messageId;

        try {
            messageId = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            sendError("messageId không hợp lệ.");
            return;
        }

        String emoji = parts[2].trim();

        // Kiểm tra emoji hợp lệ.
        boolean validEmoji = false;
        for (String allowed : database.ReactionDAO.ALLOWED_EMOJIS) {
            if (allowed.equals(emoji)) {
                validEmoji = true;
                break;
            }
        }

        if (!validEmoji) {
            sendError("Emoji không hợp lệ. Chỉ chấp nhận ❤️ 😂 👍");
            return;
        }

        try {
            // Lưu / bỏ reaction vào DB (userId từ session, không từ client).
            reactionDAO.toggleReaction(messageId, userId, emoji);

            // Đọc lại tất cả reaction của tin nhắn để broadcast.
            String payload = buildReactionPayload(messageId);

            // Broadcast cập nhật cho toàn phòng.
            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.REACTION_UPDATE,
                            String.valueOf(messageId),
                            payload
                    )
            );

            System.out.println(
                    now() + " [REACT] " + username
                    + " → msg=" + messageId
                    + ", emoji=" + emoji
            );

        } catch (Exception e) {
            System.err.println("[REACT] Lỗi khi xử lý reaction.");
            e.printStackTrace();
            sendError("Không thể lưu cảm xúc.");
        }
    }


    /**
     * Xây chuỗi payload từ map reaction.
     * Format: ❤️:3,😂:1,👍:5
     * Nếu không có reaction nào → chuỗi rỗng.
     */
    private String buildReactionPayload(int messageId) throws java.sql.SQLException {

        java.util.Map<String, Integer> counts =
                reactionDAO.countByEmoji(messageId);

        if (counts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey())
              .append(":")
              .append(entry.getValue());
        }

        return sb.toString();
    }


    // ==========================================================
    // LOGOUT
    // ==========================================================

    private void handleLogout() {

        send(
                Protocol.SYSTEM,
                "Đang đăng xuất..."
        );


        running =
                false;
    }


    // ==========================================================
    // CHECK LOGIN
    // ==========================================================

    private boolean checkLogin() {

        if (username == null) {

            sendError(
                    "Bạn chưa đăng nhập."
            );

            return false;
        }


        return true;
    }


    // ==========================================================
    // ERROR
    // ==========================================================

    private void sendError(
            String message) {

        send(
                Protocol.ERROR,
                message
        );
    }


    // ==========================================================
    // SEND
    // ==========================================================

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
     * Gửi nguyên chuỗi Protocol.
     */
    public synchronized void sendRaw(
            String message) {

        if (writer == null) {
            return;
        }


        writer.println(
                message
        );
    }


    // ==========================================================
    // CẬP NHẬT DANH SÁCH THÀNH VIÊN CHO CẢ PHÒNG
    // ==========================================================

    /**
     * Phát danh sách thành viên mới nhất đến tất cả Client
     * đang ở phòng được chỉ định.
     */
    private void broadcastUserList(String roomName) {

        if (roomName == null || roomName.isBlank()) {
            return;
        }

        String users = String.join(
                ", ",
                roomManager.getUserNames(roomName)
        );

        roomManager.broadcast(
                roomName,
                Protocol.build(
                        Protocol.USER_LIST,
                        roomName,
                        users
                )
        );
    }


    // ==========================================================
    // DISCONNECT
    // ==========================================================

    private void disconnect() {

        if (username != null) {

            String oldUsername =
                    username;


            String oldDisplayName =
                    displayName;


            String oldRoom =
                    currentRoom;


            roomManager.removeClient(
                    oldRoom,
                    this
            );


            clientManager.remove(
                    oldUsername,
                    this
            );


            // Thông báo cho thành viên còn lại.
            roomManager.broadcast(
                    oldRoom,
                    Protocol.build(
                            Protocol.SYSTEM,
                            oldDisplayName
                            + " đã rời phòng."
                    )
            );

            // Cập nhật danh sách thành viên sau khi Client rời phòng.
            broadcastUserList(oldRoom);

            // Log rõ ràng: OUT ROOM + OFFLINE
            System.out.println(
                    now() + " [OUT ROOM] "
                    + oldUsername
                    + " / "
                    + oldDisplayName
                    + " ← rời phòng: '"
                    + oldRoom
                    + "'"
            );

            System.out.println(
                    now() + " [OFFLINE]  "
                    + oldUsername
                    + " / "
                    + oldDisplayName
                    + " đã ngắt kết nối."
            );


            username =
                    null;

            // Xóa ID tài khoản khỏi Session khi ngắt kết nối.
            userId = 0;
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


    // ==========================================================
    // TIN NHẮN RIÊNG TƯ (PRIVATE CHAT 1-1)
    // ==========================================================

    /**
     * Xử lý gửi tin nhắn riêng 1-1.
     * Format client gửi: SEND_PRIVATE|targetUsername|content
     */
    private void handleSendPrivate(String[] parts) {
        if (!checkLogin()) {
            return;
        }

        if (parts.length < 3) {
            sendError("Dữ liệu gửi tin nhắn riêng không hợp lệ.");
            return;
        }

        String targetUsername = parts[1].trim();
        String content = parts[2].trim();

        if (targetUsername.isEmpty()) {
            sendError("Tên người nhận không được để trống.");
            return;
        }

        if (targetUsername.equalsIgnoreCase(this.username)) {
            sendError("Không thể tự gửi tin nhắn cho chính mình.");
            return;
        }

        if (content.isEmpty()) {
            sendError("Tin nhắn riêng không được rỗng.");
            return;
        }

        if (content.length() > 500) {
            sendError("Tin nhắn riêng tối đa 500 ký tự.");
            return;
        }

        try {
            // Bước 1: Tra cứu người nhận trong cơ sở dữ liệu
            User targetUser = userDAO.findByUsername(targetUsername);
            if (targetUser == null) {
                sendError("Người dùng '" + targetUsername + "' không tồn tại.");
                return;
            }

            // Bước 2: Lưu vào cơ sở dữ liệu SQLite (bảng private_messages)
            PrivateMessage pm = new PrivateMessage(
                    this.userId,
                    this.username,
                    this.displayName,
                    targetUser.getId(),
                    targetUser.getUsername(),
                    targetUser.getDisplayName(),
                    content
            );

            PrivateMessage saved = privateMessageDAO.save(pm);

            // Bước 3: In log Server
            System.out.println(
                    now() + " [PRIVATE CHAT] " + this.username + " (" + this.displayName + ") -> "
                    + targetUser.getUsername() + " (" + targetUser.getDisplayName() + "): " + content
            );

            // Bước 4: Đóng gói thông điệp giao thức
            String protoMsg = Protocol.build(
                    Protocol.PRIVATE_CHAT,
                    this.username,
                    this.displayName,
                    targetUser.getUsername(),
                    targetUser.getDisplayName(),
                    saved.getSentAt(),
                    saved.getContent()
            );

            // Bước 5: Chuyển tiếp tới người nhận nếu đang online
            ClientHandler recipientHandler = clientManager.getClient(targetUser.getUsername());
            if (recipientHandler != null) {
                recipientHandler.sendRaw(protoMsg);
            }

            // Bước 6: Phản hồi xác nhận cho chính người gửi (để hiển thị bubble phía mình)
            sendRaw(protoMsg);

        } catch (Exception e) {
            System.err.println(now() + " [PRIVATE CHAT ERROR] Không thể lưu hoặc gửi tin nhắn riêng.");
            e.printStackTrace();
            sendError("Không thể gửi tin nhắn riêng do lỗi hệ thống.");
        }
    }

    /**
     * Xử lý yêu cầu tải lịch sử tin nhắn riêng 1-1.
     * Format client gửi: GET_PRIVATE_HISTORY|targetUsername
     */
    private void handleGetPrivateHistory(String[] parts) {
        if (!checkLogin()) {
            return;
        }

        if (parts.length < 2) {
            return;
        }

        String targetUsername = parts[1].trim();
        if (targetUsername.isEmpty() || targetUsername.equalsIgnoreCase(this.username)) {
            return;
        }

        try {
            java.util.List<PrivateMessage> history =
                    privateMessageDAO.getHistory(this.username, targetUsername, 50);

            for (PrivateMessage pm : history) {
                sendRaw(Protocol.build(
                        Protocol.PRIVATE_HISTORY,
                        pm.getSenderUsername(),
                        pm.getSenderDisplayName(),
                        pm.getReceiverUsername(),
                        pm.getReceiverDisplayName(),
                        pm.getSentAt(),
                        pm.getContent()
                ));
            }

            send(
                    Protocol.PRIVATE_HISTORY_END,
                    targetUsername,
                    String.valueOf(history.size())
            );

        } catch (Exception e) {
            System.err.println(now() + " [PRIVATE HISTORY ERROR] Không thể tải lịch sử riêng.");
            e.printStackTrace();
        }
    }

    // ==========================================================
    // THU HỒI TIN NHẮN (RECALL MESSAGE)
    // ==========================================================

    /**
     * Xử lý yêu cầu thu hồi tin nhắn trong phòng.
     * Format client gửi: RECALL_MESSAGE|messageId
     */
    private void handleRecallMessage(String[] parts) {
        if (!checkLogin()) {
            return;
        }

        if (parts.length < 2) {
            sendError("Sai cú pháp thu hồi tin nhắn.");
            return;
        }

        int msgId;
        try {
            msgId = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            sendError("ID tin nhắn không hợp lệ.");
            return;
        }

        try {
            // Bước 1: Tra cứu tin nhắn trong SQLite
            Message msg = messageDAO.findById(msgId);
            if (msg == null) {
                sendError("Tin nhắn không tồn tại hoặc đã bị xóa.");
                return;
            }

            // Bước 2: Kiểm tra chính chủ (chỉ người gửi mới được thu hồi)
            if (msg.getSenderId() != this.userId) {
                sendError("Bạn chỉ có thể thu hồi tin nhắn do chính mình gửi.");
                return;
            }

            // Bước 3: Kiểm tra đã thu hồi chưa
            if (MessageDAO.RECALLED_CONTENT.equals(msg.getContent())) {
                sendError("Tin nhắn này đã được thu hồi trước đó.");
                return;
            }

            // Bước 4: Kiểm tra thời gian (giới hạn trong vòng 5 phút = 300 giây)
            int ageSeconds = messageDAO.getMessageAgeSeconds(msgId);
            if (ageSeconds > 300) {
                sendError("Chỉ có thể thu hồi tin nhắn trong vòng 5 phút kể từ khi gửi.");
                return;
            }

            // Bước 5: Cập nhật SQLite Database
            boolean success = messageDAO.recallMessage(msgId, this.userId);
            if (success) {
                System.out.println(
                        now() + " [RECALL MESSAGE] User=" + username
                        + " đã thu hồi tin nhắn ID=" + msgId
                        + " trong phòng '" + currentRoom + "'"
                );

                // Bước 6: Broadcast tới tất cả thành viên trong phòng
                roomManager.broadcast(
                        currentRoom,
                        Protocol.build(
                                Protocol.MESSAGE_RECALLED,
                                currentRoom,
                                String.valueOf(msgId)
                        )
                );
            } else {
                sendError("Không thể thu hồi tin nhắn. Vui lòng thử lại.");
            }

        } catch (Exception e) {
            System.err.println(now() + " [RECALL ERROR] Không thể thu hồi tin nhắn ID=" + msgId);
            e.printStackTrace();
            sendError("Lỗi hệ thống khi thu hồi tin nhắn.");
        }
    }

    // ==========================================================
    // TRẢ LỜI TIN NHẮN (REPLY MESSAGE)
    // Format: REPLY_MESSAGE|replyToId|content
    // ==========================================================

    private void handleReplyMessage(String[] parts) {
        if (!checkLogin()) {
            return;
        }

        if (parts.length < 3) {
            sendError("Cú pháp trả lời tin nhắn không hợp lệ.");
            return;
        }

        int replyToId;
        try {
            replyToId = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            sendError("ID tin nhắn được trả lời không hợp lệ.");
            return;
        }

        String content = parts[2].trim();
        if (content.isEmpty()) {
            sendError("Tin nhắn không được rỗng.");
            return;
        }

        if (content.length() > 500) {
            sendError("Tin nhắn tối đa 500 ký tự.");
            return;
        }

        try {
            // Bước 1: Tra cứu tin nhắn gốc để trích dẫn
            Message orig = messageDAO.findById(replyToId);
            if (orig == null) {
                sendError("Tin nhắn được trả lời không tồn tại.");
                return;
            }

            int roomId = roomDAO.findRoomIdByName(currentRoom);

            // Bước 2: Tạo đối tượng tin nhắn mới có gắn replyToId
            Message message = new Message(
                    roomId,
                    userId,
                    username,
                    displayName,
                    content
            );
            message.setReplyToId(replyToId);

            // Bước 3: Lưu vào SQLite Database vĩnh viễn
            Message saved = messageDAO.saveMessage(message);

            System.out.println(
                    now() + " [REPLY MESSAGE] Room=" + currentRoom
                    + ", user=" + username
                    + " replyTo=" + replyToId
                    + " (" + orig.getSenderDisplayName() + "): " + content
            );

            // Bước 4: Broadcast tin nhắn chat tới toàn bộ client online để cập nhật chat & đếm tin chưa đọc
            clientManager.broadcast(
                    Protocol.build(
                            Protocol.CHAT,
                            currentRoom,
                            String.valueOf(saved.getId()),
                            username,
                            displayName,
                            saved.getContent()
                    )
            );

            // Bước 5: Broadcast thông tin trích dẫn để các client gắn vào bubble
            String snippet = orig.getContent();
            if (snippet != null && snippet.length() > 60) {
                snippet = snippet.substring(0, 57) + "...";
            }
            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.REPLY_INFO,
                            String.valueOf(saved.getId()),
                            String.valueOf(replyToId),
                            orig.getSenderDisplayName(),
                            snippet == null ? "" : snippet
                    )
            );

            // Tắt trạng thái typing của người này sau khi gửi tin trả lời
            roomManager.broadcastExcept(
                    currentRoom,
                    Protocol.build(Protocol.TYPING, currentRoom, username, displayName, "0"),
                    this
            );

        } catch (Exception e) {
            System.err.println(now() + " [REPLY ERROR] Không thể lưu tin nhắn trả lời.");
            e.printStackTrace();
            sendError("Không thể gửi tin nhắn trả lời. Vui lòng thử lại.");
        }
    }

    // ==========================================================
    // XỬ LÝ TRẠNG THÁI SOẠN TIN NHẮN (TYPING)
    // TYPING|isTyping (1 hoặc 0)
    // ==========================================================

    private void handleTyping(String[] parts) {
        if (!checkLogin()) {
            return;
        }

        if (parts.length < 2) {
            return;
        }

        String status = parts[1].trim();
        boolean isTyping = "1".equals(status);

        // Broadcast trạng thái gõ phím tới tất cả thành viên trong phòng trừ chính người gõ
        // Format: TYPING|roomName|username|displayName|isTyping
        roomManager.broadcastExcept(
                currentRoom,
                Protocol.build(
                        Protocol.TYPING,
                        currentRoom,
                        username,
                        displayName,
                        isTyping ? "1" : "0"
                ),
                this
        );
    }
    // ==========================================================
    // XỬ LÝ BÁO ĐÃ XEM TIN NHẮN (SEEN_MESSAGE)
    // ==========================================================

    private void handleSeenMessage() {
        if (!checkLogin() || currentRoom == null) {
            return;
        }

        // Broadcast tới các thành viên khác trong phòng biết mình đã xem tin
        // Format: SEEN_MESSAGE|roomName|username|displayName
        roomManager.broadcastExcept(
                currentRoom,
                Protocol.build(
                        Protocol.SEEN_MESSAGE,
                        currentRoom,
                        username,
                        displayName
                ),
                this
        );
    }

    public String getUsername() {

        return username;
    }


    public String getDisplayName() {

        return displayName;
    }


    public String getCurrentRoom() {

        return currentRoom;
    }
}