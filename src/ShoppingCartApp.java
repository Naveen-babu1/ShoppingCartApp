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
            Product selected = null;

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab.getText().equals("Electronics")) {
            selected = electronicsListView.getSelectionModel().getSelectedItem();
        } else if (selectedTab.getText().equals("Groceries")) {
            selected = groceriesListView.getSelectionModel().getSelectedItem();
        }

        if (selected != null) {
            if (!isInStock(selected)) {
                showAlert("Out of Stock", "This product is out of stock.");
                return;
            }
            final Product selectedFinal = selected;

            Optional<CartItem> existing = cart.stream()
                    .filter(ci -> ci.getProduct().equals(selectedFinal))
                    .findFirst();

            if (existing.isPresent()) {
                existing.get().incrementQuantity();
            } else {
                cart.add(new CartItem(selectedFinal, 1));
            }

            reduceStock(selectedFinal);
            undoStack.push(() -> removeLastInstanceOf(selectedFinal));
            cartListView.refresh();
            updateTotal();
        }

    }

    

    private void removeLastInstanceOf() {
        for (CartItem ci : cart) {
            if (ci.getProduct().equals(product)) {
                if (ci.getQuantity() == 1) cart.remove(ci);
                else ci.decrementQuantity();
                break;
            }
        }
        cartListView.refresh();
        updateTotal();
    }

    private void removeFromCart() {
         CartItem selected = cartListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cart.remove(selected);

            Product matchedProduct = findProductInInventory(selected.getProduct());
            if (matchedProduct != null) {
                matchedProduct.increaseStock(selected.getQuantity());
            }

            undoStack.push(() -> {
                cart.add(selected);
                Product undoProduct = findProductInInventory(selected.getProduct());
                if (undoProduct != null) {
                    undoProduct.reduceStockBy(selected.getQuantity());
                }
                cartListView.refresh();
                updateTotal();
            });

            electronicsListView.refresh();
            groceriesListView.refresh();
            cartListView.refresh();
            updateTotal();
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            undoStack.pop().run();
        }
    }

    private void clearCart() {
        for (CartItem item : cart) {
            Product matchedProduct = findProductInInventory(item.getProduct());
            if (matchedProduct != null) {
                matchedProduct.increaseStock(item.getQuantity());
            }
        }
        cart.clear();
        couponApplied = false;
        couponDiscount = 0.0;
        updateTotal();
        cartListView.refresh();
        electronicsListView.refresh();
        groceriesListView.refresh();
        showAlert("Cart Cleared", "Your cart has been emptied.");
    }

    private void setupProductSearch(TextField searchField, FilteredList<Product> filteredElectronics, FilteredList<Product> filteredGroceries) {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            Predicate<Product> predicate = product -> product.getName().toLowerCase().contains(filter);
            filteredElectronics.setPredicate(predicate);
            filteredGroceries.setPredicate(predicate);
        });
    }

    private void reduceStock(Product product) {
        product.decreaseStock();
        electronicsListView.refresh();
        groceriesListView.refresh();
    }

     private boolean isInStock(Product product) {
        return product.getStock() > 0;
    }

    private Product findProductInInventory(Product target) {
        for (Product p : electronics) {
            if (p.equals(target)) return p;
        }
        for (Product p : groceries) {
            if (p.equals(target)) return p;
        }
        return null;
    }


    private void updateStockAfterLoad() {

    }

    private void applyCoupon(String code) {

    }


    private void updateTotal() {

    }

    private void updateCurrency(String currency) {

    }

    private void updatePriceDisplay() {

    }

    private ListCell<Product> createCurrencyImageCell() {
        return null;
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
