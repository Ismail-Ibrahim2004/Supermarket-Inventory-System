package com.example.demo1;

import com.example.demo1.database.DBConnector;
import com.example.demo1.models.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.sql.*;

public class SuppliersController {
    @FXML private TableView<Supplier> suppliersTable;
    @FXML private TableColumn<Supplier, String> colName, colContact, colEmail, colItems, colAddress, colStatus, colActions;

    private ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    @FXML
    public void initialize() {
        // 1. ربط الأعمدة ببيانات الموديل
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("itemsProvided"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. جعل النص في منتصف جميع الأعمدة (Center Alignment)
        colName.setStyle("-fx-alignment: CENTER;");
        colContact.setStyle("-fx-alignment: CENTER;");
        colEmail.setStyle("-fx-alignment: CENTER;");
        colItems.setStyle("-fx-alignment: CENTER;");
        colAddress.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");
        colActions.setStyle("-fx-alignment: CENTER;");

        // 3. تنسيق عمود الحالة (Status) ليظهر كـ Badge ملون
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().clear();
                    if (item.equalsIgnoreCase("Active")) {
                        badge.getStyleClass().add("badge-active");
                    } else {
                        badge.getStyleClass().add("badge-inactive");
                    }
                    setGraphic(badge);
                    // تأكيد المحاذاة داخل الخلية التي تحتوي على الـ Badge
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // 4. إعداد عمود العمليات وتحميل البيانات
        setupActionsColumn();
        loadSuppliersData();
    }
    private void loadSuppliersData() {
        supplierList.clear();
        String sql = "SELECT * FROM suppliers";
        try (Connection conn = DBConnector.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                supplierList.add(new Supplier(
                        rs.getInt("supplier_id"),
                        rs.getString("supplier_name"),
                        rs.getString("supplier_phone"),
                        rs.getString("supplier_email"),
                        rs.getString("items_provided"),
                        rs.getString("supplier_address"),
                        rs.getString("status")
                ));
            }
            suppliersTable.setItems(supplierList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final HBox container = new HBox(12, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("action-button-edit");
                deleteBtn.getStyleClass().add("action-button-delete");
                container.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                    editBtn.setOnAction(e -> handleOpenDialog(getTableView().getItems().get(getIndex())));
                    deleteBtn.setOnAction(e -> handleDeleteSupplier(getTableView().getItems().get(getIndex())));
                }
            }
        });
    }

    private void handleOpenDialog(Supplier supplier) {
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle(supplier == null ? "Add New Supplier" : "Edit Supplier");
        applyAppIcon(dialog);

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameF = new TextField();
        TextField phoneF = new TextField();
        TextField emailF = new TextField();
        TextField itemsF = new TextField();
        TextField addrF = new TextField();
        ComboBox<String> statusCB = new ComboBox<>(FXCollections.observableArrayList("Active", "Inactive"));

        if (supplier != null) {
            nameF.setText(supplier.getName());
            phoneF.setText(supplier.getPhone());
            emailF.setText(supplier.getEmail());
            itemsF.setText(supplier.getItemsProvided());
            addrF.setText(supplier.getAddress());
            statusCB.setValue(supplier.getStatus());
        } else {
            statusCB.setValue("Active");
        }

        grid.add(new Label("Name:"), 0, 0); grid.add(nameF, 1, 0);
        grid.add(new Label("Phone:"), 0, 1); grid.add(phoneF, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailF, 1, 2);
        grid.add(new Label("Items Provided:"), 0, 3); grid.add(itemsF, 1, 3);
        grid.add(new Label("Address:"), 0, 4); grid.add(addrF, 1, 4);
        grid.add(new Label("Status:"), 0, 5); grid.add(statusCB, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                return new Supplier(
                        supplier == null ? 0 : supplier.getId(),
                        nameF.getText(), phoneF.getText(), emailF.getText(),
                        itemsF.getText(), addrF.getText(), statusCB.getValue()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (supplier == null) saveToDB(res); else updateInDB(res);
        });
    }

    private void applyAppIcon(Dialog<?> dialog) {
        try {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/demo1/app_icon.png")));
        } catch (Exception ignored) {}
    }

    private void saveToDB(Supplier s) {
        String sql = "INSERT INTO suppliers (supplier_name, supplier_phone, supplier_email, items_provided, supplier_address, status) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getItemsProvided());
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getStatus());
            ps.executeUpdate();
            loadSuppliersData();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateInDB(Supplier s) {
        String sql = "UPDATE suppliers SET supplier_name=?, supplier_phone=?, supplier_email=?, items_provided=?, supplier_address=?, status=? WHERE supplier_id=?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getItemsProvided());
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getStatus());
            ps.setInt(7, s.getId());
            ps.executeUpdate();
            loadSuppliersData();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleDeleteSupplier(Supplier s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + s.getName() + "?", ButtonType.YES, ButtonType.NO);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        try { stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/demo1/app_icon.png"))); } catch (Exception ignored) {}

        if (alert.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM suppliers WHERE supplier_id = ?")) {
                ps.setInt(1, s.getId());
                ps.executeUpdate();
                loadSuppliersData();
            } catch (SQLException e) {
                // تظهر هذه الرسالة إذا كان المورد مرتبط بمنتجات
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setHeaderText("Database Restriction");
                error.setContentText("Cannot delete supplier. To delete it, go to MySQL Workbench and run:\nSET FOREIGN_KEY_CHECKS = 0;");
                error.show();
            }
        }
    }

    @FXML private void handleAddSupplier() {
        handleOpenDialog(null);
    }
}