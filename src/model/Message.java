/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 * Message.java
 *
 * Đại diện cho một tin nhắn trong ứng dụng Group Chat.
 *
 * Nhiệm vụ:
 * - Lưu thông tin của một tin nhắn trong bộ nhớ.
 * - Chuyển dữ liệu giữa Server, DAO và các thành phần khác.
 *
 * Lưu ý:
 * - Class này không trực tiếp truy vấn Database.
 * - Việc đọc và ghi SQLite được thực hiện trong MessageDAO.
 */

public class Message {
    // ID tự tăng của tin nhắn trong Database.
    private int id;

    // ID phòng chat mà tin nhắn thuộc về.
    private int roomId;

    // ID tài khoản của người gửi trong bảng users.
    private int senderId;

    // Tên đăng nhập tại thời điểm gửi.
    private String senderUsername;

    // Tên hiển thị tại thời điểm gửi.
    private String senderDisplayName;

    // Nội dung tin nhắn.
    private String content;

    // Thời gian gửi được lấy từ Database.
    private String sentAt;

    // ID của tin nhắn được trả lời (0 nếu không phải tin trả lời).
    private int replyToId;

    // Tên hiển thị của người gửi tin nhắn gốc (phục vụ hiển thị trích dẫn).
    private String replySenderDisplayName;

    // Nội dung trích dẫn ngắn của tin nhắn gốc.
    private String replySnippet;


    /**
     * Constructor rỗng.
     *
     * Hữu ích khi cần tạo đối tượng trước,
     * rồi mới thiết lập dữ liệu bằng setter.
     */
    public Message() {
    }


    /**
     * Constructor dùng khi tạo tin nhắn mới.
     *
     * Chưa cần truyền id và sentAt vì:
     * - id do SQLite tự tăng.
     * - sentAt do Database tự tạo.
     */
    public Message(
            int roomId,
            int senderId,
            String senderUsername,
            String senderDisplayName,
            String content) {

        this.roomId = roomId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.content = content;
    }


    /**
     * Constructor đầy đủ.
     *
     * Dùng khi đọc một tin nhắn đã tồn tại
     * trong Database.
     */
    public Message(
            int id,
            int roomId,
            int senderId,
            String senderUsername,
            String senderDisplayName,
            String content,
            String sentAt) {

        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.content = content;
        this.sentAt = sentAt;
    }


    // =====================================================
    // GETTER VÀ SETTER
    // =====================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }


    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }


    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }


    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public int getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(int replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplySenderDisplayName() {
        return replySenderDisplayName;
    }

    public void setReplySenderDisplayName(String replySenderDisplayName) {
        this.replySenderDisplayName = replySenderDisplayName;
    }

    public String getReplySnippet() {
        return replySnippet;
    }

    public void setReplySnippet(String replySnippet) {
        this.replySnippet = replySnippet;
    }


    /**
     * Trả về nội dung đại diện cho tin nhắn.
     *
     * Hữu ích khi kiểm thử bằng System.out.println(message).
     */
    @Override
    public String toString() {

        return "Message{"
                + "id=" + id
                + ", roomId=" + roomId
                + ", senderId=" + senderId
                + ", senderUsername='" + senderUsername + '\''
                + ", senderDisplayName='" + senderDisplayName + '\''
                + ", content='" + content + '\''
                + ", sentAt='" + sentAt + '\''
                + '}';
    }
}
