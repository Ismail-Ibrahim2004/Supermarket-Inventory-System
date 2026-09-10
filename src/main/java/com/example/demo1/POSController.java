package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Date;

public class POSController {
    @FXML private FlowPane productsFlowPane;
    @FXML private TextField searchField;
    @FXML private ListView<String> cartListView;
    @FXML private Label subtotalLabel, taxLabel, totalLabel;

    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private double subtotal = 0.0;

    private static class CartItem {
        String name; double price; int qty;
        CartItem(String name, double price) {
            this.name = name; this.price = price; this.qty = 1;
        }
        @Override
        public String toString() {
            return String.format("%-20s x%-3d $%.2f", name, qty, (price * qty));
        }
    }

    @FXML
    public void initialize() {
        loadProductsFromDB("");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadProductsFromDB(newVal));
    }

    private void loadProductsFromDB(String queryText) {
        productsFlowPane.getChildren().clear();
        String sql = "SELECT * FROM products WHERE product_name LIKE ? AND quantity > 0";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + queryText + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                createProductCard(rs.getString("product_name"), rs.getString("category"), rs.getDouble("selling_price"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void createProductCard(String name, String cat, double price) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15; -fx-cursor: hand;");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPrefSize(160, 130);

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 14px;");

        Label catLbl = new Label(cat);
        catLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Label priceLbl = new Label("$" + price);
        priceLbl.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 13px;");

        card.getChildren().addAll(nameLbl, catLbl, priceLbl);
        card.setOnMouseClicked(e -> addToCart(name, price));
        productsFlowPane.getChildren().add(card);
    }

    private void addToCart(String name, double price) {
        CartItem existing = cartItems.stream().filter(i -> i.name.equals(name)).findFirst().orElse(null);
        if (existing != null) existing.qty++;
        else cartItems.add(new CartItem(name, price));
        updateTotals();
        refreshCartDisplay();
    }

    private void updateTotals() {
        subtotal = cartItems.stream().mapToDouble(i -> i.price * i.qty).sum();
        double tax = subtotal * 0.14;
        subtotalLabel.setText("$" + String.format("%.2f", subtotal));
        taxLabel.setText("$" + String.format("%.2f", tax));
        totalLabel.setText("$" + String.format("%.2f", subtotal + tax));
    }

    private void refreshCartDisplay() {
        cartListView.getItems().clear();
        for (CartItem item : cartItems) cartListView.getItems().add(item.toString());
    }

    @FXML
    private void handleCheckout() {
        if (cartItems.isEmpty()) return;

        int currentUserId = UserSession.getInstance().getUserId();
        int currentCashierId = UserSession.getInstance().getCashierId();
        String currentUsername = UserSession.getInstance().getUsername();

        try (Connection conn = DBConnector.getConnection()) {
            conn.setAutoCommit(false);

            String saleSql = "INSERT INTO sales (date, total_amount, cashier_id) VALUES (NOW(), ?, ?)";
            PreparedStatement saleStmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
            saleStmt.setDouble(1, subtotal * 1.14);
            saleStmt.setInt(2, currentCashierId);
            saleStmt.executeUpdate();

            ResultSet keys = saleStmt.getGeneratedKeys();
            if (keys.next()) {
                int saleId = keys.getInt(1);
                for (CartItem item : cartItems) {
                    // تم تعديل الاستدعاء هنا ليرسل 4 باراميترات
                    updateInventoryAndLog(conn, item, currentUserId, saleId);
                }
                conn.commit();
                generateReceipt(saleId);
                showAlert("Success", "Completed by: " + currentUsername);
                cartItems.clear(); updateTotals(); refreshCartDisplay();
                loadProductsFromDB("");
            }
        } catch (SQLException e) {
            showAlert("Error", "DB Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateInventoryAndLog(Connection conn, CartItem item, int userId, int saleId) throws SQLException {
        int pId = -1;
        double unitPrice = 0.0;

        String getP = "SELECT product_id, selling_price FROM products WHERE product_name=?";
        try (PreparedStatement ps = conn.prepareStatement(getP)) {
            ps.setString(1, item.name);
            ResultSet rsP = ps.executeQuery();
            if(rsP.next()) {
                pId = rsP.getInt("product_id");
                unitPrice = rsP.getDouble("selling_price");
            }
        }

        if (pId != -1) {
            // 1. تسجيل محتويات الفاتورة في جدول sales_items (لحل مشكلة التقارير)
            String itemsSql = "INSERT INTO sales_items (sale_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement itemSt = conn.prepareStatement(itemsSql)) {
                itemSt.setInt(1, saleId);
                itemSt.setInt(2, pId);
                itemSt.setInt(3, item.qty);
                itemSt.setDouble(4, unitPrice);
                itemSt.executeUpdate();
            }

            // 2. تحديث كمية المخزون
            String updateStock = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";
            try (PreparedStatement st = conn.prepareStatement(updateStock)) {
                st.setInt(1, item.qty); st.setInt(2, pId); st.executeUpdate();
            }

            // 3. تسجيل سجل الحركة
            String logSql = "INSERT INTO inventory_logs (product_id, change_type, quantity_change, user_id, notes, change_date) VALUES (?, 'SOLD', ?, ?, 'POS Sale', NOW())";
            try (PreparedStatement logSt = conn.prepareStatement(logSql)) {
                logSt.setInt(1, pId);
                logSt.setInt(2, -item.qty);
                logSt.setInt(3, userId);
                logSt.executeUpdate();
            }
        }
    }

    private void generateReceipt(int saleId) {
        String fileName = "Receipt_" + saleId + ".txt";
        try (PrintWriter writer = new PrintWriter(new File(fileName))) {
            writer.println("**********************************");
            writer.println("       SUPERMARKET MANAGER        ");
            writer.println("    Cairo, Egypt - 0123456789     ");
            writer.println("**********************************");
            writer.println("Order ID: " + saleId);
            writer.println("Cashier : " + UserSession.getInstance().getUsername());
            writer.println("Date    : " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
            writer.println("----------------------------------");
            writer.println(String.format("%-18s %-4s %-8s", "Item", "Qty", "Total"));

            for (CartItem item : cartItems) {
                writer.println(String.format("%-18s %-4d $%-8.2f",
                        item.name, item.qty, (item.price * item.qty)));
            }

            writer.println("----------------------------------");
            writer.println("SUBTOTAL : " + subtotalLabel.getText());
            writer.println("TAX (14%): " + taxLabel.getText());
            writer.println("TOTAL    : " + totalLabel.getText());
            writer.println("**********************************");
            writer.println("    Thank you for your visit!     ");
            java.awt.Desktop.getDesktop().open(new File(fileName));
        } catch (Exception e) {
            System.err.println("Error printing receipt: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.show();
    }
}