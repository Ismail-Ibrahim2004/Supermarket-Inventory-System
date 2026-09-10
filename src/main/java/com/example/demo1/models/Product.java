package com.example.demo1.models;

import javafx.beans.property.*;

public class Product {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty category;
    private final IntegerProperty quantity;
    private final DoubleProperty buyingPrice;
    private final DoubleProperty sellingPrice;
    private final StringProperty supplier; // تغيير النوع لاستقبال الاسم

    public Product(String id, String name, String category, int quantity, double buyingPrice, double sellingPrice, String supplier) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.buyingPrice = new SimpleDoubleProperty(buyingPrice);
        this.sellingPrice = new SimpleDoubleProperty(sellingPrice);
        this.supplier = new SimpleStringProperty(supplier);
    }

    // Getters and Property methods
    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public int getQuantity() { return quantity.get(); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getBuyingPrice() { return buyingPrice.get(); }
    public DoubleProperty buyingPriceProperty() { return buyingPrice; }

    public double getSellingPrice() { return sellingPrice.get(); }
    public DoubleProperty sellingPriceProperty() { return sellingPrice; }

    public String getSupplier() { return supplier.get(); }
    public StringProperty supplierProperty() { return supplier; }
}