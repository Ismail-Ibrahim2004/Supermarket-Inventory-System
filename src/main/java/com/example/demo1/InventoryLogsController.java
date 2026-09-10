package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class InventoryLogsController {
    @FXML private TableView<InventoryLog> logsTable;
    @FXML private TableColumn<InventoryLog, String> colDate, colProduct, colAction, colQuantity, colUser, colNotes;
    @FXML private DatePicker datePicker;

    // موديل البيانات المحدث ليشمل المستخدم والملاحظات
    public static class InventoryLog {
        private String date, product, action, quantity, user, notes;
        public InventoryLog(String d, String p, String a, String q, String u, String n) {
            this.date = d; this.product = p; this.action = a;
            this.quantity = q; this.user = u; this.notes = n;
        }
        public String getDate() { return date; }
        public String getProduct() { return product; }
        public String getAction() { return action; }
        public String getQuantity() { return quantity; }
        public String getUser() { return user; }
        public String getNotes() { return notes; }
    }

    @FXML
    public void initialize() {
        // 1. ربط الأعمدة بالبيانات
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // 2. توسيط محتوى جميع الأعمدة
        String centerStyle = "-fx-alignment: CENTER;";
        colDate.setStyle(centerStyle);
        colProduct.setStyle(centerStyle);
        colAction.setStyle(centerStyle);
        colQuantity.setStyle(centerStyle);
        colUser.setStyle(centerStyle);
        colNotes.setStyle(centerStyle);

        // 3. تنسيق عمود الـ Action ليظهر كـ Badge موسط
        colAction.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().clear();
                    if (item.equalsIgnoreCase("Restocked") || item.equalsIgnoreCase("Added")) {
                        badge.getStyleClass().add("badge-restocked");
                    } else if (item.equalsIgnoreCase("Sold")) {
                        badge.getStyleClass().add("badge-sold");
                    } else {
                        badge.getStyleClass().add("badge-removed");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // تشغيل الفلترة عند اختيار تاريخ
        datePicker.setOnAction(e -> loadLogsFromDB(datePicker.getValue().toString()));

        // تحميل البيانات عند فتح الصفحة
        loadLogsFromDB(null);
    }

    private void loadLogsFromDB(String filterDate) {
        ObservableList<InventoryLog> data = FXCollections.observableArrayList();

        // استخدام LEFT JOIN لضمان ظهور البيانات حتى لو كان الـ user_id هو NULL
        String sql = "SELECT l.*, p.product_name, u.username " +
                "FROM inventory_logs l " +
                "LEFT JOIN products p ON l.product_id = p.product_id " +
                "LEFT JOIN users u ON l.user_id = u.id ";

        if (filterDate != null) {
            sql += " WHERE DATE(l.change_date) = ? ";
        }
        sql += " ORDER BY l.change_date DESC";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (filterDate != null) {
                stmt.setString(1, filterDate);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // معالجة القيم التي قد تكون NULL في قاعدة البيانات
                String pName = rs.getString("product_name") != null ? rs.getString("product_name") : "Unknown";
                String uName = rs.getString("username") != null ? rs.getString("username") : "System";
                String note = rs.getString("notes") != null ? rs.getString("notes") : "-";

                // تنسيق الكمية بإضافة علامة + للزيادة
                String qty = (rs.getInt("quantity_change") > 0 ? "+" : "") + rs.getInt("quantity_change");

                data.add(new InventoryLog(
                        rs.getString("change_date"),
                        pName,
                        rs.getString("change_type"),
                        qty,
                        uName,
                        note
                ));
            }
            logsTable.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}