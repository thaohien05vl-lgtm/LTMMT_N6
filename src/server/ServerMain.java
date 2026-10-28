/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import common.EncodingUtil;
import database.DatabaseInitializer;
import database.MessageDAO;
import database.PrivateMessageDAO;
import database.ReactionDAO;
import database.RoomDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerMain:
 *
 * Chương trình Server của Group Chat.
 *
 * Phase 2:
 * - Khởi tạo SQLite
 * - Authentication
 * - User Account
 */
public class ServerMain {

    private static final int PORT = 5000;


    /** Trả về thời gian hiện tại định dạng [HH:mm:ss]. */
    private static String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("[yyyy-MM-dd HH:mm:ss]"));
    }


    public static void main(String[] args) {

        // =====================================================
        // UTF-8 cho Console
        // =====================================================

        EncodingUtil.setupUTF8();


        // =====================================================
        // Khởi tạo Database trước khi Server hoạt động.
        // =====================================================

        try {

            DatabaseInitializer.initialize();

        } catch (Exception e) {

            System.out.println(
                    "[SERVER] Không thể khởi tạo Database."
            );

            e.printStackTrace();

            return;
        }


        // =====================================================
        // Manager
        // =====================================================

        ClientManager clientManager =
                new ClientManager();


        RoomManager roomManager =
                new RoomManager();


        // DAO quản lý tài khoản.
        database.UserDAO userDAO =
                new database.UserDAO();
                
        // DAO quản lý phòng trong Database.
        RoomDAO roomDAO = new RoomDAO();
        
        // DAO lưu và đọc lịch sử tin nhắn.
        MessageDAO messageDAO = new MessageDAO();

        // DAO quản lý cảm xúc tin nhắn.
        ReactionDAO reactionDAO = new ReactionDAO();

        // DAO quản lý tin nhắn riêng tư 1-1.
        PrivateMessageDAO privateMessageDAO = new PrivateMessageDAO();


        // =====================================================
        // Nạp tất cả phòng đã lưu trong SQLite vào bộ nhớ Server.
        // RoomManager() tự khởi tạo 4 phòng mặc định, nhưng
        // các phòng do người dùng tạo chỉ nằm trong DB.
        // =====================================================
        try {

            java.util.List<String> dbRooms =
                    roomDAO.findAllRoomNames();

            for (String name : dbRooms) {
                // createRoom() bỏ qua nếu phòng đã tồn tại (case-insensitive NFC).
                roomManager.createRoom(name);
            }

            System.out.println(
                    "[SERVER] Đã nạp "
                    + dbRooms.size()
                    + " phòng từ Database."
            );

        } catch (java.sql.SQLException e) {

            System.err.println(
                    "[SERVER] Không thể nạp danh sách phòng từ Database."
            );

            e.printStackTrace();
        }


        System.out.println(
                "=========================================="
        );


        System.out.println(
                "          GROUP CHAT SERVER"
        );


        System.out.println(
                "=========================================="
        );


        System.out.println(
                "Port: "
                + PORT
        );


        System.out.println(
                "Phòng đang hoạt động: "
                + roomManager.getRoomNames()
        );


        System.out.println(
                "Database: "
                + database.DatabaseConnection
                        .getDatabasePath()
        );


        try (
                ServerSocket serverSocket =
                        new ServerSocket(PORT)
        ) {

            System.out.println(
                    "[SERVER] Server đã khởi động."
            );


            System.out.println(
                    "[SERVER] Đang chờ Client..."
            );


            while (true) {

                Socket socket =
                        serverSocket.accept();


                System.out.println(
                        now() + " [SERVER] Client kết nối từ: "
                        + socket.getInetAddress()
                        + ":"
                        + socket.getPort()
                );


                ClientHandler clientHandler =
                        new ClientHandler(
                                socket,
                                clientManager,
                                roomManager,
                                userDAO,
                                roomDAO,
                                messageDAO,
                                reactionDAO,
                                privateMessageDAO
                        );


                Thread clientThread =
                        new Thread(
                                clientHandler
                        );


                clientThread.start();


                System.out.println(
                        now() + " [SERVER] Đã tạo Thread cho Client."
                );
            }


        } catch (IOException e) {

            System.out.println(
                    "[SERVER] Không thể khởi động Server."
            );


            System.out.println(
                    "Chi tiết: "
                    + e.getMessage()
            );
        }
    }
}