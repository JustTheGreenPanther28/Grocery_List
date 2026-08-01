package com.grocery.dto;

import com.grocery.model.ScheduledSend;

public class ScheduledSendResponse {

    private boolean enabled;
    private String dayOfWeek;
    private int hour;
    private int minute;
    private String toNumber;
    private String channel;

    public ScheduledSendResponse(ScheduledSend s) {
        this.enabled = s.isEnabled();
        this.dayOfWeek = s.getDayOfWeek() != null ? s.getDayOfWeek().name() : null;
        this.hour = s.getHour();
        this.minute = s.getMinute();
        this.toNumber = s.getToNumber();
        this.channel = s.getChannel();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String getToNumber() {
        return toNumber;
    }

    public String getChannel() {
        return channel;
    }
}
