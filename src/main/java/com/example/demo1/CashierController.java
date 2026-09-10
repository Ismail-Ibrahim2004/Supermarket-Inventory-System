package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import java.sql.*;

public class CashierController {

    @FXML private TableView<Cashier> cashierTable;
    @FXML private TableColumn<Cashier, String> colName, colUsername, colRole, colLastLogin, colSales, colPerformance;
    @FXML private TableColumn<Cashier, Void> colActions;
    @FXML private TableColumn<Cashier, Double> colSalary; // تم تغيير النوع لـ Cashier
    @FXML private TableColumn<Cashier, String> colShift;  // تم تغيير النوع لـ Cashier
    @FXML private Label lblTotalStaff;

    private ObservableList<Cashier> cashierList = FXCollections.observableArrayList();

    // --- موديل البيانات الداخلي المحدث ---
    public static class Cashier {
        private int id;
        private String name, username, role, lastLogin, performance, shift;
        private double salary;
        private int salesCount;

        public Cashier(int id, String name, String username, String role, String lastLogin, int salesCount, double salary, String shift) {
            this.id = id;
            this.name = name;
            this.username = username;
            this.role = role;
            this.lastLogin = (lastLogin == null) ? "Active Now" : lastLogin;
            this.salesCount = salesCount;
            this.salary = salary;
            this.shift = (shift == null) ? "Not Set" : shift;

            if (salesCount >= 20) this.performance = "Excellent";
            else if (salesCount >= 10) this.performance = "Good";
            else this.performance = "Average";
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getLastLogin() { return lastLogin; }
        public String getSales() { return salesCount + " sales"; }
        public String getPerformance() { return performance; }
        public double getSalary() { return salary; }
        public String getShift() { return shift; }
    }

    @FXML
    public void initialize() {
        setupColumns();
        loadCashiers();
    }

    private void setupColumns() {
        // ربط الأعمدة الأساسية
        setCenteredColumn(colName, "name");
        setCenteredColumn(colUsername, "username");
        setCenteredColumn(colRole, "role");
        setCenteredColumn(colLastLogin, "lastLogin");
        setCenteredColumn(colSales, "sales");

        // ربط أعمدة الراتب والوردية الجديدة
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colSalary.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.format("%.2f", item));
                setAlignment(Pos.CENTER);
            }
        });

        colShift.setCellValueFactory(new PropertyValueFactory<>("shift"));
        colShift.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
            }
        });

        // عمود الأداء الملون
        colPerformance.setCellValueFactory(new PropertyValueFactory<>("performance"));
        colPerformance.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge-" + item.toLowerCase());
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // عمود الأزرار
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(15, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-text-fill: #3b82f6; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 16px;");
                btnDelete.setStyle("-fx-text-fill: #ef4444; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 16px;");

                btnEdit.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
                setAlignment(Pos.CENTER);
            }
        });
    }

    private void setCenteredColumn(TableColumn<Cashier, String> column, String property) {
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
    }

    private void loadCashiers() {
        cashierList.clear();
        // استعلام معدل لربط الـ View بجدول الكاشير لجلب الراتب والوردية
        String sql = "SELECT v.*, c.salary, c.shift FROM cashier_performance_view v " +
                "JOIN cashier c ON v.cashier_id = c.cashier_id";

        try (Connection conn = DBConnector.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                cashierList.add(new Cashier(
                        rs.getInt("cashier_id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("last_login"),
                        rs.getInt("total_sales"),
                        rs.getDouble("salary"),
                        rs.getString("shift")
                ));
            }
            cashierTable.setItems(cashierList);
            if(lblTotalStaff != null) lblTotalStaff.setText(String.valueOf(cashierList.size()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAddCashier() {
        showCashierDialog(null);
    }

    private void handleEdit(Cashier cashier) {
        showCashierDialog(cashier);
    }

    private void handleDelete(Cashier cashier) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + cashier.getName() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DBConnector.getConnection();
                     PreparedStatement st = conn.prepareStatement("DELETE FROM users WHERE id = (SELECT user_id FROM cashier WHERE cashier_id = ?)")) {
                    st.setInt(1, cashier.getId());
                    st.executeUpdate();
                    loadCashiers();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void showCashierDialog(Cashier cashier) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(cashier == null ? "Add Staff" : "Edit Staff");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        TextField userField = new TextField();
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Admin", "Cashier"));
        TextField salaryField = new TextField(); // حقل الراتب
        ComboBox<String> shiftBox = new ComboBox<>(FXCollections.observableArrayList("Morning", "Night")); // حقل الوردية

        if (cashier != null) {
            nameField.setText(cashier.getName());
            userField.setText(cashier.getUsername());
            roleBox.setValue(cashier.getRole());
            salaryField.setText(String.valueOf(cashier.getSalary()));
            shiftBox.setValue(cashier.getShift().equals("Not Set") ? "Morning" : cashier.getShift());
        } else {
            roleBox.setValue("Cashier");
            shiftBox.setValue("Morning");
        }

        grid.add(new Label("Full Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Username:"), 0, 1); grid.add(userField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);     grid.add(roleBox, 1, 2);
        grid.add(new Label("Salary:"), 0, 3);   grid.add(salaryField, 1, 3);
        grid.add(new Label("Shift:"), 0, 4);    grid.add(shiftBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(res -> {
            if (res == saveBtn) {
                try {
                    double salary = Double.parseDouble(salaryField.getText());
                    saveToDatabase(cashier, nameField.getText(), userField.getText(), roleBox.getValue(), salary, shiftBox.getValue());
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Please enter a valid salary number!").show();
                }
            }
        });
    }
    private void saveToDatabase(Cashier cashier, String name, String user, String role, double salary, String shift) {
        try (Connection conn = DBConnector.getConnection()) {
            conn.setAutoCommit(false); // لبدء عملية Transaction لضمان حفظ الجدولين معاً
            try {
                if (cashier == null) {
                    // 1. إضافة المستخدم في جدول users
                    String sqlUser = "INSERT INTO users (username, password, role) VALUES (?, '123', ?)";
                    PreparedStatement psU = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
                    psU.setString(1, user);
                    psU.setString(2, role);
                    psU.executeUpdate();

                    ResultSet rs = psU.getGeneratedKeys();
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        // 2. إضافة بيانات الكاشير مع الراتب والوردية
                        String sqlC = "INSERT INTO cashier (full_name, user_id, salary, shift) VALUES (?, ?, ?, ?)";
                        PreparedStatement psC = conn.prepareStatement(sqlC);
                        psC.setString(1, name);
                        psC.setInt(2, userId);
                        psC.setDouble(3, salary);
                        psC.setString(4, shift);
                        psC.executeUpdate();
                    }
                } else {
                    // تحديث جدول users وجدول cashier معاً باستخدام JOIN
                    String sqlUpd = "UPDATE users u JOIN cashier c ON u.id = c.user_id " +
                            "SET c.full_name=?, u.username=?, u.role=?, c.salary=?, c.shift=? " +
                            "WHERE c.cashier_id=?";
                    PreparedStatement psUpd = conn.prepareStatement(sqlUpd);
                    psUpd.setString(1, name);
                    psUpd.setString(2, user);
                    psUpd.setString(3, role);
                    psUpd.setDouble(4, salary);
                    psUpd.setString(5, shift);
                    psUpd.setInt(6, cashier.getId());
                    psUpd.executeUpdate();
                }
                conn.commit(); // تنفيذ التغييرات
                loadCashiers(); // إعادة تحميل الجدول
            } catch (SQLException e) {
                conn.rollback(); // التراجع في حالة حدوث خطأ
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}