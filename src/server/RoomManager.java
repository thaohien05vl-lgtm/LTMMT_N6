/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/**
 *
 * @author DELL
 */
public class RoomManager {
     /**
     * key   = tên phòng
     * value = tập ClientHandler đang ở trong phòng
     */
    private final Map<String, Set<ClientHandler>> rooms
            = new ConcurrentHashMap<>();


    /**
     * Tạo các phòng mặc định.
     *
     * Có tiếng Việt để kiểm tra UTF-8.
     */
    public RoomManager() {

        createRoom("Sảnh chính");

        createRoom("Lập trình mạng");

        createRoom("Giải trí");

        createRoom("Học nhóm");
    }


    /**
     * Lấy tên phòng chính xác đang lưu trong Map.
     * So sánh không phân biệt hoa thường và chuẩn hóa NFC.
     */
    public String getExactRoomKey(String roomName) {
        if (roomName == null) {
            return null;
        }
        String normalized = Normalizer.normalize(
                roomName.trim(),
                Normalizer.Form.NFC
        );
        for (String key : rooms.keySet()) {
            if (key.equalsIgnoreCase(normalized)) {
                return key;
            }
        }
        return normalized;
    }

    /**
     * Tạo phòng mới.
     */
    public boolean createRoom(
            String roomName) {

        if (
                roomName == null
                || roomName.trim().isEmpty()
        ) {

            return false;
        }

        String normalized = Normalizer.normalize(
                roomName.trim(),
                Normalizer.Form.NFC
        );

        if (exists(normalized)) {
            return false;
        }

        rooms.put(
                normalized,
                ConcurrentHashMap.newKeySet()
        );

        return true;
    }


    /**
     * Kiểm tra phòng tồn tại (không phân biệt hoa thường, chuẩn hóa NFC).
     */
    public boolean exists(
            String roomName) {

        if (roomName == null || roomName.trim().isEmpty()) {
            return false;
        }

        String normalized = Normalizer.normalize(
                roomName.trim(),
                Normalizer.Form.NFC
        );

        for (String key : rooms.keySet()) {
            if (key.equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Thêm Client vào phòng.
     */
    public void addClient(
            String roomName,
            ClientHandler client) {

        String key = getExactRoomKey(roomName);

        Set<ClientHandler> members =
                rooms.get(key);


        if (members != null) {

            members.add(client);
        }
    }


    /**
     * Xóa Client khỏi phòng.
     */
    public void removeClient(
            String roomName,
            ClientHandler client) {

        String key = getExactRoomKey(roomName);

        Set<ClientHandler> members =
                rooms.get(key);


        if (members != null) {

            members.remove(client);
        }
    }


    /**
     * Broadcast message tới toàn bộ
     * thành viên của một phòng.
     */
    public void broadcast(
            String roomName,
            String message) {

        String key = getExactRoomKey(roomName);

        Set<ClientHandler> members =
                rooms.get(key);


        if (members == null) {
            return;
        }


        for (
                ClientHandler client
                : members
        ) {

            client.send(message);
        }
    }




    /**
     * Lấy danh sách tên phòng.
     */
    public List<String> getRoomNames() {

        List<String> result =
                new ArrayList<>(
                        rooms.keySet()
                );


        Collections.sort(result);


        return result;
    }


    /**
     * Lấy danh sách username
     * trong một phòng.
     */
    public List<String> getUserNames(
            String roomName) {

        String key = getExactRoomKey(roomName);

        Set<ClientHandler> members =
                rooms.get(key);


        if (members == null) {

            return new ArrayList<>();
        }


        List<String> result =
                new ArrayList<>();


        for (
                ClientHandler client
                : members
        ) {

            if (
                    client.getUsername()
                    != null
            ) {

                result.add(
                        client.getUsername()
                );
            }
        }


        Collections.sort(result);


        return result;
    }


    /**
     * Xóa Client khỏi toàn bộ phòng.
     */
    public void removeFromAllRooms(
            ClientHandler client) {

        for (
                Set<ClientHandler> members
                : rooms.values()
        ) {

            members.remove(client);
        }
    }
}