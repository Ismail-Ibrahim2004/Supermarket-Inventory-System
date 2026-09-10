package com.example.demo1;

import com.example.demo1.database.DBConnector;
import com.example.demo1.models.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class ProductsController {

    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, String> idCol, nameCol, categoryCol, supplierCol, statusCol;
    @FXML private TableColumn<Product, Integer> quantityCol;
    @FXML private TableColumn<Product, Double> priceCol;
    @FXML private TableColumn<Product, Void> actionsCol;

    @FXML
    public void initialize() {
        setupColumns();
        loadDataFromDB();
    }

    private void setupColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplier"));

        tableView.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));

        // منطق حالة المخزن
        statusCol.setCellValueFactory(cellData -> {
            int qty = cellData.getValue().getQuantity();
            if (qty == 0) return new SimpleStringProperty("Out Of Stock");
            if (qty <= 5) return new SimpleStringProperty("Low Stock");
            return new SimpleStringProperty("In Stock");
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(status);
                    String styleClass = status.toLowerCase().replace(" ", "-");
                    label.getStyleClass().add("status-" + (styleClass.contains("in") ? "in" : styleClass.contains("low") ? "low" : "out"));
                    setGraphic(label);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // برمجة أزرار العمليات
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(10, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("icon-btn");
                deleteBtn.getStyleClass().add("icon-btn");

                editBtn.setOnAction(e -> openEditProductPopup(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadDataFromDB() {
        ObservableList<Product> products = FXCollections.observableArrayList();

        // SQL JOIN لجلب اسم المورد بدلاً من الـ ID
        String sql = "SELECT p.*, s.supplier_name " +
                "FROM products p " +
                "LEFT JOIN suppliers s ON p.supplier_id = s.supplier_id";

        try (Connection conn = DBConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getDouble("buying_price"),
                        rs.getDouble("selling_price"),
                        rs.getString("supplier_name") != null ? rs.getString("supplier_name") : "No Supplier"
                ));
            }
            tableView.setItems(products);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database Error", "Could not load products: " + e.getMessage());
        }
    }

    @FXML
    private void openAddProductPopup() {
        showProductDialog(null);
    }

    private void openEditProductPopup(Product product) {
        showProductDialog(product);
    }

    private void showProductDialog(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_product.fxml"));
            Parent root = loader.load();

            if (product != null) {
                AddProductController controller = loader.getController();
                controller.setProductToEdit(product);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle(product == null ? "Add Product" : "Edit Product");
            stage.showAndWait();
            loadDataFromDB();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + product.getName() + "?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBConnector.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        // حذف السجلات المرتبطة أولاً لتجنب Foreign Key Error
                        String deleteLogs = "DELETE FROM inventory_logs WHERE product_id=?";
                        String deleteSales = "DELETE FROM sales_items WHERE product_id=?";
                        String deleteProduct = "DELETE FROM products WHERE product_id=?";

                        try (PreparedStatement st1 = conn.prepareStatement(deleteLogs);
                             PreparedStatement st2 = conn.prepareStatement(deleteSales);
                             PreparedStatement st3 = conn.prepareStatement(deleteProduct)) {

                            st1.setString(1, product.getId());
                            st1.executeUpdate();

                            st2.setString(1, product.getId());
                            st2.executeUpdate();

                            st3.setString(1, product.getId());
                            st3.executeUpdate();
                        }

                        conn.commit();
                        loadDataFromDB();
                    } catch (SQLException ex) {
                        conn.rollback();
                        showError("Delete Error", "Cannot delete product: " + ex.getMessage());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}