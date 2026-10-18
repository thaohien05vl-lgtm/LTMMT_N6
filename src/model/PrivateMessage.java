/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 * PrivateMessage:
 *
 * Đại diện cho một tin nhắn riêng tư 1-1 giữa hai người dùng.
 * Được lưu trữ trong bảng `private_messages` của SQLite.
 */
public class PrivateMessage {

    private int id;
    private int senderId;
    private String senderUsername;
    private String senderDisplayName;

    private int receiverId;
    private String receiverUsername;
    private String receiverDisplayName;

    private String content;
    private String sentAt;

    /**
     * Constructor phục vụ tạo tin nhắn mới trước khi lưu vào Database.
     */
    public PrivateMessage(
            int senderId,
            String senderUsername,
            String senderDisplayName,
            int receiverId,
            String receiverUsername,
            String receiverDisplayName,
            String content) {

        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.receiverId = receiverId;
        this.receiverUsername = receiverUsername;
        this.receiverDisplayName = receiverDisplayName;
        this.content = content;
    }

    /**
     * Constructor đầy đủ khi đọc từ Database.
     */
    public PrivateMessage(
            int id,
            int senderId,
            String senderUsername,
            String senderDisplayName,
            int receiverId,
            String receiverUsername,
            String receiverDisplayName,
            String content,
            String sentAt) {

        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderDisplayName = senderDisplayName;
        this.receiverId = receiverId;
        this.receiverUsername = receiverUsername;
        this.receiverDisplayName = receiverDisplayName;
        this.content = content;
        this.sentAt = sentAt;
    }

    // ==========================================================
    // GETTERS & SETTERS
    // ==========================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getReceiverDisplayName() {
        return receiverDisplayName;
    }

    public void setReceiverDisplayName(String receiverDisplayName) {
        this.receiverDisplayName = receiverDisplayName;
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

    @Override
    public String toString() {
        return "PrivateMessage{"
                + "id=" + id
                + ", from='" + senderUsername + '\''
                + ", to='" + receiverUsername + '\''
                + ", content='" + content + '\''
                + ", sentAt='" + sentAt + '\''
                + '}';
    }
}
