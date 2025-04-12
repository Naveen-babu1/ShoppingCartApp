import javafx.application.Application;
import javafx.scene.control.*;
import javafx.stage.Stage;



abstract class Product {
}

class ElectronicsProduct extends Product {

}

class GroceryProduct extends Product {

}

class CartItem {

}

public class ShoppingCartApp extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    private void showLogin(Stage primaryStage) {

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
