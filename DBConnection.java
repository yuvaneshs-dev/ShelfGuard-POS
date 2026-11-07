import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/ShelfGuardDB";
    private static final String USER = "root";
    private static final String PASSWORD = "Yuvanesh8e,()"; // Change to your MySQL password if any

    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[SUCCESS] Success! Connected to ShelfGuardDB MySQL database.");
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] MySQL JDBC Driver not found in your project! " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[ERROR] Database Connection Failed! Check your username/password. " + e.getMessage());
        }
        return conn;
    }

    // Quick test main method
    public static void main(String[] args) {
        getConnection();
    }
}
