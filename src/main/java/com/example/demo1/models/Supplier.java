package com.example.demo1.models;

public class Supplier {
    private int id;
    private String name;
    private String phone;
    private String email;
    private String itemsProvided;
    private String address;
    private String status;

    // 1. الـ Constructor الخاص بالـ ComboBox (يحتاج ID واسم فقط)
    public Supplier(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // 2. الـ Constructor الكامل الخاص بجدول الموردين
    public Supplier(int id, String name, String phone, String email, String itemsProvided, String address, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.itemsProvided = itemsProvided;
        this.address = address;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getItemsProvided() { return itemsProvided; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }

    // هذه الدالة تجعل الـ ComboBox يعرض الاسم فقط بدلاً من عنوان الذاكرة
    @Override
    public String toString() {
        return name;
    }
}