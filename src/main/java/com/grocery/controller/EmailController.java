package com.grocery.controller;

import com.grocery.dto.EmailSendRequest;
import com.grocery.dto.EmailSendResponse;
import com.grocery.model.GroceryItem;
import com.grocery.service.EmailService;
import com.grocery.service.GroceryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final GroceryService groceryService;
    private final EmailService emailService;

    public EmailController(GroceryService groceryService, EmailService emailService) {
        this.groceryService = groceryService;
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public EmailSendResponse send(@Valid @RequestBody EmailSendRequest request, Authentication authentication) {

		String username = authentication.getName();
        List<GroceryItem> items = groceryService.getItems(username, request.getDate());
        return emailService.sendGroceryList(username, request.getToEmail(), request.getDate(), items, false);
    }
}
