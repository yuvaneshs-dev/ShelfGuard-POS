import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.sql.Statement;

public class ProductDAO {

    public void addProduct(String productName, double basePrice, int expiryAlertDays, int bulkDiscountQty, double bulkDiscountPercent, double customDiscount) {
        String query = "INSERT INTO Products (product_name, base_price, expiry_alert_days, bulk_discount_qty, bulk_discount_percent, discount_percent) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, productName);
            pstmt.setDouble(2, basePrice);
            pstmt.setInt(3, expiryAlertDays);
            pstmt.setInt(4, bulkDiscountQty);
            pstmt.setDouble(5, bulkDiscountPercent);
            pstmt.setDouble(6, customDiscount);
            
            pstmt.executeUpdate();
            System.out.println("✅ Product '" + productName + "' saved to MySQL successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Error saving product: " + e.getMessage());
        }
    }

    public void addStockBatch(int productId, int quantity, String expiryDateStr) {
        String query = "INSERT INTO Stock_Batches (product_id, quantity_available, expiry_date) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, quantity);
            pstmt.setString(3, expiryDateStr); 
            pstmt.executeUpdate();
            System.out.println("✅ Stock batch added successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Error adding stock batch: " + e.getMessage());
        }
    }

    public Product getProductByName(String searchName) {
        String query = "SELECT * FROM Products WHERE product_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, searchName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Product(
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getDouble("base_price"),
                    rs.getInt("expiry_alert_days"),
                    rs.getInt("bulk_discount_qty"),
                    rs.getDouble("bulk_discount_percent"),
                    rs.getDouble("discount_percent")
                );
            }
        } catch (SQLException e) {
            System.out.println("❌ Error fetching product: " + e.getMessage());
        }
        return null;
    }

    public int getEarliestExpiryDays(int productId) {
        String query = "SELECT MIN(expiry_date) AS earliest_expiry FROM Stock_Batches WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getDate("earliest_expiry") != null) {
                LocalDate expiryDate = rs.getDate("earliest_expiry").toLocalDate();
                long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
                return (int) Math.max(0, daysBetween);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error fetching batch expiry: " + e.getMessage());
        }
        return 999; 
    }

    // --- NEW METHOD ADDED HERE FOR KHATA RISK MANAGEMENT ---
    // Fetch customer credit details: Returns array [credit_limit, current_due]
    public double[] getCustomerCreditDetails(String phoneNumber) {
        String query = "SELECT credit_limit, current_due FROM Customers WHERE phone_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, phoneNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                double limit = rs.getDouble("credit_limit");
                double due = rs.getDouble("current_due");
                return new double[]{limit, due}; 
            }
        } catch (SQLException e) {
            System.out.println("❌ Error fetching customer: " + e.getMessage());
        }
        return null; // Returns null if customer is not found
    }
    // --- NEW: Open a new Khata Account ---
    public void addCustomer(String phone, String name, double creditLimit) {
        String query = "INSERT INTO Customers (phone_number, customer_name, credit_limit, current_due) VALUES (?, ?, ?, 0.00)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, phone);
            pstmt.setString(2, name);
            pstmt.setDouble(3, creditLimit);
            pstmt.executeUpdate();
            System.out.println("✅ Customer '" + name + "' registered successfully for Khata!");
        } catch (SQLException e) {
            System.out.println("❌ Error adding customer (Phone number might already exist): " + e.getMessage());
        }
    }

    // --- NEW: Update Khata Balance (Add bill OR Clear due) ---
    public void updateCustomerDue(String phone, double amount, boolean isPayment) {
        // If isPayment is true, subtract from due. If false, it's a new bill, so add to due.
        String query = isPayment 
            ? "UPDATE Customers SET current_due = current_due - ? WHERE phone_number = ?"
            : "UPDATE Customers SET current_due = current_due + ? WHERE phone_number = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, phone);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error updating Khata balance: " + e.getMessage());
        }
    }
    // --- NEW: Manager's Morning Expiry Report ---
    public void printExpiringStockReport() {
        // Joins Products and Stock_Batches to find items expiring within 7 days
        String query = "SELECT p.product_name, s.quantity_available, s.expiry_date, DATEDIFF(s.expiry_date, CURDATE()) as days_left " +
                       "FROM Stock_Batches s " +
                       "JOIN Products p ON s.product_id = p.product_id " +
                       "WHERE s.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                       "ORDER BY days_left ASC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            ResultSet rs = pstmt.executeQuery();
            System.out.println("\n==========================================");
            System.out.println("       🚨 7-DAY EXPIRY ALERT REPORT       ");
            System.out.println("==========================================");
            
            boolean foundExpiringItems = false;
            while (rs.next()) {
                foundExpiringItems = true;
                System.out.println("Item: " + rs.getString("product_name") + 
                                   " | Qty: " + rs.getInt("quantity_available") + 
                                   " | Expires in: " + rs.getInt("days_left") + " days (" + rs.getDate("expiry_date") + ")");
            }
            
            if (!foundExpiringItems) {
                System.out.println("✅ All stock is fresh. Nothing expiring in the next 7 days!");
            }
            System.out.println("==========================================");
            
        } catch (SQLException e) {
            System.out.println("❌ Error generating report: " + e.getMessage());
        }
    }
    // --- NEW: Manager Update Price Feature ---
    public void updateProductPrice(String productName, double newPrice) {
        String query = "UPDATE Products SET base_price = ? WHERE product_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, productName);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Success: Price of '" + productName + "' updated to Rs. " + String.format("%.2f", newPrice));
            } else {
                System.out.println("❌ Error: Product '" + productName + "' not found in the database.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error updating price: " + e.getMessage());
        }
    }
    // --- NEW: Save the Master Bill ---
    public int saveBill(String phoneNumber, double totalAmount, String paymentMode) {
        Integer customerId = null;

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            String custQuery = "SELECT customer_id FROM Customers WHERE phone_number = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(custQuery)) {
                pstmt.setString(1, phoneNumber);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    customerId = rs.getInt("customer_id");
                }
            } catch (SQLException e) {
                System.out.println("❌ Error fetching customer ID: " + e.getMessage());
            }
        }

        String insertQuery = "INSERT INTO Bills (customer_id, total_amount, payment_mode) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            
            if (customerId != null) {
                pstmt.setInt(1, customerId);
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setDouble(2, totalAmount);
            pstmt.setString(3, paymentMode);
            
            pstmt.executeUpdate();
            
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1); // Returns the newly generated bill_id
            }
        } catch (SQLException e) {
            System.out.println("❌ Error saving Bill: " + e.getMessage());
        }
        return -1; 
    }

    // --- NEW: Save individual items linked to the bill ---
    public void saveBillItem(int billId, int productId, int quantity, double itemTotal) {
        // Updated query columns: product_id, quantity_sold, unit_price_charged
        String query = "INSERT INTO Bill_Items (bill_id, product_id, quantity_sold, unit_price_charged) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, billId);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantity);
            
            // If you strictly want the per-unit price saved rather than the total line price:
            // double unitPrice = itemTotal / quantity;
            // pstmt.setDouble(4, unitPrice);
            pstmt.setDouble(4, itemTotal); 
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error saving cart item: " + e.getMessage());
        }
    }
}