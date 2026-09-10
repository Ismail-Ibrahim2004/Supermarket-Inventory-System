package com.example.demo1;

import com.example.demo1.database.DBConnector;
import com.example.demo1.models.Product;
import com.example.demo1.models.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class AddProductController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField, categoryField, quantityField, buyingPriceField, sellingPriceField;
    @FXML private ComboBox<Supplier> supplierComboBox;

    private Product productToEdit;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        loadSuppliers();
    }

    private void loadSuppliers() {
        ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
        String sql = "SELECT supplier_id, supplier_name FROM suppliers";
        try (Connection conn = DBConnector.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                suppliers.add(new Supplier(rs.getInt("supplier_id"), rs.getString("supplier_name")));
            }
            supplierComboBox.setItems(suppliers);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setProductToEdit(Product product) {
        this.productToEdit = product;
        this.isEditMode = true;

        // التأكد من أن العنوان ليس null قبل استخدامه
        if (titleLabel != null) {
            titleLabel.setText("Edit Product Details");
        }

        nameField.setText(product.getName());
        categoryField.setText(product.getCategory());
        quantityField.setText(String.valueOf(product.getQuantity()));
        buyingPriceField.setText(String.valueOf(product.getBuyingPrice()));
        sellingPriceField.setText(String.valueOf(product.getSellingPrice()));

        // اختيار المورد الحالي في الـ ComboBox
        for (Supplier s : supplierComboBox.getItems()) {
            if (s.getName().equals(product.getSupplier())) {
                supplierComboBox.setValue(s);
                break;
            }
        }
    }

    @FXML
    private void handleSave() {
        // التحقق من المدخلات
        if (nameField.getText().isEmpty() || supplierComboBox.getValue() == null) {
            showAlert("Validation Error", "Please fill name and select a supplier.");
            return;
        }

        String sql = isEditMode ?
                "UPDATE products SET product_name=?, category=?, quantity=?, buying_price=?, selling_price=?, supplier_id=? WHERE product_id=?" :
                "INSERT INTO products (product_name, category, quantity, buying_price, selling_price, supplier_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnector.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, categoryField.getText());
                pstmt.setInt(3, Integer.parseInt(quantityField.getText()));
                pstmt.setDouble(4, Double.parseDouble(buyingPriceField.getText()));
                pstmt.setDouble(5, Double.parseDouble(sellingPriceField.getText()));
                pstmt.setInt(6, supplierComboBox.getValue().getId());

                if (isEditMode) {
                    pstmt.setInt(7, Integer.parseInt(productToEdit.getId())); // تحويل String ID إلى int
                }

                pstmt.executeUpdate();

                if (!isEditMode) {
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        logAction(conn, rs.getInt(1), Integer.parseInt(quantityField.getText()), "ADDED");
                    }
                }

                conn.commit();
                closeWindow();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not save product: " + e.getMessage());
        }
    }

    private void logAction(Connection conn, int pId, int qty, String type) throws SQLException {
        String sql = "INSERT INTO inventory_logs (product_id, change_type, quantity_change, user_id, notes, change_date) VALUES (?, ?, ?, 1, 'Initial Stock', NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pId);
            pstmt.setString(2, type);
            pstmt.setInt(3, qty);
            pstmt.executeUpdate();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}