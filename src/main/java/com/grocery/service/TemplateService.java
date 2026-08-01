package com.grocery.service;

import com.grocery.dto.TemplateItemRequest;
import com.grocery.exception.ResourceNotFoundException;
import com.grocery.model.GroceryItem;
import com.grocery.model.TemplateItem;
import com.grocery.repository.TemplateItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TemplateService {

    private final TemplateItemRepository templateRepository;
    private final GroceryService groceryService;

    public TemplateService(TemplateItemRepository templateRepository, GroceryService groceryService) {
        this.templateRepository = templateRepository;
        this.groceryService = groceryService;
    }

    public List<TemplateItem> getTemplates(String username) {
        return templateRepository.findByUsernameOrderByIdAsc(username);
    }

    public TemplateItem addTemplate(String username, TemplateItemRequest request) {
        TemplateItem item = new TemplateItem(username, request.getName(), request.getQty());
        return templateRepository.save(item);
    }

    public void deleteTemplate(String username, Long id) {
        TemplateItem item = templateRepository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        templateRepository.delete(item);
    }

    // Copies one template item onto a specific date's grocery list.
    public GroceryItem addTemplateToList(String username, Long templateId, LocalDate date) {
        TemplateItem template = templateRepository.findByIdAndUsername(templateId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        return groceryService.addItemRaw(username, date, template.getName(), template.getQty());
    }

    // Copies every template item onto a date's list in one go ("repeat my usual list").
    public List<GroceryItem> addAllTemplatesToList(String username, LocalDate date) {
        return getTemplates(username).stream()
                .map(t -> groceryService.addItemRaw(username, date, t.getName(), t.getQty()))
                .toList();
    }
}
