public class CartItem {
    private int productId;
    private String productName;
    private int quantity;
    private double itemTotal;

    public CartItem(int productId, String productName, int quantity, double itemTotal) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.itemTotal = itemTotal;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getItemTotal() { return itemTotal; }
}