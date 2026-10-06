/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;
import common.EncodingUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/**
 *
 * @author DELL
 */
public class ServerMain {
    private static final int PORT = 5000;


    public static void main(String[] args) {

        // =====================================================
        // Cấu hình Console sử dụng UTF-8.
        //
        // PHẢI đặt trước mọi System.out.println().
        // =====================================================

        EncodingUtil.setupUTF8();


        // =====================================================
        // Hiển thị thông tin Server
        // =====================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "           GROUP CHAT SERVER"
        );

        System.out.println(
                "=========================================="
        );


        System.out.println(
                "Port: " + PORT
        );


        // =====================================================
        // Tạo ClientManager
        // =====================================================

        ClientManager clientManager =
                new ClientManager();


        // =====================================================
        // Tạo RoomManager
        // =====================================================

        RoomManager roomManager =
                new RoomManager();


        System.out.println(
                "Phòng mặc định: "
                + roomManager.getRoomNames()
        );


        // =====================================================
        // Khởi động ServerSocket
        // =====================================================

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


            // =================================================
            // Server chạy liên tục
            // =================================================

            while (true) {

                /*
                 * accept() sẽ chờ cho tới khi
                 * một Client kết nối.
                 */
                Socket socket =
                        serverSocket.accept();


                System.out.println(
                        "[SERVER] Client kết nối từ: "
                        + socket.getInetAddress()
                        + ":"
                        + socket.getPort()
                );


                // =================================================
                // Tạo ClientHandler
                // =================================================

                ClientHandler clientHandler =
                        new ClientHandler(
                                socket,
                                clientManager,
                                roomManager
                        );


                // =================================================
                // Mỗi Client chạy Thread riêng
                // =================================================

                Thread clientThread =
                        new Thread(
                                clientHandler
                        );


                clientThread.start();


                System.out.println(
                        "[SERVER] Đã tạo Thread cho Client."
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
