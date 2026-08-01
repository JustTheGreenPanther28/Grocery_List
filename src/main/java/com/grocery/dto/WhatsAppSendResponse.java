package com.grocery.dto;

public class WhatsAppSendResponse {

    private boolean sentDirectly; // true if sent via WhatsApp Cloud API, false if only a link was generated
    private String message;
    private String waLink; // fallback link the frontend can open when sentDirectly is false

    public WhatsAppSendResponse(boolean sentDirectly, String message, String waLink) {
        this.sentDirectly = sentDirectly;
        this.message = message;
        this.waLink = waLink;
    }

    public boolean isSentDirectly() {
        return sentDirectly;
    }

    public String getMessage() {
        return message;
    }

    public String getWaLink() {
        return waLink;
    }
}
