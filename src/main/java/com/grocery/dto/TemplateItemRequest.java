package com.grocery.dto;

import jakarta.validation.constraints.NotBlank;

public class TemplateItemRequest {

    @NotBlank
    private String name;

    private String qty;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }
}
