package com.grocery.dto;

public class EmailSendResponse {

    private boolean sent;
    private String message;

    public EmailSendResponse(boolean sent, String message) {
        this.sent = sent;
        this.message = message;
    }

    public boolean isSent() {
        return sent;
    }

    public String getMessage() {
        return message;
    }
}
