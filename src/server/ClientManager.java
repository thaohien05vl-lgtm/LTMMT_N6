/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 *
 * @author DELL
 */
public class ClientManager {
     private final Map<String, ClientHandler> onlineClients
            = new ConcurrentHashMap<>();


    public boolean register(
            String username,
            ClientHandler client) {

        String key =
                username.toLowerCase(
                        Locale.ROOT
                );

        return onlineClients.putIfAbsent(
                key,
                client
        ) == null;
    }


    public void remove(
            String username,
            ClientHandler client) {

        if (username == null) {
            return;
        }


        String key =
                username.toLowerCase(
                        Locale.ROOT
                );


        onlineClients.remove(
                key,
                client
        );
    }


    public boolean isOnline(
            String username) {

        if (username == null) {
            return false;
        }


        String key =
                username.toLowerCase(
                        Locale.ROOT
                );


        return onlineClients.containsKey(
                key
        );
    }

    /**
     * Lấy ClientHandler của một người dùng theo username nếu đang online.
     * Trả về null nếu không tìm thấy hoặc người dùng đã offline.
     */
    public ClientHandler getClient(String username) {
        if (username == null) {
            return null;
        }
        return onlineClients.get(username.toLowerCase(Locale.ROOT));
    }
    
    // ==========================================================
// GỬI THÔNG ĐIỆP ĐẾN TẤT CẢ CLIENT ĐANG ONLINE
// ==========================================================

/**
 * Broadcast một thông điệp đến các tài khoản online.
 *
 * Dùng để cập nhật danh sách phòng sau khi tạo phòng mới.
 * Không tạo Socket mới; sử dụng ClientHandler đang tồn tại.
 */
public void broadcast(String message) {

    for (ClientHandler client : onlineClients.values()) {

        if (client != null) {
            client.send(message);
        }
    }
}
}