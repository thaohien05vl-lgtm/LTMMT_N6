/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
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


        roomName =
                roomName.trim();


        if (
                rooms.containsKey(
                        roomName
                )
        ) {

            return false;
        }


        rooms.put(
                roomName,
                ConcurrentHashMap.newKeySet()
        );


        return true;
    }


    /**
     * Kiểm tra phòng tồn tại.
     */
    public boolean exists(
            String roomName) {

        return roomName != null
                && rooms.containsKey(
                        roomName
                );
    }


    /**
     * Thêm Client vào phòng.
     */
    public void addClient(
            String roomName,
            ClientHandler client) {

        Set<ClientHandler> members =
                rooms.get(roomName);


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

        Set<ClientHandler> members =
                rooms.get(roomName);


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

        Set<ClientHandler> members =
                rooms.get(roomName);


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

        Set<ClientHandler> members =
                rooms.get(roomName);


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
