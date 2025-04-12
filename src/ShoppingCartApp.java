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

    private ObservableList<Product> electronics = FXCollections.observableArrayList();
    private ObservableList<Product> groceries = FXCollections.observableArrayList();
    private ObservableList<CartItem> cart = FXCollections.observableArrayList();

    private ListView<Product> electronicsListView = new ListView<>();
    private ListView<Product> groceriesListView = new ListView<>();
    private ListView<CartItem> cartListView = new ListView<>();
    private Label totalLabel = new Label("Total: $0.00");
    private TabPane tabPane;


    private ComboBox<String> currencySelector = new ComboBox<>();
    private String currentCurrency = "USD";
    private double conversionRate = 1.0;

    private boolean couponApplied = false;
    private final String VALID_COUPON = "SAVE10";
    private double couponDiscount = 0.0;

    private Stack<Runnable> undoStack = new Stack<>();
    private final String CART_FILE = "cart_data.json";

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
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/products.json"))) {
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            for (JsonElement el : jsonArray) {
                JsonObject obj = el.getAsJsonObject();
                String type = obj.get("type").getAsString();
                String name = obj.get("name").getAsString();
                double price = obj.get("price").getAsDouble();
                String imageUrl = obj.get("imageUrl").getAsString();

                if ("electronics".equalsIgnoreCase(type)) {
                    electronics.add(new ElectronicsProduct(name, price, imageUrl));
                } else if ("grocery".equalsIgnoreCase(type)) {
                    groceries.add(new GroceryProduct(name, price, imageUrl));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load products from file.");
        }
    }

    private void startMainApp(Stage stage) {
        loadProductsFromFile();
        TextField searchField = new TextField();
        searchField.setPromptText("Search products");
        FilteredList<Product> filteredElectronics = new FilteredList<>(electronics, p -> true);
        FilteredList<Product> filteredGroceries = new FilteredList<>(groceries, p -> true);
        electronicsListView.setItems(filteredElectronics);
        groceriesListView.setItems(filteredGroceries);
        setupProductSearch(searchField, filteredElectronics, filteredGroceries);
        cartListView.setItems(cart);
        electronicsListView.setCellFactory(param -> createCurrencyImageCell());
        groceriesListView.setCellFactory(param -> createCurrencyImageCell());
        currencySelector.getItems().addAll("USD", "EUR", "INR");
        currencySelector.setValue("USD");
        currencySelector.setOnAction(e -> updateCurrency(currencySelector.getValue()));
        tabPane = new TabPane();
        Tab electronicsTab = new Tab("Electronics", electronicsListView);
        Tab groceriesTab = new Tab("Groceries", groceriesListView);
        tabPane.getTabs().addAll(electronicsTab, groceriesTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Button addButton = new Button("Add to Cart");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> addToCartFromTab());

        VBox productBox = new VBox(10,
                new Label("Products"),
                searchField,
                currencySelector,
                tabPane,
                addButton);
        productBox.setPadding(new Insets(15));
        productBox.setPrefWidth(350);

        Button removeButton = new Button("Remove from Cart");
        Button undoButton = new Button("Undo");
        Button saveButton = new Button("Save Cart");
        Button loadButton = new Button("Load Cart");
        Button receiptButton = new Button("Generate Receipt");
        Button clearCartButton = new Button("Clear Cart");
        TextField couponField = new TextField();
        couponField.setPromptText("Enter coupon code");
        Button applyCouponButton = new Button("Apply Coupon");

        removeButton.setOnAction(e -> removeFromCart());
        undoButton.setOnAction(e -> undo());
        saveButton.setOnAction(e -> saveCart());
        loadButton.setOnAction(e -> loadCart());
        receiptButton.setOnAction(e -> generateReceipt());
        clearCartButton.setOnAction(e -> clearCart());
        applyCouponButton.setOnAction(e -> applyCoupon(couponField.getText()));
        VBox leftButtons = new VBox(10, couponField, applyCouponButton);
        VBox rightButtons = new VBox(10, saveButton, loadButton, receiptButton);

        HBox buttonColumns = new HBox(20, leftButtons, rightButtons);
        HBox cartControls = new HBox(10,
                removeButton,
                undoButton,
                clearCartButton,
                new Label("Total:"),
                totalLabel,
                buttonColumns);

        VBox cartBox = new VBox(15, new Label("Shopping Cart"), cartListView, cartControls);
        cartBox.setPadding(new Insets(15));
        cartBox.setPrefWidth(400);

        BorderPane root = new BorderPane();
        root.setLeft(productBox);
        root.setCenter(cartBox);

        Scene scene = new Scene(root, 1300, 600);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setTitle("🛒 Shopping Cart System");
        stage.setScene(scene);
        stage.getIcons().add(new Image("https://cdn-icons-png.flaticon.com/512/1170/1170678.png"));
        stage.show();
    }

    public void initCartOnStartup() {
        File file = new File(CART_FILE);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            RuntimeTypeAdapterFactory<Product> productAdapterFactory = RuntimeTypeAdapterFactory
                    .of(Product.class, "type")
                    .registerSubtype(ElectronicsProduct.class, "electronics")
                    .registerSubtype(GroceryProduct.class, "grocery");

            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(productAdapterFactory)
                    .create();

            Type type = new TypeToken<List<CartItem>>() {}.getType();
            List<CartItem> loaded = gson.fromJson(reader, type);
            cart.setAll(loaded);
            cartListView.refresh();
            updateTotal();
        } catch (IOException e) {
            System.out.println("[INFO] No previous cart found or failed to load.");
        }
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

    private void setupProductSearch(TextField searchField, FilteredList<Product> filteredElectronics, FilteredList<Product> filteredGroceries) {

    }

    private void reduceStock() {

    }

    private boolean isInStock() {
        return false;
    }

    private Product findProductInInventory() {
        return null;
    }


    private void applyCoupon(String code) {
        if (code.equalsIgnoreCase(VALID_COUPON) && !couponApplied) {
            couponApplied = true;
            showAlert("Coupon Applied", "10% discount applied!");
            updateTotal();
        } else if (couponApplied) {
            showAlert("Already Applied", "Coupon has already been used.");
        } else {
            showAlert("Invalid Coupon", "Please enter a valid coupon code.");
        }
    }


    private void updateTotal() {
        double total = cart.stream().mapToDouble(ci -> ci.getProduct().getPrice() * ci.getQuantity() * conversionRate).sum();
        if (couponApplied) {
            couponDiscount = total * 0.10;
            total -= couponDiscount;
            totalLabel.setText(String.format("Total: %s %.2f (10%% off)", currentCurrency, total));
        } else {
            totalLabel.setText(String.format("Total: %s %.2f", currentCurrency, total));
        }
    }

    private void updateCurrency(String currency) {
        try {
            String apiKey = "f29cdcc22808a5da29fba2b8"; // Replace with your API key from exchangerate-api.com
            String urlStr = "https://v6.exchangerate-api.com/v6/" + apiKey + "/pair/USD/" + currency;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();

            Gson gson = new Gson();
            Map<?, ?> jsonMap = gson.fromJson(response.toString(), Map.class);
            conversionRate = ((Number) jsonMap.get("conversion_rate")).doubleValue();
            currentCurrency = currency;
            updatePriceDisplay();
        } catch (Exception e) {
            showAlert("Error", "Failed to fetch currency conversion.");
        }
    }

    private void updatePriceDisplay() {
        electronicsListView.setCellFactory(param -> createCurrencyImageCell());
        groceriesListView.setCellFactory(param -> createCurrencyImageCell());
        cartListView.refresh();
        updateTotal();
    }

    private ListCell<Product> createCurrencyImageCell() {
        return new ListCell<>() {
            private final ImageView imageView = new ImageView();
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    double convertedPrice = item.getPrice() * conversionRate;
                    setText(item.getName() + " (" + currentCurrency + " " + String.format("%.2f", convertedPrice) + ") - Stock: " + item.getStock());
                    imageView.setImage(new Image(item.getImageUrl(), 40, 40, true, true));
                    setGraphic(imageView);
                }
            }
        };
    }

    private void saveCart() {
        try (Writer writer = new FileWriter(CART_FILE)) {
            RuntimeTypeAdapterFactory<Product> productAdapterFactory = RuntimeTypeAdapterFactory
                    .of(Product.class, "type")
                    .registerSubtype(ElectronicsProduct.class, "electronics")
                    .registerSubtype(GroceryProduct.class, "grocery");

            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(productAdapterFactory)
                    .create();

            gson.toJson(cart, writer);
            showAlert("Success", "Cart saved successfully.");
        } catch (IOException e) {
            showAlert("Error", "Failed to save cart.");
        }
    }

    private void loadCart() {
        try (Reader reader = new FileReader(CART_FILE)) {
            RuntimeTypeAdapterFactory<Product> productAdapterFactory = RuntimeTypeAdapterFactory
                    .of(Product.class, "type")
                    .registerSubtype(ElectronicsProduct.class, "electronics")
                    .registerSubtype(GroceryProduct.class, "grocery");

            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(productAdapterFactory)
                    .create();

            Type type = new TypeToken<List<CartItem>>() {}.getType();
            List<CartItem> loaded = gson.fromJson(reader, type);
            cart.setAll(loaded);
            updateStockAfterLoad(loaded);
            cartListView.refresh();
            updateTotal();
            showAlert("Success", "Cart loaded successfully.");
        } catch (IOException e) {
            showAlert("Error", "Failed to load cart.");
        }

    }

    private void generateReceipt() {
        StringBuilder sb = new StringBuilder("Receipt:\n");
        for (CartItem ci : cart) {
            double lineTotal = ci.getProduct().getPrice() * ci.getQuantity() * conversionRate;
            sb.append(String.format("%s x%d - %s %.2f\n",
                    ci.getProduct().getName(), ci.getQuantity(), currentCurrency, lineTotal));
        }
        sb.append(totalLabel.getText());
        showAlert("Receipt", sb.toString());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
