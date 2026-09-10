package com.example.demo1.database;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnector {
    private static final Logger LOGGER = Logger.getLogger(DBConnector.class.getName());

    // معلومات الاتصال
    private static final String URL = "jdbc:mysql://localhost:3306/supermarket_db";
    private static final String USERNAME = "root";  // غير حسب إعداداتك
    private static final String PASSWORD = "";      // ضع الباسورد

    private static Connection connection = null;

    // الحصول على اتصال - محسنة
    public static Connection getConnection() {
        try {
            // إذا كان الاتصال مغلقاً أو غير موجود، أنشئ اتصالاً جديداً
            if (connection == null || connection.isClosed()) {
                // تحميل Driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // إنشاء الاتصال مع إعدادات إضافية
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

                // تعيين إعدادات إضافية للاتصال
                connection.setAutoCommit(true);
                LOGGER.info("✅ Database connected successfully!");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "❌ MySQL Driver not found!", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Database connection failed!", e);
        }
        return connection;
    }

    // إغلاق الاتصال
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOGGER.info("Database connection closed.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing connection", e);
            } finally {
                connection = null;
            }
        }
    }

    // اختبار الاتصال
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // === طرق جديدة مطلوبة للـ Dashboard ===

    // 1. الحصول على إجمالي الإيرادات
    public static String getTotalRevenue() {
        String query = "SELECT CONCAT('$', FORMAT(SUM(total_amount), 2)) AS Total_Revenue FROM sales";
        return executeScalarQuery(query, "$0.00");
    }

    // 2. الحصول على إجمالي المنتجات المباعة
    public static String getProductsSold() {
        String query = "SELECT FORMAT(SUM(quantity), 0) AS Products_Sold FROM sales_items";
        return executeScalarQuery(query, "0");
    }

    // 3. الحصول على عدد العملاء النشطين (عدد الفواتير)
    public static String getActiveCustomers() {
        String query = "SELECT FORMAT(COUNT(DISTINCT id), 0) AS Active_Customers FROM sales";
        return executeScalarQuery(query, "0");
    }

    // 4. الحصول على معدل النمو
    public static String getGrowthRate() {
        String query = """
            WITH monthly_revenue AS (
                SELECT 
                    YEAR(date) AS year,
                    MONTH(date) AS month,
                    SUM(total_amount) AS revenue
                FROM sales
                GROUP BY YEAR(date), MONTH(date)
                ORDER BY year DESC, month DESC
                LIMIT 2
            )
            SELECT 
                CONCAT(
                    FORMAT(
                        ((MAX(CASE WHEN row_num = 1 THEN revenue END) - 
                          MAX(CASE WHEN row_num = 2 THEN revenue END)) / 
                         MAX(CASE WHEN row_num = 2 THEN revenue END)) * 100, 1
                    ), '%'
                ) AS Growth_Rate
            FROM (
                SELECT 
                    revenue,
                    ROW_NUMBER() OVER (ORDER BY year DESC, month DESC) AS row_num
                FROM monthly_revenue
            ) AS t
            """;

        String result = executeScalarQuery(query, "0.0%");
        return result.equals("0.0%") ? "+0.0%" : result;
    }

    // 5. طريقة مساعدة لتنفيذ استعلامات العودة بقيمة واحدة
    static String executeScalarQuery(String query, String defaultValue) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            if (conn == null || conn.isClosed()) {
                return defaultValue;
            }

            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                String result = rs.getString(1);
                return result != null ? result : defaultValue;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Error executing query: " + query, e);
            return defaultValue;
        } finally {
            closeResources(rs, stmt);
        }

        return defaultValue;
    }

    // 6. طريقة مساعدة لإغلاق الموارد
    private static void closeResources(ResultSet rs, Statement stmt) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing resources", e);
        }
    }

    // 7. طريقة للحصول على جميع إحصائيات الـ Dashboard في مرة واحدة
    public static class DashboardStats {
        public String totalRevenue;
        public String productsSold;
        public String activeCustomers;
        public String growthRate;

        public DashboardStats() {
            this.totalRevenue = "$0.00";
            this.productsSold = "0";
            this.activeCustomers = "0";
            this.growthRate = "+0.0%";
        }
    }

    public static DashboardStats getAllDashboardStats() {
        DashboardStats stats = new DashboardStats();

        // استخدم مؤشرات ترابط منفصلة لجلب البيانات بشكل أسرع (اختياري)
        stats.totalRevenue = getTotalRevenue();
        stats.productsSold = getProductsSold();
        stats.activeCustomers = getActiveCustomers();
        stats.growthRate = getGrowthRate();

        return stats;
    }
}

