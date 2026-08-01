package com.grocery.dto;

public class LoginResponse {

    private String token;
    private String username;
    private long expiresInMs;

    public LoginResponse(String token, String username, long expiresInMs) {
        this.token = token;
        this.username = username;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }
}
