package com.grocery.service;

import com.grocery.dto.GroceryItemRequest;
import com.grocery.dto.GroceryItemUpdateRequest;
import com.grocery.exception.ResourceNotFoundException;
import com.grocery.model.GroceryItem;
import com.grocery.repository.GroceryItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GroceryService {

    private final GroceryItemRepository repository;

    public GroceryService(GroceryItemRepository repository) {
        this.repository = repository;
    }

    public List<GroceryItem> getItems(String username, LocalDate date) {
        return repository.findByUsernameAndItemDateOrderByIdAsc(username, date);
    }

    public GroceryItem addItem(String username, GroceryItemRequest request) {
        GroceryItem item = new GroceryItem(username, request.getDate(), request.getName(), request.getQty());
        return repository.save(item);
    }

    // Same as addItem but takes raw fields directly - used by TemplateService
    // when copying a template item onto a date's list without a full DTO.
    public GroceryItem addItemRaw(String username, LocalDate date, String name, String qty) {
        GroceryItem item = new GroceryItem(username, date, name, qty);
        return repository.save(item);
    }

    public GroceryItem updateItem(String username, Long id, GroceryItemUpdateRequest request) {
        GroceryItem item = repository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));

        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getQty() != null) {
            item.setQty(request.getQty());
        }
        if (request.getChecked() != null) {
            item.setChecked(request.getChecked());
        }
        return repository.save(item);
    }

    public void deleteItem(String username, Long id) {
        GroceryItem item = repository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
        repository.delete(item);
    }
}
