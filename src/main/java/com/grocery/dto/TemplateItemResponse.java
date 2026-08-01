package com.grocery.dto;

import com.grocery.model.TemplateItem;

public class TemplateItemResponse {

    private Long id;
    private String name;
    private String qty;

    public TemplateItemResponse(TemplateItem item) {
        this.id = item.getId();
        this.name = item.getName();
        this.qty = item.getQty();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getQty() {
        return qty;
    }
}
