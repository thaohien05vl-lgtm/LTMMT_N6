/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package common;

/**
 * Protocol:
 *
 * Quy ước giao tiếp giữa Client và Server.
 *
 * Format:
 *
 * COMMAND|DATA1|DATA2|...
 */
public final class Protocol {

    // =========================================================
    // CLIENT → SERVER
    // =========================================================

    public static final String REGISTER =
            "REGISTER";

    public static final String LOGIN =
            "LOGIN";

    public static final String JOIN_ROOM =
            "JOIN_ROOM";
    
    // =========================================================
// PHASE 5.3 — QUẢN LÝ PHÒNG VÀ VAI TRÒ
// =========================================================

// Client yêu cầu tạo phòng mới.
public static final String CREATE_ROOM = "CREATE_ROOM";

// Client yêu cầu xem vai trò trong phòng hiện tại.
public static final String GET_MY_ROLE = "GET_MY_ROLE";

// Owner yêu cầu bổ nhiệm một thành viên thành Admin.
public static final String PROMOTE_ADMIN = "PROMOTE_ADMIN";

// Admin hoặc Owner yêu cầu kick một thành viên ra khỏi phòng.
// Format: KICK_USER|targetUsername
public static final String KICK_USER = "KICK_USER";

// Owner yêu cầu xóa phòng.
// Format: DELETE_ROOM|roomName
public static final String DELETE_ROOM = "DELETE_ROOM";

    
    public static final String GET_ROOMS =
            "GET_ROOMS";

    public static final String GET_USERS =
            "GET_USERS";

    public static final String SEND_MESSAGE =
            "SEND_MESSAGE";

    public static final String LOGOUT =
            "LOGOUT";

    // Server gửi từng tin nhắn lịch sử cho Client.
    public static final String HISTORY = "HISTORY";

    // Báo hiệu đã tải xong lịch sử của phòng.
    public static final String HISTORY_END = "HISTORY_END";
    
    // Yêu cầu tìm kiếm lịch sử tin nhắn.
    public static final String SEARCH_MESSAGES = "SEARCH_MESSAGES";
    
    // =========================================================
    // SERVER → CLIENT
    // =========================================================

    public static final String REGISTER_SUCCESS =
            "REGISTER_SUCCESS";

    public static final String REGISTER_FAILED =
            "REGISTER_FAILED";

    public static final String LOGIN_SUCCESS =
            "LOGIN_SUCCESS";

    public static final String LOGIN_FAILED =
            "LOGIN_FAILED";

    public static final String ROOM_LIST =
            "ROOM_LIST";

    public static final String USER_LIST =
            "USER_LIST";

    public static final String CHAT =
            "CHAT";

    public static final String SYSTEM =
            "SYSTEM";

    public static final String ERROR =
            "ERROR";

    
    // Phản hồi khi tạo phòng.
public static final String CREATE_ROOM_SUCCESS = "CREATE_ROOM_SUCCESS";
public static final String CREATE_ROOM_FAILED = "CREATE_ROOM_FAILED";

// Server trả về vai trò của tài khoản trong phòng hiện tại.
public static final String MY_ROLE = "MY_ROLE";

// Phản hồi khi bổ nhiệm Admin.
public static final String PROMOTE_ADMIN_SUCCESS = "PROMOTE_ADMIN_SUCCESS";
public static final String PROMOTE_ADMIN_FAILED = "PROMOTE_ADMIN_FAILED";

// Phản hồi khi Kick thành viên.
// Server → Client (Admin/Owner): báo kick thành công (KICK_USER_SUCCESS|targetUsername|roomName)
public static final String KICK_USER_SUCCESS = "KICK_USER_SUCCESS";
// Server → Client (Admin/Owner): báo kick thất bại (KICK_USER_FAILED|reason)
public static final String KICK_USER_FAILED = "KICK_USER_FAILED";
// Server → Client bị kick: thông báo bị kick và bị chuyển về Sảnh chính (KICKED|roomName|kickerDisplayName)
public static final String KICKED = "KICKED";

// Phản hồi khi xóa phòng:
// Server → Client xóa phòng: DELETE_ROOM_SUCCESS|roomName
public static final String DELETE_ROOM_SUCCESS = "DELETE_ROOM_SUCCESS";
// Server → Client xóa phòng thất bại: DELETE_ROOM_FAILED|reason
public static final String DELETE_ROOM_FAILED = "DELETE_ROOM_FAILED";
// Server → Các thành viên trong phòng bị xóa: ROOM_DELETED|roomName|deletedByDisplayName
public static final String ROOM_DELETED = "ROOM_DELETED";

    
    // Một kết quả tìm kiếm.
    public static final String SEARCH_RESULT = "SEARCH_RESULT";

