/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client.ui;

import common.EncodingUtil;
import common.Protocol;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * ClientConnectionTest:
 *
 * Công cụ kiểm tra ClientConnection trước khi
 * tích hợp vào giao diện Swing chính thức.
 *
 * Đây chỉ là chương trình kiểm thử.
 * Sau này LoginFrame/RegisterFrame/MainFrame
 * sẽ sử dụng ClientConnection trực tiếp.
 */
public class ClientConnectionTest {

    public static void main(String[] args) {

        EncodingUtil.setupUTF8();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "       KIEM THU CLIENT CONNECTION"
        );

        System.out.println(
                "=========================================="
        );

        // Tạo ClientConnection.
        ClientConnection connection =
                new ClientConnection(
                        "127.0.0.1",
                        5000,
                        new ClientConnection.Listener() {

                            @Override
                            public void onConnected() {

                                System.out.println(
                                        "[TEST] Da ket noi Server."
                                );
                            }

                            @Override
                            public void onMessage(
                                    String message) {

                                System.out.println(
                                        "[SERVER] " + message
                                );
                            }

                            @Override
                            public void onDisconnected() {

                                System.out.println(
                                        "[TEST] Da ngat ket noi."
                                );
                            }

                            @Override
                            public void onError(
                                    String message) {

                                System.out.println(
                                        "[TEST ERROR] " + message
                                );
                            }
                        }
                );

        // Kết nối chạy bất đồng bộ.
        connection.connect();

        System.out.println();
        System.out.println(
                "Da bat dau ket noi."
        );

        System.out.println(
                "Co the nhap mot dong Protocol de gui Server."
        );

        System.out.println(
                "Nhap /quit de dong Client."
        );

        // Đọc bàn phím trong main thread.
        // Không dùng Scanner để đọc dữ liệu TCP.
        try (Scanner scanner =
                     new Scanner(
                             System.in,
                             StandardCharsets.UTF_8
                     )) {

            while (scanner.hasNextLine()) {

                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("/quit")) {

                    connection.send(
                            Protocol.LOGOUT
                    );

                    connection.close();

                    break;
                }

                boolean sent =
                        connection.send(input);

                if (!sent) {

                    System.out.println(
                            "[TEST] Gui that bai."
                    );
                }
            }

        } finally {

            connection.close();
        }
    }
}