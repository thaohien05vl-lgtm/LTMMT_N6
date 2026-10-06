/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;
import common.EncodingUtil;
import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
/**
 *
 * @author TUAN HUY
 */
public class ClientMain {
    // Địa chỉ Server.
    private static final String SERVER_HOST =
            "127.0.0.1";

    // Port Server.
    private static final int SERVER_PORT =
            5000;


    public static void main(String[] args) {

        // =====================================================
        // Cấu hình Console UTF-8
        //
        // PHẢI gọi trước System.out.println().
        // =====================================================

        EncodingUtil.setupUTF8();


        // =====================================================
        // Tiêu đề Client
        // =====================================================

        System.out.println(
                "================================"
        );

        System.out.println(
                "          GROUP CHAT CLIENT"
        );

        System.out.println(
                "================================"
        );


        try (

                // =================================================
                // Scanner đọc bàn phím.
                // UTF-8 hỗ trợ nhập tiếng Việt.
                // =================================================

                Scanner scanner =
                        new Scanner(
                                System.in,
                                StandardCharsets.UTF_8
                        );


                // =================================================
                // Kết nối tới Server
                // =================================================

                Socket socket =
                        new Socket(
                                SERVER_HOST,
                                SERVER_PORT
                        );


                // =================================================
                // Đọc Server → Client
                //
                // UTF-8
                // =================================================

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );


                // =================================================
                // Gửi Client → Server
                //
                // UTF-8
                // =================================================

                PrintWriter writer =
                        new PrintWriter(
                                new OutputStreamWriter(
                                        socket.getOutputStream(),
                                        StandardCharsets.UTF_8
                                ),
                                true
                        )
        ) {

            System.out.println(
                    "[CLIENT] Đã kết nối Server."
            );


            // =================================================
            // Nhập username
            // =================================================

            System.out.print(
                    "Nhập username: "
            );


            String username =
                    scanner.nextLine().trim();


            // =================================================
            // Gửi LOGIN
            // =================================================

            writer.println(
                    Protocol.build(
                            Protocol.LOGIN,
                            username
                    )
            );


            // =================================================
            // Thread nhận dữ liệu Server
            // =================================================

            Thread readerThread =
                    new Thread(() -> {

                        try {

                            String response;


                            while (
                                    (response =
                                            reader.readLine())
                                            != null
                            ) {

                                processServerMessage(
                                        response
                                );
                            }


                        } catch (IOException e) {

                            System.out.println(
                                    "\n[CLIENT] "
                                    + "Mất kết nối Server."
                            );
                        }

                    });


            readerThread.start();


            // =================================================
            // Main Thread:
            // đọc bàn phím và gửi Server
            // =================================================

            while (true) {

                String input =
                        scanner.nextLine();


                // =================================================
                // Xem danh sách phòng
                // =================================================

                if (
                        input.equalsIgnoreCase(
                                "/rooms"
                        )
                ) {

                    writer.println(
                            Protocol.GET_ROOMS
                    );
                }


                // =================================================
                // Xem thành viên
                // =================================================

                else if (
                        input.equalsIgnoreCase(
                                "/users"
                        )
                ) {

                    writer.println(
                            Protocol.GET_USERS
                    );
                }


                // =================================================
                // Join phòng
                //
                // Ví dụ:
                // /join Lập trình mạng
                // =================================================

                else if (
                        input.toLowerCase()
                                .startsWith(
                                        "/join "
                                )
                ) {

                    String roomName =
                            input.substring(6)
                                    .trim();


                    if (roomName.isEmpty()) {

                        System.out.println(
                                "Ví dụ: /join Lập trình mạng"
                        );

                        continue;
                    }


                    writer.println(
                            Protocol.build(
                                    Protocol.JOIN_ROOM,
                                    roomName
                            )
                    );
                }


                // =================================================
                // Thoát
                // =================================================

                else if (
                        input.equalsIgnoreCase(
                                "/quit"
                        )
                ) {

                    writer.println(
                            Protocol.LOGOUT
                    );

                    break;
                }


                // =================================================
                // Tin nhắn
                // =================================================

                else {

                    if (
                            input.trim().isEmpty()
                    ) {

                        continue;
                    }


                    writer.println(
                            Protocol.build(
                                    Protocol.SEND_MESSAGE,
                                    input
                            )
                    );
                }
            }


        } catch (IOException e) {

            System.out.println(
                    "[CLIENT] Không thể kết nối Server."
            );


            System.out.println(
                    "Kiểm tra Server đã chạy chưa."
            );


            System.out.println(
                    "Chi tiết: "
                    + e.getMessage()
            );
        }
    }


    /**
     * Xử lý dữ liệu Server gửi về.
     */
    private static void processServerMessage(
            String response) {

        if (
                response == null
                || response.isEmpty()
        ) {

            return;
        }


        /*
         * Ví dụ:
         *
         * CHAT|Lập trình mạng|Phúc|Xin chào mọi người
         *
         * split tối đa 4 phần để giữ nguyên
         * phần nội dung tin nhắn.
         */
        String[] parts =
                response.split("\\|", 4);


        String command =
                parts[0];


        switch (command) {

            case Protocol.SYSTEM:

                if (parts.length >= 2) {

                    System.out.println(
                            "[SYSTEM] "
                            + parts[1]
                    );
                }

                break;


            case Protocol.ERROR:

                if (parts.length >= 2) {

                    System.out.println(
                            "[ERROR] "
                            + parts[1]
                    );
                }

                break;


            case Protocol.LOGIN_SUCCESS:

                if (parts.length >= 2) {

                    System.out.println(
                            "[LOGIN] "
                            + parts[1]
                    );
                }


                System.out.println(
                        "--------------------------------"
                );


                System.out.println(
                        "Lệnh sử dụng:"
                );


                System.out.println(
                        "/rooms  - Xem danh sách phòng"
                );


                System.out.println(
                        "/users  - Xem thành viên"
                );


                System.out.println(
                        "/join Lập trình mạng"
                );


                System.out.println(
                        "/join Giải trí"
                );


                System.out.println(
                        "/join Học nhóm"
                );


                System.out.println(
                        "/join Sảnh chính"
                );


                System.out.println(
                        "/quit   - Thoát"
                );


                System.out.println(
                        "--------------------------------"
                );

                break;


            case Protocol.ROOM_LIST:

                if (parts.length >= 2) {

                    System.out.println(
                            "[ROOMS] "
                            + parts[1]
                    );
                }

                break;


            case Protocol.USER_LIST:

                if (parts.length >= 3) {

                    System.out.println(
                            "[USERS - "
                            + parts[1]
                            + "] "
                            + parts[2]
                    );
                }

                break;


            case Protocol.CHAT:

                if (parts.length >= 4) {

                    String room =
                            parts[1];

                    String sender =
                            parts[2];

                    String message =
                            parts[3];


                    System.out.println(
                            "["
                            + room
                            + "] "
                            + sender
                            + ": "
                            + message
                    );
                }

                break;


            default:

                System.out.println(
                        "[SERVER] "
                        + response
                );
        }
    }
}
