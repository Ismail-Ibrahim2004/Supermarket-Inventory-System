package com.example.demo1;

import com.example.demo1.database.DBConnector;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.*;
import java.sql.*;
import java.util.Properties;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button signInButton;
    @FXML private Button demoAdminButton;
    @FXML private Button demoCashierButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label statusLabel;
    @FXML private Circle statusIndicator;

    private final String CONFIG_FILE = "remember_me.properties";

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Admin", "Cashier");
        roleComboBox.setValue("Admin");

        loadRememberedUser();
        checkDatabaseConnection();

        signInButton.setOnAction(e -> handleLogin());

        // أزرار الديمو
        demoAdminButton.setOnAction(e -> {
            usernameField.setText("admin");
            passwordField.setText("admin123");
            roleComboBox.setValue("Admin");
            handleLogin();
        });

        demoCashierButton.setOnAction(e -> {
            usernameField.setText("cashier");
            passwordField.setText("123");
            roleComboBox.setValue("Cashier");
            handleLogin();
        });

        forgotPasswordLink.setOnAction(e -> showAlert("Forgot Password", "Please contact the system administrator.", Alert.AlertType.INFORMATION));
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill all fields!", Alert.AlertType.ERROR);
            return;
        }

        if (authenticateUser(username, password, role)) {
            saveRememberMeSettings(username);
            openDashboard(username, role);
        } else {
            showAlert("Login Failed", "Invalid credentials or role selection.", Alert.AlertType.ERROR);
        }
    }

    private boolean authenticateUser(String username, String password, String role) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int uId = rs.getInt("id");
                String uName = rs.getString("username");
                String uRole = rs.getString("role");

                // 1. تحديث وقت آخر ظهور في قاعدة البيانات
                updateLastLogin(uId);

                // 2. جلب الـ cashier_id المرتبط بهذا المستخدم
                int cId = fetchCashierId(uId);

                // 3. إنشاء الجلسة (UserSession) لتخزين البيانات في الذاكرة
                UserSession.getInstance(uName, uRole, uId, cId);

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    private int fetchCashierId(int userId) {
        String query = "SELECT cashier_id FROM cashier WHERE user_id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int cId = rs.getInt("cashier_id");
                System.out.println("DEBUG: Found Cashier ID " + cId + " for User ID " + userId);
                return cId;
            } else {
                System.err.println("DEBUG: No cashier record found for User ID " + userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // نرجع -1 بدل 0 للتفرقة
    }
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openDashboard(String name, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setUserData(name, role);

            Stage stage = (Stage) signInButton.getScene().getWindow();
            setStageIcon(stage);

            stage.setScene(new Scene(root));
            stage.setFullScreen(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        setStageIcon(alertStage);
        alert.showAndWait();
    }

    private void setStageIcon(Stage stage) {
        try {
            InputStream iconStream = getClass().getResourceAsStream("/com/example/demo1/images/app_icon.png");
            if (iconStream != null) {
                stage.getIcons().setAll(new Image(iconStream));
            }
        } catch (Exception e) {
            System.out.println("Icon not found.");
        }
    }

    private void checkDatabaseConnection() {
        if (DBConnector.testConnection()) {
            statusLabel.setText("Database connected!");
            statusIndicator.setFill(Color.web("#22c55e"));
        } else {
            statusLabel.setText("Connection failed!");
            statusIndicator.setFill(Color.web("#ef4444"));
        }
    }

    private void saveRememberMeSettings(String user) {
        Properties props = new Properties();
        props.setProperty("username", rememberMeCheckBox.isSelected() ? user : "");
        props.setProperty("remember", String.valueOf(rememberMeCheckBox.isSelected()));
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRememberedUser() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            if ("true".equals(props.getProperty("remember"))) {
                usernameField.setText(props.getProperty("username"));
                rememberMeCheckBox.setSelected(true);
            }
        } catch (IOException ignored) {}
    }
}