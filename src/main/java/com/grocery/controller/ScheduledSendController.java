package com.grocery.controller;

import com.grocery.dto.ScheduledSendRequest;
import com.grocery.dto.ScheduledSendResponse;
import com.grocery.model.ScheduledSend;
import com.grocery.service.ScheduledSendService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whatsapp/schedule")
public class ScheduledSendController {

    private final ScheduledSendService scheduledSendService;

    public ScheduledSendController(ScheduledSendService scheduledSendService) {
        this.scheduledSendService = scheduledSendService;
    }

    @GetMapping
    public ResponseEntity<ScheduledSendResponse> get(Authentication authentication) {
        return scheduledSendService.get(authentication.getName())
                .map(s -> ResponseEntity.ok(new ScheduledSendResponse(s)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public ScheduledSendResponse save(@Valid @RequestBody ScheduledSendRequest request, Authentication authentication) {
        ScheduledSend saved = scheduledSendService.save(authentication.getName(), request);
        return new ScheduledSendResponse(saved);
    }
}
