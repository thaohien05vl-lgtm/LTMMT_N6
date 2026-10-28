/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 * User:
 *
 * Đại diện cho một tài khoản trong hệ thống.
 *
 * Class này chỉ chứa dữ liệu người dùng.
 * Không xử lý SQL.
 */
public class User {
    private int id;

    private String username;

    private String displayName;

    private String passwordHash;

    private String passwordSalt;

    private int passwordIterations;

    private String role;

    private String createdAt;


    /**
     * Constructor đầy đủ.
     *
     * Dùng khi đọc User từ Database.
     */
    public User(
            int id,
            String username,
            String displayName,
            String passwordHash,
            String passwordSalt,
            int passwordIterations,
            String role,
            String createdAt) {

        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.passwordIterations = passwordIterations;
        this.role = role;
        this.createdAt = createdAt;
    }


    public int getId() {
        return id;
    }


    public String getUsername() {
        return username;
    }


    public String getDisplayName() {
        return displayName;
    }


    public String getPasswordHash() {
        return passwordHash;
    }


    public String getPasswordSalt() {
        return passwordSalt;
    }


    public int getPasswordIterations() {
        return passwordIterations;
    }


    public String getRole() {
        return role;
    }


    public String getCreatedAt() {
        return createdAt;
    } 
}
