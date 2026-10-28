/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ClientOutbox:
 * Quản lý hàng đợi tin nhắn ngoại tuyến (Offline Outbox) lưu trữ bền vững trên ổ cứng Client.
 *
 * Tính năng chuẩn như Facebook Messenger & Zalo:
 * - Khi mất mạng, tin nhắn vẫn hiển thị lên bong bóng của người dùng với trạng thái "Đang gửi...".
 * - Tin nhắn được ghi vào file cục bộ trên máy Client gắn với username.
 * - Dù người dùng Đăng xuất (Logout) hay tắt hẳn tiến trình ứng dụng, tin nhắn vẫn được lưu an toàn.
 * - Khi có mạng trở lại và đăng nhập, ứng dụng tự động đồng bộ gửi bù (flush) lên Server.
 * - Server tiếp nhận, lưu SQLite và phản hồi kèm thời gian thực lúc gửi thành công.
 */
public final class ClientOutbox {

    public static final class OutboxItem {
        public final int tempId;
        public final String username;
        public final String room;
        public final int replyToId;
        public final String replySender;
        public final String replySnippet;
        public final String content;
        public final String createdAt;

        public OutboxItem(
                int tempId,
                String username,
                String room,
                int replyToId,
                String replySender,
                String replySnippet,
                String content,
                String createdAt) {
            this.tempId = tempId;
            this.username = username == null ? "" : username.trim();
            this.room = room == null ? "" : room.trim();
            this.replyToId = replyToId;
            this.replySender = replySender == null ? "" : replySender;
            this.replySnippet = replySnippet == null ? "" : replySnippet;
            this.content = content == null ? "" : content;
            this.createdAt = createdAt == null ? "" : createdAt;
        }
    }

    private static final String DATA_DIR = "data";
    private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    private ClientOutbox() {
    }

    private static File getFile(String username) {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String safeName = (username == null || username.isBlank()) ? "guest" : username.trim().toLowerCase();
        return new File(dir, "outbox_" + safeName + ".txt");
    }

    private static String enc(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return B64_ENC.encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        try {
            return new String(B64_DEC.decode(s), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    public static synchronized List<OutboxItem> load(String username) {
        File file = getFile(username);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        List<OutboxItem> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 8) {
                    try {
                        int tempId = Integer.parseInt(parts[0].trim());
                        String u = parts[1].trim();
                        String room = parts[2].trim();
                        int replyId = Integer.parseInt(parts[3].trim());
                        String replySender = dec(parts[4]);
                        String replySnippet = dec(parts[5]);
                        String content = dec(parts[6]);
                        String createdAt = parts[7].trim();
                        list.add(new OutboxItem(tempId, u, room, replyId, replySender, replySnippet, content, createdAt));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[OUTBOX] Lỗi khi nạp danh sách chờ gửi: " + e.getMessage());
        }
        return list;
    }

    public static synchronized void save(String username, List<OutboxItem> list) {
        File file = getFile(username);
        if (list == null || list.isEmpty()) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            for (OutboxItem it : list) {
                String line = it.tempId + "|"
                        + it.username + "|"
                        + it.room + "|"
                        + it.replyToId + "|"
                        + enc(it.replySender) + "|"
                        + enc(it.replySnippet) + "|"
                        + enc(it.content) + "|"
                        + it.createdAt;
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[OUTBOX] Lỗi khi lưu danh sách chờ gửi: " + e.getMessage());
        }
    }

    public static synchronized void addItem(String username, OutboxItem item) {
        List<OutboxItem> list = load(username);
        list.add(item);
        save(username, list);
    }

    public static synchronized List<OutboxItem> getItemsForRoom(String username, String room) {
        List<OutboxItem> all = load(username);
        List<OutboxItem> res = new ArrayList<>();
        if (room == null) {
            return res;
        }
        for (OutboxItem it : all) {
            if (room.equalsIgnoreCase(it.room)) {
                res.add(it);
            }
        }
        return res;
    }

    public static synchronized void removeByTempId(String username, int tempId) {
        List<OutboxItem> list = load(username);
        if (list.isEmpty()) {
            return;
        }
        boolean removed = list.removeIf(it -> it.tempId == tempId);
        if (removed) {
            save(username, list);
        }
    }

    public static synchronized boolean removeFirstMatching(String username, String room, String content) {
        List<OutboxItem> list = load(username);
        if (list.isEmpty() || content == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            OutboxItem it = list.get(i);
            if ((room == null || room.equalsIgnoreCase(it.room)) && content.equals(it.content)) {
                list.remove(i);
                save(username, list);
                return true;
            }
        }
        return false;
    }
}
