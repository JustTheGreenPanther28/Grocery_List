package com.grocery.dto;

import com.grocery.model.GroceryItem;
import java.time.LocalDate;

public class GroceryItemResponse {

    private Long id;
    private LocalDate date;
    private String name;
    private String qty;
    private boolean checked;

    public GroceryItemResponse(GroceryItem item) {
        this.id = item.getId();
        this.date = item.getItemDate();
        this.name = item.getName();
        this.qty = item.getQty();
        this.checked = item.isChecked();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getQty() {
        return qty;
    }

    public boolean isChecked() {
        return checked;
    }
}