    // Thông báo đã hoàn thành truy vấn.
    public static final String SEARCH_END = "SEARCH_END";

    // =========================================================
    // REACTION — CẢM XÚC TIN NHẮN
    // =========================================================

    // Client → Server: thả / bỏ cảm xúc.
    // Format: REACT|messageId|emoji
    public static final String REACT = "REACT";

    // Server → tất cả client trong phòng: cập nhật reaction.
    // Format: REACTION_UPDATE|messageId|❤️:3,😂:1,👍:5
    //         (chỉ gửi các emoji có count > 0)
    public static final String REACTION_UPDATE = "REACTION_UPDATE";

    // =========================================================
    // PRIVATE CHAT (1-1) — TIN NHẮN RIÊNG TƯ
    // =========================================================

    // Client → Server: gửi tin nhắn riêng cho 1 người
    // Format: SEND_PRIVATE|targetUsername|content
    public static final String SEND_PRIVATE = "SEND_PRIVATE";

    // Server → Client: thông báo có tin nhắn riêng (gửi cho cả người nhận và người gửi)
    // Format: PRIVATE_CHAT|senderUsername|senderDisplayName|receiverUsername|receiverDisplayName|sentAt|content
    public static final String PRIVATE_CHAT = "PRIVATE_CHAT";

    // Client → Server: yêu cầu tải lịch sử tin nhắn riêng với một người
    // Format: GET_PRIVATE_HISTORY|targetUsername
    public static final String GET_PRIVATE_HISTORY = "GET_PRIVATE_HISTORY";

    // Server → Client: một dòng lịch sử tin nhắn riêng
    // Format: PRIVATE_HISTORY|senderUsername|senderDisplayName|receiverUsername|receiverDisplayName|sentAt|content
    public static final String PRIVATE_HISTORY = "PRIVATE_HISTORY";

    // Server → Client: báo hiệu tải xong lịch sử tin nhắn riêng
    // Format: PRIVATE_HISTORY_END|targetUsername|count
    public static final String PRIVATE_HISTORY_END = "PRIVATE_HISTORY_END";

    // =========================================================
    // THU HỒI TIN NHẮN (RECALL MESSAGE)
    // =========================================================

    // Client → Server: yêu cầu thu hồi tin nhắn của chính mình
    // Format: RECALL_MESSAGE|messageId
    public static final String RECALL_MESSAGE = "RECALL_MESSAGE";

    // Server → Room: thông báo tin nhắn đã bị thu hồi
    // Format: MESSAGE_RECALLED|roomName|messageId
    public static final String MESSAGE_RECALLED = "MESSAGE_RECALLED";

    // =========================================================
    // TRẢ LỜI TIN NHẮN (REPLY MESSAGE)
    // =========================================================

    // Client → Server: gửi tin nhắn trả lời một tin nhắn cụ thể
    // Format: REPLY_MESSAGE|replyToMessageId|content
    public static final String REPLY_MESSAGE = "REPLY_MESSAGE";

    // Server → Room / Client: thông tin trích dẫn của tin nhắn trả lời
    // Format: REPLY_INFO|messageId|replyToId|replySenderDisplayName|replySnippet
    public static final String REPLY_INFO = "REPLY_INFO";

    // =========================================================
    // TRẠNG THÁI SOẠN TIN NHẮN (TYPING INDICATOR)
    // =========================================================

    // Client → Server: thông báo bắt đầu/dừng gõ tin nhắn trong phòng
    // Format: TYPING|isTyping (1 = đang gõ, 0 = dừng gõ)
    public static final String TYPING = "TYPING";

    // =========================================================
    // TRẠNG THÁI ĐÃ XEM TIN NHẮN (SEEN INDICATOR)
    // =========================================================

    // Client -> Server: Báo hiệu đã đọc/xem tin nhắn trong phòng
    // Server -> Room: Báo cho người trong phòng biết user đã xem
    // Format: SEEN_MESSAGE|roomName|username|displayName
    public static final String SEEN_MESSAGE = "SEEN_MESSAGE";

    private Protocol() {
    }


    /**
     * Tạo message theo format:
     *
     * COMMAND|DATA1|DATA2|...
     */
    public static String build(
            String command,
            String... data) {

        StringBuilder message =
                new StringBuilder(command);


        for (String item : data) {

            String safeItem =
                    item
                            .replace("\r", " ")
                            .replace("\n", " ");


            message
                    .append("|")
                    .append(safeItem);
        }


        return message.toString();
    }
}