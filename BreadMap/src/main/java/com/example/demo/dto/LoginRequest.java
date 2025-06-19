package com.example.demo.dto;

public class LoginRequest {
    private String address;  // ← メールアドレス
    private String password; // ← パスワード

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
