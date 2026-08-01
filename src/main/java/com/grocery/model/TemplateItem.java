package com.grocery.model;

import jakarta.persistence.*;

// A "usual item" the user can tap to instantly add to any date's list,
// instead of retyping milk/bread/eggs every week.
@Entity
@Table(name = "template_items")
public class TemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String name;

    private String qty;

    public TemplateItem() {
    }

    public TemplateItem(String username, String name, String qty) {
        this.username = username;
        this.name = name;
        this.qty = qty;
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
