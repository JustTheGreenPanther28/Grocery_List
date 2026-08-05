package com.grocery.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ScheduledSendRequest {

	@NotNull
	private Boolean enabled;

	// MONDAY, TUESDAY ...
	@NotBlank
	private String dayOfWeek;

	@Min(0)
	@Max(23)
	private int hour;

	@Min(0)
	@Max(59)
	private int minute;

	private String toNumber;
	
	@NotNull
	private LocalDateTime groceryDateTimeStamp;

	// "WHATSAPP" or "EMAIL" - defaults to WHATSAPP if omitted for backward
	private String channel = "WHATSAPP";

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
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

	public LocalDateTime getGroceryTimeStamp() {
		return groceryDateTimeStamp;
	}

	public void setGroceryDateTimeStamp(LocalDateTime groceryDateTimeStamp) {
		this.groceryDateTimeStamp = groceryDateTimeStamp;
	}
}
