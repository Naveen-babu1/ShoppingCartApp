import javafx.application.Application;
import com.google.gson.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.google.gson.reflect.TypeToken;
import adapters.RuntimeTypeAdapterFactory;
import java.io.*;
import java.util.function.Predicate;
import javafx.collections.transformation.FilteredList;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;


abstract class Product {
    private String name, category, imageUrl;
    private double price;
    protected int stock;

    public Product(String name, double price, String category, String imageUrl) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public int getStock() {return stock; }
    public void decreaseStock() { if (stock > 0) stock--; }
    public void increaseStock(int amount) {this.stock += amount; }
    public void reduceStockBy(int amount) { this.stock = Math.max(0, this.stock - amount); }

    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return Double.compare(product.price, price) == 0 &&
                name.equals(product.name) &&
                category.equals(product.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price, category);
    }
}

class ElectronicsProduct extends Product {
    public ElectronicsProduct(String name, double price, String imageUrl) {
        super(name, price, "Electronics", imageUrl);
        this.stock = 100;
    }
}

class GroceryProduct extends Product {
    public GroceryProduct(String name, double price, String imageUrl) {
        super(name, price, "Groceries", imageUrl);
        this.stock = 100;
    }
}

class CartItem {
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void incrementQuantity() { quantity++; }
    public void decrementQuantity() { if (quantity > 1) quantity--; }
    public double getTotalPrice(double rate) {
        return product.getPrice() * quantity * rate;
    }

    @Override
    public String toString() {
        return product.getName() + " x" + quantity;
    }
}

public class ShoppingCartApp extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    private void showLogin(Stage primaryStage) {
        Label userLabel = new Label("Username:");
        TextField userField = new TextField();
        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();
        Button loginButton = new Button("Login");
        Label messageLabel = new Label();

        loginButton.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();
            if ("admin".equals(username) && "admin123".equals(password)) {
                startMainApp(primaryStage); // Launch main app
            } else {
                messageLabel.setText("Invalid credentials.");
            }
        });

        VBox loginBox = new VBox(10, userLabel, userField, passLabel, passField, loginButton, messageLabel);
        loginBox.setPadding(new Insets(20));
        Scene loginScene = new Scene(loginBox, 400, 300);

        primaryStage.setTitle("Admin Login");
        primaryStage.setScene(loginScene);
        primaryStage.show();

    }

    @Override
    public void start(Stage primeStage) {
        showLogin(primeStage);
    }

    private void loadProductsFromFile() {

    }

    private void startMainApp(Stage stage) {

    }

    public void initCartOnStartup() {

    }

    private void addToCartFromTab() {


    }

    private void removeLastInstanceOf() {

    }

    private void removeFromCart() {

    }

    private void undo() {

    }

    private void clearCart() {

    }

    private void setupProductSearch() {

    }

    private void reduceStock() {

    }

    private boolean isInStock() {

    }

    private Product findProductInInventory() {

    }


    private void updateStockAfterLoad() {

    }

    private void applyCoupon() {

    }


    private void updateTotal() {

    }

    private void updateCurrency() {

    }

    private void updatePriceDisplay() {

    }

    private ListCell<Product> createCurrencyImageCell() {

    }

    private void saveCart() {

    }

    private void loadCart() {


    }

    private void generateReceipt() {

    }

    private void showAlert(String title, String message) {

    }
}
