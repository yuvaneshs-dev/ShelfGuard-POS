public class Product {
    private int productId;
    private String productName;
    private double basePrice;
    private int expiryAlertDays;
    private int bulkDiscountQty;
    private double bulkDiscountPercent;
    private double customDiscount; // The manual clearance discount

    public Product(int productId, String productName, double basePrice, int expiryAlertDays, int bulkDiscountQty, double bulkDiscountPercent, double customDiscount) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.expiryAlertDays = expiryAlertDays;
        this.bulkDiscountQty = bulkDiscountQty;
        this.bulkDiscountPercent = bulkDiscountPercent;
        this.customDiscount = customDiscount;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getBasePrice() { return basePrice; }
    public int getExpiryAlertDays() { return expiryAlertDays; }

    // Standard price calculation (1 item, no expiry data)
    public double calculateFinalPrice() {
        return basePrice - (basePrice * (customDiscount / 100));
    }

    // OVERLOADED METHOD: Calculates price based on quantity bought and days left to expiry!
    public double calculateFinalPrice(int quantityBought, int daysUntilExpiry) {
        double currentPrice = basePrice;

        // 1. Expiry Alert Clearance Rule (Overrides everything if expiring soon)
        if (daysUntilExpiry <= expiryAlertDays) {
            System.out.println("   [ALERT] Item expiring in " + daysUntilExpiry + " days! Applying 20% Clearance Discount.");
            currentPrice = basePrice - (basePrice * 0.20);
        } 
        // 2. Fallback to custom manual discount if not expiring
        else if (customDiscount > 0) {
            currentPrice = basePrice - (basePrice * (customDiscount / 100));
        }

        // 3. Bulk Discount Rule (e.g., Buy 3 or more, get extra 5% off)
        if (bulkDiscountQty > 0 && quantityBought >= bulkDiscountQty) {
            System.out.println("   [PROMO] Bulk quantity reached! Applying extra " + bulkDiscountPercent + "% off.");
            currentPrice = currentPrice - (currentPrice * (bulkDiscountPercent / 100));
        }

        return currentPrice * quantityBought; // Total price for all pieces
    }
}
