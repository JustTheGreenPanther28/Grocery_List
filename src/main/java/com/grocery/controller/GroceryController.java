package com.grocery.controller;

import com.grocery.dto.GroceryItemRequest;
import com.grocery.dto.GroceryItemResponse;
import com.grocery.dto.GroceryItemUpdateRequest;
import com.grocery.model.GroceryItem;
import com.grocery.service.GroceryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groceries")
public class GroceryController {

    private final GroceryService groceryService;

    public GroceryController(GroceryService groceryService) {
        this.groceryService = groceryService;
    }

    @GetMapping
    public List<GroceryItemResponse> getItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = authentication.getName();
        return groceryService.getItems(username, date).stream()
                .map(GroceryItemResponse::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<GroceryItemResponse> addItem(
            @Valid @RequestBody GroceryItemRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        GroceryItem saved = groceryService.addItem(username, request);
        return ResponseEntity.ok(new GroceryItemResponse(saved));
    }

    @PutMapping("/{id}")
    public GroceryItemResponse updateItem(
            @PathVariable Long id,
            @RequestBody GroceryItemUpdateRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        GroceryItem updated = groceryService.updateItem(username, id, request);
        return new GroceryItemResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        groceryService.deleteItem(username, id);
        return ResponseEntity.noContent().build();
    }
}
