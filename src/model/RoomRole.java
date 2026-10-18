/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package model;

import java.util.Locale;

/**
 * RoomRole:
 *
 * Quy định các vai trò trong một phòng chat.
 *
 * OWNER  : Chủ phòng, có toàn quyền quản lý phòng.
 * ADMIN  : Quản trị viên, có thể quản lý thành viên.
 * MEMBER : Thành viên bình thường.
 *
 * Lưu ý:
 * Class này định nghĩa chính sách quyền.
 * Server phải gọi các hàm này khi xử lý yêu cầu thực tế.
 */
public enum RoomRole {

    OWNER,
    ADMIN,
    MEMBER;

    // ==========================================================
    // KIỂM TRA QUYỀN QUẢN LÝ THÀNH VIÊN
    // ==========================================================

    /**
     * OWNER và ADMIN được quản lý thành viên trong phòng.
     */
    public boolean canManageMembers() {

        return this == OWNER
                || this == ADMIN;
    }

    // ==========================================================
    // KIỂM TRA QUYỀN QUẢN LÝ ADMIN
    // ==========================================================

    /**
     * Chỉ OWNER được bổ nhiệm hoặc thay đổi vai trò ADMIN.
     */
    public boolean canManageAdmins() {

        return this == OWNER;
    }

    /**
     * Chỉ OWNER được bổ nhiệm ADMIN mới.
     */
    public boolean canPromoteAdmin() {

        return this == OWNER;
    }

    // ==========================================================
    // KIỂM TRA QUYỀN KICK
    // ==========================================================

    /**
     * Kiểm tra vai trò hiện tại có được Kick
     * một người mang targetRole hay không.
     *
     * Chưa kiểm tra người thực hiện có phải chính mình hay không.
     * Việc đó phải được Server kiểm tra bằng tài khoản/ID.
     */
    public boolean canKick(RoomRole targetRole) {

        if (targetRole == null) {
            return false;
        }

        // Không ai được Kick chủ phòng bằng quy tắc này.
        if (targetRole == OWNER) {
            return false;
        }

        // Owner được Kick Admin hoặc Member.
        if (this == OWNER) {
            return true;
        }

        // Admin chỉ được Kick Member.
        if (this == ADMIN) {
            return targetRole == MEMBER;
        }

        // Member không có quyền Kick.
        return false;
    }

    // ==========================================================
    // KIỂM TRA QUYỀN MUTE
    // ==========================================================

    /**
     * Kiểm tra vai trò hiện tại có được Mute
     * một người mang targetRole hay không.
     */
    public boolean canMute(RoomRole targetRole) {

        if (targetRole == null) {
            return false;
        }

        // Owner luôn được bảo vệ khỏi hành động Mute.
        if (targetRole == OWNER) {
            return false;
        }

        if (this == OWNER) {
            return true;
        }

        if (this == ADMIN) {
            return targetRole == MEMBER;
        }

        return false;
    }

    // ==========================================================
    // QUYỀN THAY ĐỔI CẤU HÌNH PHÒNG
    // ==========================================================

    /**
     * Chỉ Owner được thay đổi trạng thái công khai/riêng tư.
     */
    public boolean canChangeRoomPrivacy() {

        return this == OWNER;
    }

    /**
     * Chỉ Owner được xóa phòng.
     */
    public boolean canDeleteRoom() {

        return this == OWNER;
    }

    /**
     * Owner và Admin có thể gửi lời mời thành viên.
     * Kiểm tra quyền truy cập phòng riêng vẫn do Server thực hiện.
     */
    public boolean canInviteMembers() {

        return this == OWNER
                || this == ADMIN;
    }

    // ==========================================================
    // ĐỌC ROLE TỪ DATABASE
    // ==========================================================

    /**
     * Chuyển chuỗi trong Database thành RoomRole.
     *
     * Ví dụ:
     * "OWNER"  -> OWNER
     * "admin"  -> ADMIN
     * "MEMBER" -> MEMBER
     *
     * Nếu dữ liệu không hợp lệ, mặc định là MEMBER
     * để tránh cấp quyền cao do dữ liệu sai.
     */
    public static RoomRole fromDatabase(String value) {

        if (value == null || value.trim().isEmpty()) {
            return MEMBER;
        }

        try {

            return RoomRole.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException e) {

            return MEMBER;
        }
    }
}
