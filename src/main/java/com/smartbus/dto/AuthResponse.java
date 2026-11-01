package com.smartbus.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String role;
    private String passengerId;
    private String redirectUrl;

    public AuthResponse() {}
    public AuthResponse(boolean success, String message, String username, String role, String passengerId, String redirectUrl) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
        this.passengerId = passengerId;
        this.redirectUrl = redirectUrl;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
}
