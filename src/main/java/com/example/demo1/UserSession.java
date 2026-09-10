package com.example.demo1;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String role;
    private int userId;
    private int cashierId; // هذا الرقم مهم جداً لربط المبيعات

    // Constructor خاص لمنع الإنشاء الخارجي
    private UserSession(String username, String role, int userId, int cashierId) {
        this.username = username;
        this.role = role;
        this.userId = userId;
        this.cashierId = cashierId;
    }

    // دالة إنشاء الجلسة عند تسجيل الدخول
    public static UserSession getInstance(String username, String role, int userId, int cashierId) {
        if (instance == null) {
            instance = new UserSession(username, role, userId, cashierId);
        }
        return instance;
    }

    // دالة جلب الجلسة الحالية في أي مكان بالكود
    public static UserSession getInstance() {
        return instance;
    }

    // تنظيف الجلسة عند تسجيل الخروج (Logout)
    public void cleanUserSession() {
        instance = null;
    }

    // Getters
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public int getUserId() { return userId; }
    public int getCashierId() { return cashierId; }
}