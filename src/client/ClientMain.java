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

import java.util.Base64;
import java.util.Scanner;

/**
 * ClientMain:
 *
 * Client Console của Group Chat.
 *
 * Phase 2:
 *
 * - Đăng ký
 * - Đăng nhập
 * - Password
 * - SQLite authentication
 */
public class ClientMain {

    private static final String SERVER_HOST =
            System.getProperty("server.host", "10.148.92.193");

    private static final int SERVER_PORT =
            5000;


    public static void main(
            String[] args) {

        EncodingUtil.setupUTF8();


        System.out.println(
                "===================================="
        );


        System.out.println(
                "          GROUP CHAT CLIENT"
        );


        System.out.println(
                "===================================="
        );


        try (

                Scanner scanner =
                        new Scanner(
                                System.in,
                                StandardCharsets.UTF_8
                        );


                Socket socket =
                        new Socket(
                                SERVER_HOST,
                                SERVER_PORT
                        );


                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );


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
            // Đọc 2 message ban đầu từ Server.
            // =================================================

            for (int i = 0; i < 2; i++) {

                String response =
                        reader.readLine();


                if (response == null) {

                    return;
                }


                processServerMessage(
                        response
                );
            }


            // =================================================
            // AUTH MENU
            // =================================================

            boolean loggedIn =
                    authenticationMenu(
                            scanner,
                            reader,
                            writer
                    );


            if (!loggedIn) {

                return;
            }


            // =================================================
            // Sau khi login:
            //
            // Tạo Reader Thread.
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
            // Menu chat
            // =================================================

            printChatHelp();


            while (true) {

                String input =
                        scanner.nextLine();


                // =================================================
                // ROOMS
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
                // USERS
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
                // JOIN
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


                    if (
                            roomName.isEmpty()
                    ) {

                        System.out.println(
                                "Ví dụ: /join Học nhóm"
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
                // QUIT
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
                // TÌM KIẾM TIN NHẮN
                //
                // Ví dụ:
                // /search Socket
                // /search Java Network
                // =================================================
                else if (
                        input.regionMatches(
                                true,
                                0,
                                "/search ",
                                0,
                                8
                        )
                ) {

                    // Cắt bỏ phần "/search " để lấy từ khóa.
                    String keyword = input.substring(8).trim();

                    if (keyword.isEmpty()) {

                        System.out.println(
                                "Cú pháp: /search từ khóa"
                        );

                        continue;
                    }

                    // Gửi yêu cầu tìm kiếm đến Server.
                    writer.println(
                            Protocol.build(
                                    Protocol.SEARCH_MESSAGES,
                                    keyword
                            )
                    );
                }

                // =================================================
                // MESSAGE
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
                    "Chi tiết: "
                    + e.getMessage()
            );
        }
    }


    // ==========================================================
    // AUTH MENU
    // ==========================================================

    private static boolean authenticationMenu(
            Scanner scanner,
            BufferedReader reader,
            PrintWriter writer)
            throws IOException {

        while (true) {

            System.out.println();
            System.out.println(
                    "===================================="
            );
            System.out.println(
                    "          TÀI KHOẢN"
            );
            System.out.println(
                    "===================================="
            );
            System.out.println(
                    "1. Đăng ký"
            );
            System.out.println(
                    "2. Đăng nhập"
            );
            System.out.println(
                    "0. Thoát"
            );
            System.out.println(
                    "===================================="
            );


            System.out.print(
                    "Chọn: "
            );


            String choice =
                    scanner.nextLine().trim();


            // =================================================
            // REGISTER
            // =================================================

            if (
                    choice.equals("1")
            ) {

                register(
                        scanner,
                        reader,
                        writer
                );
            }


            // =================================================
            // LOGIN
            // =================================================

            else if (
                    choice.equals("2")
            ) {

                boolean success =
                        login(
                                scanner,
                                reader,
                                writer
                        );


                if (success) {

                    return true;
                }
            }


            // =================================================
            // EXIT
            // =================================================

            else if (
                    choice.equals("0")
            ) {

                return false;
            }


            else {

                System.out.println(
                        "Lựa chọn không hợp lệ."
                );
            }
        }
    }


    // ==========================================================
    // REGISTER
    // ==========================================================

    private static void register(
            Scanner scanner,
            BufferedReader reader,
            PrintWriter writer)
            throws IOException {

        System.out.println();
        System.out.println(
                "========== ĐĂNG KÝ =========="
        );


        System.out.print(
                "Username: "
        );


        String username =
                scanner.nextLine().trim();


        System.out.print(
                "Tên hiển thị: "
        );


        String displayName =
                scanner.nextLine().trim();


        System.out.print(
                "Password: "
        );


        String password =
                scanner.nextLine();


        System.out.print(
                "Nhập lại Password: "
        );


        String confirm =
                scanner.nextLine();


        if (
                !password.equals(confirm)
        ) {

            System.out.println(
                    "[CLIENT] Hai password không giống nhau."
            );

            return;
        }


        // Encode password bằng Base64
        // để không phá Protocol bởi ký tự |.
        String encodedPassword =
                Base64.getEncoder()
                        .encodeToString(
                                password.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );


        writer.println(
                Protocol.build(
                        Protocol.REGISTER,
                        username,
                        displayName,
                        encodedPassword
                )
        );


        // Đợi phản hồi REGISTER.
        while (true) {

            String response =
                    reader.readLine();


            if (response == null) {
                return;
            }


            processServerMessage(
                    response
            );


            String command =
                    response.split(
                            "\\|",
                            2
                    )[0];


            if (
                    command.equals(
                            Protocol.REGISTER_SUCCESS
                    )
                    ||
                    command.equals(
                            Protocol.REGISTER_FAILED
                    )
                    ||
                    command.equals(
                            Protocol.ERROR
                    )
            ) {

                break;
            }
        }
    }


    // ==========================================================
    // LOGIN
    // ==========================================================

    private static boolean login(
            Scanner scanner,
            BufferedReader reader,
            PrintWriter writer)
            throws IOException {

        System.out.println();
        System.out.println(
                "========== ĐĂNG NHẬP =========="
        );


        System.out.print(
                "Username: "
        );


        String username =
                scanner.nextLine().trim();


        System.out.print(
                "Password: "
        );


        String password =
                scanner.nextLine();


        String encodedPassword =
                Base64.getEncoder()
                        .encodeToString(
                                password.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );


        writer.println(
                Protocol.build(
                        Protocol.LOGIN,
                        username,
                        encodedPassword
                )
        );


        // =================================================
        // Đợi LOGIN_SUCCESS / LOGIN_FAILED
        // =================================================

        while (true) {

            String response =
                    reader.readLine();


            if (response == null) {

                return false;
            }


            processServerMessage(
                    response
            );


            String command =
                    response.split(
                            "\\|",
                            2
                    )[0];


            if (
                    command.equals(
                            Protocol.LOGIN_SUCCESS
                    )
            ) {

                return true;
            }


            if (
                    command.equals(
                            Protocol.LOGIN_FAILED
                    )
                    ||
                    command.equals(
                            Protocol.ERROR
                    )
            ) {

                return false;
            }
        }
    }


    // ==========================================================
    // HELP
    // ==========================================================

    private static void printChatHelp() {

        System.out.println();
        System.out.println(
                "===================================="
        );
        System.out.println(
                "            CHAT COMMAND"
        );
        System.out.println(
                "===================================="
        );

        System.out.println(
                "/rooms  - Xem danh sách phòng"
        );

        System.out.println(
                "/users  - Xem thành viên phòng"
        );

        System.out.println(
                "/join Học nhóm"
        );

        System.out.println(
                "/join Lập trình mạng"
        );

        System.out.println(
                "/join Giải trí"
        );

        System.out.println(
                "/quit   - Đăng xuất"
        );
        
        System.out.println(
                "/search <tu khoa> - Tim kiem tin nhan trong phong"
        );

        System.out.println(
                "===================================="
        );
    }


    // ==========================================================
    // PROCESS SERVER MESSAGE
    // ==========================================================

    private static void processServerMessage(
            String response) {

        if (
                response == null
                || response.isEmpty()
        ) {

            return;
        }

        /*
         * Xác định command trước để có thể dùng
         * số phần split khác nhau cho từng loại message.
         */
        String command = response.contains("|")
                ? response.substring(0, response.indexOf("|"))
                : response;

        // =====================================================
        // LỊCH SỬ TIN NHẮN
        //
        // Format:
        // HISTORY|roomName|messageId|sentAt|displayName|content
        //
        // Split tối đa 6 phần để nội dung vẫn giữ được
        // dấu | nếu người dùng gõ dấu này.
        // =====================================================
        if (command.equals(Protocol.HISTORY)) {

            String[] historyParts =
                    response.split("\\|", 6);

            if (historyParts.length >= 6) {

                String roomName = historyParts[1];
                String messageId = historyParts[2];
                String sentAt = historyParts[3];
                String displayName = historyParts[4];
                String content = historyParts[5];

                System.out.println(
                        "[LỊCH SỬ - "
                        + roomName
                        + "] "
                        + displayName
                        + " ("
                        + sentAt
                        + ", ID "
                        + messageId
                        + "): "
                        + content
                );
            }

            return;
        }

        // =====================================================
        // BÁO ĐÃ TẢI XONG LỊCH SỬ
        // =====================================================
        if (command.equals(Protocol.HISTORY_END)) {

            String[] endParts =
                    response.split("\\|", 2);

            if (endParts.length >= 2) {

                System.out.println(
                        "[LỊCH SỬ] Đã tải xong phòng "
                        + endParts[1]
                );
            }

            return;
        }
        
        // =====================================================
        // HIỂN THỊ MỘT KẾT QUẢ TÌM KIẾM
        //
        // Format:
        // SEARCH_RESULT|room|id|sentAt|displayName|content
        //
        // Split tối đa 6 phần để nội dung vẫn có thể chứa dấu |.
        // =====================================================
        if (command.equals(Protocol.SEARCH_RESULT)) {

            String[] resultParts = response.split("\\|", 6);

            if (resultParts.length >= 6) {

                String roomName = resultParts[1];
                String messageId = resultParts[2];
                String sentAt = resultParts[3];
                String displayName = resultParts[4];
                String content = resultParts[5];

                System.out.println(
                        "[TÌM KIẾM - " + roomName + "] "
                        + displayName
                        + " (" + sentAt
                        + ", ID " + messageId + "): "
                        + content
                );
            }

            return;
        }

        // =====================================================
        // KẾT THÚC TÌM KIẾM
        //
        // Format:
        // SEARCH_END|roomName|numberOfResults
        // =====================================================
        if (command.equals(Protocol.SEARCH_END)) {

            String[] endParts = response.split("\\|", 3);

            if (endParts.length >= 3) {

                String roomName = endParts[1];
                String total = endParts[2];

                System.out.println(
                        "[TÌM KIẾM] Hoàn tất tại phòng "
                        + roomName
                        + ". Số kết quả: "
                        + total
                );
            }

            return;
        }

        // =====================================================
        // Các loại message cũ tiếp tục được xử lý bên dưới.
        // =====================================================
        String[] parts =
                response.split(
                        "\\|",
                        5
                );


        // command đã được lấy ở trên, gán lại để đồng bộ với switch bên dưới
        command =
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


            case Protocol.REGISTER_SUCCESS:

                if (parts.length >= 2) {

                    System.out.println(
                            "[REGISTER] "
                            + parts[1]
                    );
                }

                break;


            case Protocol.REGISTER_FAILED:

                if (parts.length >= 2) {

                    System.out.println(
                            "[REGISTER FAILED] "
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

                break;


            case Protocol.LOGIN_FAILED:

                if (parts.length >= 2) {

                    System.out.println(
                            "[LOGIN FAILED] "
                            + parts[1]
                    );
                }

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

                // Format mới: CHAT|room|messageId|username|displayName|content
                if (parts.length >= 6) {

                    String room =
                            parts[1];

                    // parts[2] = messageId (bỏ qua trong console client)

                    String username =
                            parts[3];

                    String displayName =
                            parts[4];

                    String message =
                            parts[5];


                    System.out.println(
                            "["
                            + room
                            + "] "
                            + displayName
                            + " ("
                            + username
                            + "): "
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