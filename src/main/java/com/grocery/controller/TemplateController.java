package com.grocery.controller;

import com.grocery.dto.GroceryItemResponse;
import com.grocery.dto.TemplateItemRequest;
import com.grocery.dto.TemplateItemResponse;
import com.grocery.model.GroceryItem;
import com.grocery.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateItemResponse> getTemplates(Authentication authentication) {
        return templateService.getTemplates(authentication.getName()).stream()
                .map(TemplateItemResponse::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<TemplateItemResponse> addTemplate(
            @Valid @RequestBody TemplateItemRequest request, Authentication authentication) {
        var saved = templateService.addTemplate(authentication.getName(), request);
        return ResponseEntity.ok(new TemplateItemResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id, Authentication authentication) {
        templateService.deleteTemplate(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // Copies a single template item onto the given date's grocery list.
    @PostMapping("/{id}/add")
    public GroceryItemResponse addToList(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        GroceryItem created = templateService.addTemplateToList(authentication.getName(), id, date);
        return new GroceryItemResponse(created);
    }

    // Copies every template item onto the given date's list at once ("repeat my usual list").
    @PostMapping("/add-all")
    public List<GroceryItemResponse> addAllToList(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        return templateService.addAllTemplatesToList(authentication.getName(), date).stream()
                .map(GroceryItemResponse::new)
                .collect(Collectors.toList());
    }
}
