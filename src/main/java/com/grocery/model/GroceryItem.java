package com.grocery.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "grocery_items")
public class GroceryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // owner - matches the authenticated user's username

    @Column(nullable = false)
    private LocalDate itemDate;

    @Column(nullable = false)
    private String name;

    private String qty;

    @Column(nullable = false)
    private boolean checked = false;

    public GroceryItem() {
    }

    public GroceryItem(String username, LocalDate itemDate, String name, String qty) {
        this.username = username;
        this.itemDate = itemDate;
        this.name = name;
        this.qty = qty;
        this.checked = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getItemDate() {
        return itemDate;
    }

    public void setItemDate(LocalDate itemDate) {
        this.itemDate = itemDate;
    }

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

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
