/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;
import model.User;
/**
 *
 * @author ASUS
 */
public class UserDAOTest {
     public static void main(String[] args) {

        try {

            DatabaseInitializer.initialize();


            UserDAO userDAO =
                    new UserDAO();


            String username =
                    "test_user";


            // Nếu chạy lại nhiều lần thì tài khoản
            // đã tồn tại, không tạo lại.
            if (
                    !userDAO.usernameExists(
                            username
                    )
            ) {

                boolean created =
                        userDAO.createUser(
                                username,
                                "Người dùng kiểm tra",
                                "XinChao123!"
                        );


                System.out.println(
                        "Tạo tài khoản: "
                        + created
                );

            } else {

                System.out.println(
                        "Tài khoản đã tồn tại."
                );
            }


            User user =
                    userDAO.findByUsername(
                            username
                    );


            if (user != null) {

                System.out.println(
                        "ID: "
                        + user.getId()
                );


                System.out.println(
                        "Username: "
                        + user.getUsername()
                );


                System.out.println(
                        "Display name: "
                        + user.getDisplayName()
                );


                System.out.println(
                        "Role: "
                        + user.getRole()
                );


                System.out.println(
                        "Hash: "
                        + user.getPasswordHash()
                );


                System.out.println(
                        "Salt: "
                        + user.getPasswordSalt()
                );
            }


        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
