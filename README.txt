# ShelfGuard POS System

## Project Overview
ShelfGuard is a terminal-based Point of Sale (POS) and inventory management system built with Java and MySQL. It is designed to handle daily retail operations, process transactions, manage customer micro-credit (Khata) accounts, and proactively track expiring stock.

## Core Technical Features
* **Object-Oriented Architecture:** Utilizes the DAO (Data Access Object) design pattern to separate business logic from database operations, utilizing custom POJOs (`Product`, `CartItem`).
* **Custom Exception Handling:** Implements a custom `CreditLimitExceededException` to calculate live dues versus maximum credit limits, preventing unauthorized transactions.
* **Relational Database Design:** Employs a One-to-Many relational structure (`Bills` to `Bill_Items`) utilizing `Statement.RETURN_GENERATED_KEYS` to log transaction details accurately.
* **Data Integrity:** Enforces `UNIQUE` SQL constraints to prevent duplicate inventory entries.
* **Advanced SQL Queries:** Generates automated daily expiry reports using `JOIN` statements and MySQL temporal functions (`CURDATE()`, `DATEDIFF`).

## Setup Instructions for Evaluators

### 1. Database Setup
1. Open MySQL Workbench.
2. Open the provided `ShelfGuard_Database.sql` file.
3. Execute the SQL script. This will automatically create the `ShelfGuardDB` schema, build all required tables (`Products`, `Customers`, `Bills`, `Bill_Items`, `Stock_Batches`), and insert sample data.

### 2. Application Setup
1. Open the project folder in your preferred Java IDE (VS Code, Eclipse, IntelliJ).
2. Open `DBConnection.java`.
3. Update the `USER` and `PASSWORD` variables to match your local MySQL credentials.
4. Ensure the MySQL JDBC Connector (`mysql-connector-j.jar`) is added to your project's build path/referenced libraries.

### 3. Execution
Run `Main.java` to start the POS terminal interface. 

**Testing the System:**
* Select **Option 6** to view the dynamic SQL JOIN query that calculates expiring stock.
* Select **Option 3** to run a cashier checkout. Type the name of a product (e.g., "Horlicks"), enter the quantity, and type "pay". 
* Process the payment via Cash or Credit to view the database transaction logging in real-time.