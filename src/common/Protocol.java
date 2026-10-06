/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package common;

/**
 *
 * @author TUAN HUY
 */
public class Protocol {
   // =====================================================
    // CLIENT → SERVER
    // =====================================================

    public static final String LOGIN =
            "LOGIN";

    public static final String JOIN_ROOM =
            "JOIN_ROOM";

    public static final String GET_ROOMS =
            "GET_ROOMS";

    public static final String GET_USERS =
            "GET_USERS";

    public static final String SEND_MESSAGE =
            "SEND_MESSAGE";

    public static final String LOGOUT =
            "LOGOUT";


    // =====================================================
    // SERVER → CLIENT
    // =====================================================

    public static final String LOGIN_SUCCESS =
            "LOGIN_SUCCESS";

    public static final String LOGIN_FAILED =
            "LOGIN_FAILED";

    public static final String ROOM_LIST =
            "ROOM_LIST";

    public static final String USER_LIST =
            "USER_LIST";

    public static final String CHAT =
            "CHAT";

    public static final String SYSTEM =
            "SYSTEM";

    public static final String ERROR =
            "ERROR";


    private Protocol() {
    }


    /**
     * Tạo một message theo format:
     *
     * COMMAND|DATA1|DATA2|...
     */
    public static String build(
            String command,
            String... data) {

        StringBuilder message =
                new StringBuilder(command);


        for (String item : data) {

            /*
             * readLine() được sử dụng để nhận dữ liệu.
             *
             * Vì vậy không cho phép message chứa
             * ký tự xuống dòng.
             */
            String safeItem =
                    item
                            .replace("\r", " ")
                            .replace("\n", " ");


            message
                    .append("|")
                    .append(safeItem);
        }


        return message.toString();
    } 
}
