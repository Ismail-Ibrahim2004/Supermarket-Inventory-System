package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class SettingsController {
    @FXML private TextField txtFullName, txtEmail, txtUsername, txtPhone;
    @FXML private ToggleButton btnNotifications, btnAutosave;

    private String loggedInUser;

    public void initData(String username) {
        this.loggedInUser = username;
        loadCurrentUserData();
    }

    private void loadCurrentUserData() {
        // جلب البيانات باستخدام LEFT JOIN كما في الصورة التي أرفقتها
        String sql = "SELECT u.username, c.full_name, c.email, c.phone_number " +
                "FROM users u " +
                "LEFT JOIN cashier c ON u.id = c.user_id " +
                "WHERE u.username = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, loggedInUser);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                txtUsername.setText(rs.getString("username"));
                // تعبئة البيانات أو تركها فارغة إذا كانت NULL في الداتابيز
                txtFullName.setText(rs.getString("full_name") == null ? "" : rs.getString("full_name"));
                txtEmail.setText(rs.getString("email") == null ? "" : rs.getString("email"));
                txtPhone.setText(rs.getString("phone_number") == null ? "" : rs.getString("phone_number"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSave() {
        // نتحقق أولاً هل للمستخدم سجل في جدول cashier؟
        String checkSql = "SELECT id FROM users WHERE username = ?";

        try (Connection conn = DBConnector.getConnection()) {
            int userId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, loggedInUser);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) userId = rs.getInt("id");
            }

            // محاولة التحديث (Update)
            String updateSql = "UPDATE cashier SET full_name = ?, email = ?, phone_number = ? WHERE user_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, txtFullName.getText());
            updateStmt.setString(2, txtEmail.getText());
            updateStmt.setString(3, txtPhone.getText());
            updateStmt.setInt(4, userId);

            int affectedRows = updateStmt.executeUpdate();

            // إذا لم يتم التحديث (يعني الخانة NULL في الصورة)، نقوم بالإضافة (Insert)
            if (affectedRows == 0) {
                String insertSql = "INSERT INTO cashier (user_id, full_name, email, phone_number) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, userId);
                insertStmt.setString(2, txtFullName.getText());
                insertStmt.setString(3, txtEmail.getText());
                insertStmt.setString(4, txtPhone.getText());
                insertStmt.executeUpdate();
            }

            showSuccessAlert("Settings saved successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}