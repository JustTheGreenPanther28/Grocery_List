package com.grocery.service;

import com.grocery.model.GroceryItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

//Again again writing same method for printing Grocery item < Do it in one class 
public final class GroceryListMessageFormatter {

    private GroceryListMessageFormatter() {
    }

    public static String build(LocalDate date, List<GroceryItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Grocery List - ").append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");

        int i = 1;
        for (GroceryItem item : items) {
            sb.append(i++).append(". ").append(item.getName());
            if (item.getQty() != null && !item.getQty().isBlank()) {
                sb.append(" - ").append(item.getQty());
            }
            if (item.isChecked()) {
                sb.append(" [checked]");
            }
            sb.append("\n");
        }
        sb.append("\nSent from Grocery List app");
        return sb.toString();
    }
}
