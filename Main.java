import java.util.Scanner;
import java.util.ArrayList; // Added for Cart Memory

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ProductDAO productDAO = new ProductDAO();

        while (true) {
            System.out.println("\n==========================================");
            System.out.println("           SHELFGUARD POS SYSTEM          ");
            System.out.println("==========================================");
            System.out.println("1. Add New Master Product (Manager Setup)");
            System.out.println("2. Update Product Price (Manager Tool)");
            System.out.println("3. Start Customer Checkout (Cashier Billing)");
            System.out.println("4. Open New Customer Khata Account");
            System.out.println("5. Receive Khata Payment (Clear Due Balance)");
            System.out.println("6. Check Expiring Stock (Morning Report)");
            System.out.println("7. Exit System");
            System.out.print("Select an option (1 to 7): ");
            
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                System.out.println("\n--- Master Product Setup ---");
                System.out.print("Enter product name: ");
                String itemName = scanner.nextLine();
                System.out.print("Enter base price (Rs.): ");
                double basePrice = Double.parseDouble(scanner.nextLine());
                System.out.print("Alert if it expires within how many days?: ");
                int alertDays = Integer.parseInt(scanner.nextLine());
                System.out.print("Minimum pieces for a bulk discount? (Enter 0 if none): ");
                int bulkQty = Integer.parseInt(scanner.nextLine());
                double bulkPercent = 0.0;
                if (bulkQty > 0) {
                    System.out.print("Bulk discount percentage? (e.g., 5.0): ");
                    bulkPercent = Double.parseDouble(scanner.nextLine());
                }
                productDAO.addProduct(itemName, basePrice, alertDays, bulkQty, bulkPercent, 0.0);
                Product tempProd = productDAO.getProductByName(itemName);
                if (tempProd != null) {
                    System.out.print("Enter expiry date for current stock (YYYY-MM-DD): ");
                    String expiryDate = scanner.nextLine();
                    productDAO.addStockBatch(tempProd.getProductId(), 10, expiryDate);
                }

            } else if (choice.equals("2")) {
                // --- NEW: Update Price UI ---
                System.out.println("\n--- Update Product Price ---");
                System.out.print("Enter exact Product Name: ");
                String pName = scanner.nextLine();
                System.out.print("Enter New Base Price (Rs.): ");
                double pPrice = Double.parseDouble(scanner.nextLine());
                productDAO.updateProductPrice(pName, pPrice);

            } else if (choice.equals("3")) {
                // --- THE CHECKOUT & TRANSACTION LOGGING LOOP ---
                System.out.println("\n==========================================");
                System.out.println("         CASHIER CHECKOUT TERMINAL        ");
                System.out.println("      (Type 'done' or 'pay' to finish)    ");
                System.out.println("==========================================");
                
                double grandTotal = 0.0;
                int totalItems = 0;
                ArrayList<CartItem> cart = new ArrayList<>(); // Temporary memory for the receipt

                while (true) {
                    System.out.print("\nScan Product Name: ");
                    String searchName = scanner.nextLine();
                    if (searchName.equalsIgnoreCase("done") || searchName.equalsIgnoreCase("pay")) {
                        break; 
                    }
                    Product fetchedItem = productDAO.getProductByName(searchName);
                    if (fetchedItem != null) {
                        System.out.print("Qty for " + fetchedItem.getProductName() + ": ");
                        int quantityBought = Integer.parseInt(scanner.nextLine());
                        int daysUntilExpiry = productDAO.getEarliestExpiryDays(fetchedItem.getProductId());
                        double itemTotal = fetchedItem.calculateFinalPrice(quantityBought, daysUntilExpiry);
                        
                        grandTotal += itemTotal;
                        totalItems += quantityBought;
                        
                        // Save to cart memory
                        cart.add(new CartItem(fetchedItem.getProductId(), fetchedItem.getProductName(), quantityBought, itemTotal));
                        
                        System.out.println("   -> Added to cart | Item Total: Rs. " + String.format("%.2f", itemTotal));
                    } else {
                        System.out.println("❌ Product not found. Try again.");
                    }
                }

                if (cart.isEmpty()) {
                    System.out.println("Cart is empty. Cancelling checkout.");
                    continue; 
                }

                System.out.println("\n==========================================");
                System.out.println("             FINAL RECEIPT                ");
                System.out.println("==========================================");
                for (CartItem item : cart) {
                    System.out.println(item.getQuantity() + "x " + item.getProductName() + " - Rs. " + String.format("%.2f", item.getItemTotal()));
                }
                System.out.println("------------------------------------------");
                System.out.println("Total Items Bought : " + totalItems);
                System.out.println("GRAND TOTAL        : Rs. " + String.format("%.2f", grandTotal));
                System.out.println("==========================================");

                System.out.println("\nPayment Method: (1) Cash/UPI  (2) Store Credit Account");
                System.out.print("Select payment option: ");
                String payOption = scanner.nextLine();

                boolean paymentSuccessful = false;
                String finalPhone = null;
                String finalPayMode = "CASH";

                if (payOption.equals("2")) {
                    finalPayMode = "CREDIT";
                    System.out.print("Enter Customer Phone Number: ");
                    finalPhone = scanner.nextLine();
                    double[] creditInfo = productDAO.getCustomerCreditDetails(finalPhone);
                    
                    if (creditInfo == null) {
                        System.out.println("❌ Customer not found. Transaction cancelled.");
                    } else {
                        double availableCredit = creditInfo[0] - creditInfo[1];
                        try {
                            if (grandTotal > availableCredit) {
                                double amountToClear = grandTotal - availableCredit;
                                throw new CreditLimitExceededException("Limit Exceeded! Customer must pay at least Rs. " + String.format("%.2f", amountToClear) + " in cash right now.");
                            }
                            System.out.println("✅ Payment approved on credit!");
                            productDAO.updateCustomerDue(finalPhone, grandTotal, false); 
                            paymentSuccessful = true;
                        } catch (CreditLimitExceededException e) {
                            System.out.println("🚨 ALERT: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("✅ Payment received via Cash/UPI. Thank you!");
                    paymentSuccessful = true;
                }
                
                // --- TRANSACTION LOGGING TO MYSQL ---
                if (paymentSuccessful) {
                    int newBillId = productDAO.saveBill(finalPhone, grandTotal, finalPayMode);
                    if (newBillId != -1) {
                        for (CartItem item : cart) {
                            productDAO.saveBillItem(newBillId, item.getProductId(), item.getQuantity(), item.getItemTotal());
                        }
                        System.out.println("💾 Bill #" + newBillId + " saved to database securely.");
                    }
                }
                
            } else if (choice.equals("4")) {
                System.out.println("\n--- Open Customer Khata Account ---");
                System.out.print("Enter Customer Phone Number: ");
                String phone = scanner.nextLine();
                System.out.print("Enter Customer Name: ");
                String name = scanner.nextLine();
                System.out.print("Enter Max Credit Limit (Rs.): ");
                double limit = Double.parseDouble(scanner.nextLine());
                productDAO.addCustomer(phone, name, limit);
                
            } else if (choice.equals("5")) {
                System.out.println("\n--- Receive Khata Payment ---");
                System.out.print("Enter Customer Phone Number: ");
                String phone = scanner.nextLine();
                double[] creditInfo = productDAO.getCustomerCreditDetails(phone);
                if (creditInfo == null) {
                    System.out.println("❌ Customer not found.");
                } else {
                    System.out.println("-> Customer's Current Unpaid Due: Rs. " + String.format("%.2f", creditInfo[1]));
                    System.out.print("Enter amount being paid (Rs.): ");
                    double paymentAmt = Double.parseDouble(scanner.nextLine());
                    productDAO.updateCustomerDue(phone, paymentAmt, true);
                    System.out.println("✅ Payment processed! Dues updated.");
                }
                
            } else if (choice.equals("6")) {
                productDAO.printExpiringStockReport();
                
            } else if (choice.equals("7")) {
                System.out.println("Shutting down ShelfGuard POS... Have a great day!");
                break; 
            } else {
                System.out.println("❌ Invalid option. Please choose a number from 1 to 7.");
            }
        }
        scanner.close();
    }
}