package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import java.sql.*;

public class AdminOverviewController {
    @FXML private LineChart<String, Number> salesChart;
    @FXML private Label revenueLabel, soldLabel, customersLabel, growthLabel;

    @FXML
    public void initialize() {
        updateStatsFromDB();
        loadSalesChart();
    }

    private void updateStatsFromDB() {
        try (Connection conn = DBConnector.getConnection()) {

            // 1. حساب إجمالي الإيرادات (Total Revenue)
            String revenueSQL = "SELECT SUM(total_amount) FROM sales";
            ResultSet rs1 = conn.createStatement().executeQuery(revenueSQL);
            if (rs1.next()) {
                double totalRevenue = rs1.getDouble(1);
                revenueLabel.setText("$" + String.format("%,.2f", totalRevenue));
            }

            // 2. حساب عدد المنتجات المباعة (Products Sold)
            String soldSQL = "SELECT SUM(quantity) FROM sales_items";
            ResultSet rs2 = conn.createStatement().executeQuery(soldSQL);
            if (rs2.next()) {
                int totalSold = rs2.getInt(1);
                soldLabel.setText(String.format("%,d", totalSold));
            }

            // 3. حساب عدد العمليات (Active Customers)
            String countSQL = "SELECT COUNT(id) FROM sales";
            ResultSet rs3 = conn.createStatement().executeQuery(countSQL);
            if (rs3.next()) {
                int totalOrders = rs3.getInt(1);
                customersLabel.setText(String.format("%,d", totalOrders));
            }

            // 4. حساب نسبة النمو (Growth Rate) - مقارنة الشهر الحالي بالشهر الماضي
            String growthSQL = "SELECT " +
                    "(SELECT SUM(total_amount) FROM sales WHERE MONTH(date) = MONTH(CURRENT_DATE) AND YEAR(date) = YEAR(CURRENT_DATE)) as current_month, " +
                    "(SELECT SUM(total_amount) FROM sales WHERE MONTH(date) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) AND YEAR(date) = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)) as last_month";

            ResultSet rs4 = conn.createStatement().executeQuery(growthSQL);
            if (rs4.next()) {
                double currentMonth = rs4.getDouble("current_month");
                double lastMonth = rs4.getDouble("last_month");

                double growthPercent = 0.0;
                if (lastMonth > 0) {
                    growthPercent = ((currentMonth - lastMonth) / lastMonth) * 100;
                } else if (currentMonth > 0) {
                    growthPercent = 100.0; // إذا كان هناك مبيعات الآن ولم يكن هناك الشهر الماضي
                }

                // تحديث قيم الـ Labels الخاصة بالنمو
                growthLabel.setText(String.format("%.1f%%", growthPercent));

                // تحديث اللون بناءً على النتيجة (أخضر للموجب، أحمر للسالب)
                if (growthPercent >= 0) {
                    growthLabel.setStyle("-fx-text-fill: #119642; -fx-font-weight: bold; -fx-font-size: 24px;");
                } else {
                    growthLabel.setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold; -fx-font-size: 24px;");
                }
            }

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
        }
    }
    private void loadSalesChart() {
        salesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue 2025");

        // استعلام يجلب المبيعات مجمعة حسب الشهر (بناءً على عمود date)
        String chartSQL = "SELECT DATE_FORMAT(date, '%b') as month, SUM(total_amount) as total " +
                "FROM sales GROUP BY MONTH(date) ORDER BY MONTH(date) ASC";

        try (Connection conn = DBConnector.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(chartSQL)) {

            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("month"), rs.getDouble("total")));
            }
            salesChart.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}