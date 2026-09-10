package com.example.demo1;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label userNameLabel, userRoleLabel;

    // الأزرار المربوطة بملف الـ FXML
    @FXML private Button btnDashboard, btnProducts, btnSuppliers, btnCashiers,
            btnPOS, btnInventory, btnReports, btnSettings, btnAbout, btnLogout;

    private String currentRole;

    // دالة استقبال البيانات من شاشة الدخول وتحديد الصلاحيات
    public void setUserData(String name, String role) {
        this.currentRole = role.toLowerCase();
        userNameLabel.setText(name);
        userRoleLabel.setText(role);

        configurePermissions();

        // فتح الصفحة الأولى تلقائياً بناءً على الدور (أدمن أو كاشير)
        if (currentRole.equals("admin")) {
            loadView("AdminOverview.fxml");
            setActiveButton(btnDashboard);
        } else {
            loadView("POSView.fxml");
            setActiveButton(btnPOS);
        }
    }

    // إخفاء الأزرار غير المسموح بها للكاشير (الأدمن يرى كل شيء)
    private void configurePermissions() {
        if (currentRole.equals("cashier")) {
            // الكاشير لا يرى الإحصائيات، الموردين، إدارة الموظفين، والتقارير
            Button[] adminOnlyButtons = {btnDashboard, btnSuppliers, btnCashiers,btnReports};
            for (Button btn : adminOnlyButtons) {
                if (btn != null) {
                    btn.setVisible(false);
                    btn.setManaged(false);
                }
            }
        }
    }
    @FXML
    public void initialize() {
        // برمجة زر لوحة التحكم (الرئيسية)
        btnDashboard.setOnAction(e -> { loadView("AdminOverview.fxml"); setActiveButton(btnDashboard); });

        // --- تعديل مسار المنتجات ليتوافق مع الملفات الجديدة ---
        btnProducts.setOnAction(e -> {
            // تأكد من المسار إذا كان داخل مجلد com/example/demo1/ProductsView.fxml
            loadView("ProductsView.fxml");
            setActiveButton(btnProducts);
        });

        // برمجة زر الموردين
        btnSuppliers.setOnAction(e -> { loadView("SuppliersView.fxml"); setActiveButton(btnSuppliers); });

        // برمجة زر الكاشير
        btnCashiers.setOnAction(e -> { loadView("CashiersView.fxml"); setActiveButton(btnCashiers); });

        // برمجة زر نقطة البيع (POS)
        btnPOS.setOnAction(e -> { loadView("POSView.fxml"); setActiveButton(btnPOS); });

        // برمجة زر سجلات المخزن (تعديل ليتوافق مع الجدول الجديد inventory_logs)
        btnInventory.setOnAction(e -> { loadView("InventoryLogsView.fxml"); setActiveButton(btnInventory); });

        // برمجة باقي الأزرار
        btnReports.setOnAction(e -> { loadView("SalesReportsView.fxml"); setActiveButton(btnReports); });
        btnAbout.setOnAction(e -> { loadView("AboutView.fxml"); setActiveButton(btnAbout); });
        btnSettings.setOnAction(e -> handleSettingsNavigation());
        btnLogout.setOnAction(e -> handleLogout());
    }
    // دالة خاصة للتنقل للإعدادات لأنها تحتاج تمرير بيانات للمتحكم الآخر
    private void handleSettingsNavigation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsView.fxml"));
            Parent view = loader.load();

            SettingsController controller = loader.getController();
            controller.initData(userNameLabel.getText());

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            setActiveButton(btnSettings);
        } catch (IOException e) {
            System.err.println("Error loading SettingsView: " + e.getMessage());
        }
    }

    // الدالة العامة لتحميل أي صفحة FXML داخل منطقة المحتوى المركزية
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            // في حال عدم وجود الملف أو خطأ في المسار يظهر هذا الخطأ في الكونسول
            System.err.println("Error loading: " + fxmlFile + " | Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // دالة لتمييز الزر النشط وتغيير لونه (عن طريق كلاس CSS)
    private void setActiveButton(Button activeBtn) {
        Button[] allButtons = {btnDashboard, btnProducts, btnSuppliers, btnCashiers,
                btnPOS, btnInventory, btnReports, btnSettings, btnAbout};
        for (Button btn : allButtons) {
            if (btn != null) btn.getStyleClass().remove("active");
        }
        if (activeBtn != null) activeBtn.getStyleClass().add("active");
    }

    // دالة العودة لشاشة تسجيل الدخول
    // دالة العودة لشاشة تسجيل الدخول مع تصفير الجلسة
    private void handleLogout() {
        try {
            // 1. أهم خطوة: تصفير الجلسة الحالية تماماً قبل الخروج
            if (UserSession.getInstance() != null) {
                UserSession.getInstance().cleanUserSession();
                System.out.println("User session cleared successfully.");
            }

            // 2. تحميل شاشة الدخول
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnLogout.getScene().getWindow();

            stage.setScene(scene);
            stage.setFullScreen(false); // نلغي الشاشة الكاملة عند العودة للدخول
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}