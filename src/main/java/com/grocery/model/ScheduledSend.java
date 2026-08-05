package com.grocery.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

// One row per user: "send my list automatically every <dayOfWeek> at
// <hour>:<minute> to <toNumber>". Checked once a minute by ScheduledSendService.
@Entity
@Table(name = "scheduled_sends")
public class ScheduledSend {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private boolean enabled = false;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DayOfWeek dayOfWeek;

	@Column(nullable = false)
	private int hour; // 0-23, server local time

	@Column(nullable = false)
	private int minute; // 0-59

	@Column(nullable = false)
	private String toNumber;

	@Column(nullable = false)
	private String channel = "WHATSAPP"; // "WHATSAPP" or "EMAIL" - toNumber holds a phone number or email address
											// accordingly
	@NotNull
	private LocalDateTime groceryDateTimeStamp;

	public ScheduledSend() {
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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public int getMinute() {
		return minute;
	}

	public void setMinute(int minute) {
		this.minute = minute;
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

	public LocalDateTime getGroceryDateTimeStamp() {
		return groceryDateTimeStamp;
	}

	public void setGroceryDateTimeStamp(LocalDateTime groceryDateTimeStamp) {
		this.groceryDateTimeStamp = groceryDateTimeStamp;
	}
}
