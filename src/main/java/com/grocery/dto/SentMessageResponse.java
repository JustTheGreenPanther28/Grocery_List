package com.grocery.dto;

import com.grocery.model.SentMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SentMessageResponse {

    private Long id;
    private LocalDate date;
    private String toNumber;
    private String channel;
    private String message;
    private boolean sentDirectly;
    private boolean automatic;
    private LocalDateTime sentAt;

    public SentMessageResponse(SentMessage m) {
        this.id = m.getId();
        this.date = m.getItemDate();
        this.toNumber = m.getToNumber();
        this.channel = m.getChannel();
        this.message = m.getMessageText();
        this.sentDirectly = m.isSentDirectly();
        this.automatic = m.isAutomatic();
        this.sentAt = m.getSentAt();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getToNumber() {
        return toNumber;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSentDirectly() {
        return sentDirectly;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
