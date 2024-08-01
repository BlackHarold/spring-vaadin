package ru.suek.model;

import jakarta.validation.constraints.NotEmpty;

public class Element {
    private String id;

    @NotEmpty
    private String name;
    @NotEmpty
    private String description;
    private double price;

    public Element() {
    }

    public Element(String name, String description, double price) {
        this.name=name;
        this.description=description;
        this.price=price;
    }

    public Element(String id, String name, String description, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
