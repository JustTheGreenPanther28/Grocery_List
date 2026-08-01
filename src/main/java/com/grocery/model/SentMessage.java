package com.grocery.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// A log row for every WhatsApp send - manual taps and scheduler-triggered
// ones alike - so users can look back at what was sent and when.
@Entity
@Table(name = "sent_messages")
public class SentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDate itemDate;

    @Column(nullable = false)
    private String toNumber;

    @Column(nullable = false)
    private String channel = "WHATSAPP"; // "WHATSAPP" or "EMAIL"

    @Lob
    @Column(nullable = false)
    private String messageText;

    @Column(nullable = false)
    private boolean sentDirectly;

    @Column(nullable = false)
    private boolean automatic; // true = fired by the schedule, false = manual tap

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public SentMessage() {
    }

    public SentMessage(String username, LocalDate itemDate, String toNumber, String channel, String messageText,
                        boolean sentDirectly, boolean automatic) {
        this.username = username;
        this.itemDate = itemDate;
        this.toNumber = toNumber;
        this.channel = channel;
        this.messageText = messageText;
        this.sentDirectly = sentDirectly;
        this.automatic = automatic;
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getItemDate() {
        return itemDate;
    }

    public void setItemDate(LocalDate itemDate) {
        this.itemDate = itemDate;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public boolean isSentDirectly() {
        return sentDirectly;
    }

    public void setSentDirectly(boolean sentDirectly) {
        this.sentDirectly = sentDirectly;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
