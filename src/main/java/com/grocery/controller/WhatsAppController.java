package com.grocery.controller;

import com.grocery.dto.SentMessageResponse;
import com.grocery.dto.WhatsAppSendRequest;
import com.grocery.dto.WhatsAppSendResponse;
import com.grocery.model.GroceryItem;
import com.grocery.repository.SentMessageRepository;
import com.grocery.service.GroceryService;
import com.grocery.service.WhatsAppService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final GroceryService groceryService;
    private final WhatsAppService whatsAppService;
    private final SentMessageRepository sentMessageRepository;

    public WhatsAppController(GroceryService groceryService, WhatsAppService whatsAppService,
                               SentMessageRepository sentMessageRepository) {
        this.groceryService = groceryService;
        this.whatsAppService = whatsAppService;
        this.sentMessageRepository = sentMessageRepository;
    }

    @PostMapping("/send")
    public WhatsAppSendResponse send(@Valid @RequestBody WhatsAppSendRequest request, Authentication authentication) {
        String username = authentication.getName();
        List<GroceryItem> items = groceryService.getItems(username, request.getDate());
        return whatsAppService.sendGroceryList(username, request.getToNumber(), request.getDate(), items, false);
    }

    // History of every send (manual + scheduled) for the logged-in user, most recent first.
    // Optional ?date= filters to sends for that one grocery-list date.
    @GetMapping("/history")
    public List<SentMessageResponse> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String username = authentication.getName();
        List<com.grocery.model.SentMessage> messages = date != null
                ? sentMessageRepository.findByUsernameAndItemDateOrderBySentAtDesc(username, date)
                : sentMessageRepository.findByUsernameOrderBySentAtDesc(username);
        return messages.stream().map(SentMessageResponse::new).collect(Collectors.toList());
    }
}
