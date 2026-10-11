/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
import database.MessageDAO;
import database.RoomDAO;
import model.Message;
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


    public ClientHandler(
            Socket socket,
            ClientManager clientManager,
            RoomManager roomManager,
            UserDAO userDAO,
            RoomDAO roomDAO,
            MessageDAO messageDAO) {

        this.socket = socket;
        this.clientManager = clientManager;
        this.roomManager = roomManager;
        this.userDAO = userDAO;

        this.roomDAO = roomDAO;
        this.messageDAO = messageDAO;
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
                    "Vui lòng đăng ký hoặc đăng nhập."
            );


            String message;


            while (
                    running
                    && (message =
                            reader.readLine()) != null
            ) {

                System.out.println(
                        "[RECEIVE] "
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
                    "[SERVER] Client mất kết nối: "
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

            case Protocol.SEARCH_MESSAGES:

                // Chỉ tách tại dấu | đầu tiên để giữ nguyên từ khóa.
                handleSearchMessages(message.split("\\|", 2));

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
                        "[REGISTER] "
                        + requestedUsername
                        + " - "
                        + requestedDisplayName
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
                    "[LOGIN] "
                    + username
                    + " / "
                    + displayName
                    + " login thành công."
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
            }

            // Bước 4: Báo đã kết thúc tải lịch sử.
            send(
                    Protocol.HISTORY_END,
                    roomName
            );

            System.out.println(
                    "[HISTORY] Đã gửi "
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


        roomManager.broadcast(
                currentRoom,
                Protocol.build(
                        Protocol.SYSTEM,
                        displayName
                        + " đã tham gia phòng."
                )
        );

        // Cập nhật danh sách thành viên của cả hai phòng.
        broadcastUserList(oldRoom);
        broadcastUserList(currentRoom);

        send(
                Protocol.SYSTEM,
                "Bạn đã vào phòng "
                + currentRoom
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
                    "[ROOM] User=" + username
                    + ", Room=" + roomName
                    + ", Role=OWNER"
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
                    "[ROLE] Owner=" + username
                    + " promoted " + target.getUsername()
                    + " to ADMIN in " + currentRoom
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
                    "[SEARCH] User=" + username
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
                    "[DEBUG MESSAGE] Room=" + currentRoom
                    + ", roomId=" + roomId
                    + ", userId=" + userId
                    + ", username=" + username
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
                    "[MESSAGE SAVED] id=" + savedMessage.getId()
                    + ", roomId=" + savedMessage.getRoomId()
                    + ", content=" + savedMessage.getContent()
            );


            // Bước 4: Chỉ phát tin nhắn sau khi lưu thành công.
            roomManager.broadcast(
                    currentRoom,
                    Protocol.build(
                            Protocol.CHAT,
                            currentRoom,
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

            System.out.println(
                    "[DISCONNECT] "
                    + oldUsername
                    + " / "
                    + oldDisplayName
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