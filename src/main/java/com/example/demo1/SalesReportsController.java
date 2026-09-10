package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import java.sql.*;

public class SalesReportsController {
    @FXML private Label totalSalesLabel, bestProductLabel, unitsSoldLabel, topDayLabel;
    @FXML private BarChart<String, Number> salesBarChart;
    @FXML private CategoryAxis daysAxis;

    @FXML
    public void initialize() {
        loadSummaryStats();
        loadWeeklyChart();
    }

    private void loadSummaryStats() {
        try (Connection conn = DBConnector.getConnection()) {
            // 1. إجمالي المبيعات (قمنا بإزالة شرط الأسبوع مؤقتاً لضمان ظهور بياناتك القديمة)
            String sqlTotal = "SELECT SUM(total_amount) FROM sales";
            ResultSet rs1 = conn.createStatement().executeQuery(sqlTotal);
            if (rs1.next()) {
                totalSalesLabel.setText("$" + String.format("%,.2f", rs1.getDouble(1)));
            }

            // 2. المنتج الأكثر مبيعاً
            String sqlBest = "SELECT p.product_name, SUM(si.quantity) as total_qty " +
                    "FROM sales_items si JOIN products p ON si.product_id = p.product_id " +
                    "GROUP BY p.product_id ORDER BY total_qty DESC LIMIT 1";
            ResultSet rs2 = conn.createStatement().executeQuery(sqlBest);
            if (rs2.next()) {
                bestProductLabel.setText(rs2.getString("product_name"));
                unitsSoldLabel.setText(rs2.getInt("total_qty") + " units");
            }

            // 3. اليوم الأكثر تحقيقاً للأرباح
            String sqlDay = "SELECT DAYNAME(date) as day_name, SUM(total_amount) as daily_total " +
                    "FROM sales GROUP BY date ORDER BY daily_total DESC LIMIT 1";
            ResultSet rs3 = conn.createStatement().executeQuery(sqlDay);
            if (rs3.next()) {
                topDayLabel.setText(rs3.getString("day_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadWeeklyChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        // جلب آخر 7 أيام سجلت مبيعات
        String sql = "SELECT DATE_FORMAT(date, '%a') as day, SUM(total_amount) as total " +
                "FROM sales GROUP BY date ORDER BY date ASC LIMIT 7";

        try (Connection conn = DBConnector.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("day"), rs.getDouble("total")));
            }
            salesBarChart.getData().add(series);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}