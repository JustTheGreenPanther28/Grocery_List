package com.grocery.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.grocery.model.GroceryItem;
import com.grocery.model.ScheduledSend;
import com.grocery.repository.ScheduledSendRepository;

// Split out of ScheduledSendService so the send itself can be @Async.
// (Spring's @Async proxy is skipped on this-> calls within the same
// class, so the async method has to live on a different bean.)
@Service
public class ScheduledSendDispatcher {

	private final ScheduledSendRepository scheduledSendRepository;
	private final WhatsAppService whatsAppService;
	private final EmailService emailService;

	public ScheduledSendDispatcher(ScheduledSendRepository scheduledSendRepository, WhatsAppService whatsAppService,
			EmailService emailService) {
		this.scheduledSendRepository = scheduledSendRepository;
		this.whatsAppService = whatsAppService;
		this.emailService = emailService;
	}

//	@Async("scheduledSendExecutor")
	public void dispatch(ScheduledSend schedule, LocalDate today, List<GroceryItem> items) {
		if (!items.isEmpty()) {
			if ("EMAIL".equals(schedule.getChannel())) {
				emailService.sendGroceryList(schedule.getUsername(), schedule.getToNumber(), today, items, true);
			} else {
				whatsAppService.sendGroceryList(schedule.getUsername(), schedule.getToNumber(), today, items, true);
			}
		}
		schedule.setEnabled(false);
		schedule.setLastTriggeredDate(today);
		scheduledSendRepository.save(schedule);
	}
}
