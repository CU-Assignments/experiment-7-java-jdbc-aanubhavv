import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ProductManager {
    // Database credentials
    private static final String URL = "jdbc:mysql://localhost:3306/databases";
    private static final String USER = "root";
    private static final String PASSWORD = "anubhav@123";
    
    private static Connection connection = null;
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        try {
            // Register JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Open a connection to the schema
            System.out.println("Connecting to databases schema...");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            
            // Create the products table if it doesn't exist
            createProductsTable();
            
            // Disable auto-commit to enable transaction control
            connection.setAutoCommit(false);
            
            System.out.println("Connected to databases schema. Products table is ready.");
            
            boolean exit = false;
            while (!exit) {
                displayMenu();
                int choice = getIntInput("Enter your choice: ");
                
                switch (choice) {
                    case 1:
                        createProduct();
                        break;
                    case 2:
                        readAllProducts();
                        break;
                    case 3:
                        updateProduct();
                        break;
                    case 4:
                        deleteProduct();
                        break;
                    case 5:
                        searchProduct();
                        break;
                    case 6:
                        exit = true;
                        System.out.println("Exiting program...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                    System.out.println("Database connection closed.");
                }
                if (scanner != null) {
                    scanner.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    private static void createProductsTable() {
        Statement stmt = null;
        
        try {
            stmt = connection.createStatement();
            
            // Create the products table if it doesn't exist
            String sql = "CREATE TABLE IF NOT EXISTS products (" +
                         "ProductID INT AUTO_INCREMENT PRIMARY KEY, " +
                         "ProductName VARCHAR(100) NOT NULL, " +
                         "Price DECIMAL(10, 2) NOT NULL, " +
                         "Quantity INT NOT NULL)";
            
            stmt.executeUpdate(sql);
            System.out.println("Products table is ready.");
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(stmt);
        }
    }
    
    private static void displayMenu() {
        System.out.println("\n=== PRODUCT MANAGEMENT SYSTEM ===");
        System.out.println("1. Add New Product");
        System.out.println("2. View All Products");
        System.out.println("3. Update Product");
        System.out.println("4. Delete Product");
        System.out.println("5. Search Product by ID");
        System.out.println("6. Exit");
        System.out.println("================================");
    }
    
    private static void createProduct() {
        System.out.println("\n-- ADD NEW PRODUCT --");
        String productName = getStringInput("Enter product name: ");
        double price = getDoubleInput("Enter price: ");
        int quantity = getIntInput("Enter quantity: ");
        
        PreparedStatement pstmt = null;
        
        try {
            String sql = "INSERT INTO products (ProductName, Price, Quantity) VALUES (?, ?, ?)";
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, productName);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, quantity);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                connection.commit();
                System.out.println("Product added successfully!");
            } else {
                connection.rollback();
                System.out.println("Failed to add product.");
            }
            
        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Transaction rolled back due to error.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            closeStatement(pstmt);
        }
    }
    
    private static void readAllProducts() {
        System.out.println("\n-- ALL PRODUCTS --");
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = connection.createStatement();
            String sql = "SELECT * FROM products";
            rs = stmt.executeQuery(sql);
            
            displayProductHeader();
            
            boolean hasProducts = false;
            while (rs.next()) {
                hasProducts = true;
                int id = rs.getInt("ProductID");
                String name = rs.getString("ProductName");
                double price = rs.getDouble("Price");
                int quantity = rs.getInt("Quantity");
                
                System.out.printf("%-10d %-30s $%-10.2f %-10d\n", id, name, price, quantity);
            }
            
            if (!hasProducts) {
                System.out.println("No products found in database.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
        }
    }
    
    private static void updateProduct() {
        System.out.println("\n-- UPDATE PRODUCT --");
        int productId = getIntInput("Enter product ID to update: ");
        
        // First check if product exists
        if (!productExists(productId)) {
            System.out.println("Product with ID " + productId + " does not exist.");
            return;
        }
        
        System.out.println("Enter new details (leave blank to keep current value):");
        String productName = getStringInputOrEmpty("Enter new product name: ");
        String priceStr = getStringInputOrEmpty("Enter new price: ");
        String quantityStr = getStringInputOrEmpty("Enter new quantity: ");
        
        PreparedStatement pstmt = null;
        
        try {
            StringBuilder sqlBuilder = new StringBuilder("UPDATE products SET ");
            boolean hasUpdates = false;
            
            if (!productName.isEmpty()) {
                sqlBuilder.append("ProductName = ?");
                hasUpdates = true;
            }
            
            if (!priceStr.isEmpty()) {
                if (hasUpdates) sqlBuilder.append(", ");
                sqlBuilder.append("Price = ?");
                hasUpdates = true;
            }
            
            if (!quantityStr.isEmpty()) {
                if (hasUpdates) sqlBuilder.append(", ");
                sqlBuilder.append("Quantity = ?");
                hasUpdates = true;
            }
            
            if (!hasUpdates) {
                System.out.println("No updates provided. Operation cancelled.");
                return;
            }
            
            sqlBuilder.append(" WHERE ProductID = ?");
            String sql = sqlBuilder.toString();
            pstmt = connection.prepareStatement(sql);
            
            int paramIndex = 1;
            
            if (!productName.isEmpty()) {
                pstmt.setString(paramIndex++, productName);
            }
            
            if (!priceStr.isEmpty()) {
                try {
                    double price = Double.parseDouble(priceStr);
                    pstmt.setDouble(paramIndex++, price);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price format. Update cancelled.");
                    return;
                }
            }
            
            if (!quantityStr.isEmpty()) {
                try {
                    int quantity = Integer.parseInt(quantityStr);
                    pstmt.setInt(paramIndex++, quantity);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid quantity format. Update cancelled.");
                    return;
                }
            }
            
            pstmt.setInt(paramIndex, productId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                connection.commit();
                System.out.println("Product updated successfully!");
            } else {
                connection.rollback();
                System.out.println("Failed to update product.");
            }
            
        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Transaction rolled back due to error.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            closeStatement(pstmt);
        }
    }
    
    private static void deleteProduct() {
        System.out.println("\n-- DELETE PRODUCT --");
        int productId = getIntInput("Enter product ID to delete: ");
        
        PreparedStatement pstmt = null;
        
        try {
            // First check if product exists
            if (!productExists(productId)) {
                System.out.println("Product with ID " + productId + " does not exist.");
                return;
            }
            
            // Get confirmation before deleting
            System.out.print("Are you sure you want to delete this product? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("y") && !confirm.equals("yes")) {
                System.out.println("Deletion cancelled.");
                return;
            }
            
            String sql = "DELETE FROM products WHERE ProductID = ?";
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, productId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                connection.commit();
                System.out.println("Product deleted successfully!");
            } else {
                connection.rollback();
                System.out.println("Failed to delete product.");
            }
            
        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Transaction rolled back due to error.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            closeStatement(pstmt);
        }
    }
    
    private static void searchProduct() {
        System.out.println("\n-- SEARCH PRODUCT --");
        int productId = getIntInput("Enter product ID to search: ");
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            String sql = "SELECT * FROM products WHERE ProductID = ?";
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, productId);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                displayProductHeader();
                int id = rs.getInt("ProductID");
                String name = rs.getString("ProductName");
                double price = rs.getDouble("Price");
                int quantity = rs.getInt("Quantity");
                
                System.out.printf("%-10d %-30s $%-10.2f %-10d\n", id, name, price, quantity);
            } else {
                System.out.println("No product found with ID: " + productId);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            closeStatement(pstmt);
        }
    }
    
    private static boolean productExists(int productId) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean exists = false;
        
        try {
            String sql = "SELECT 1 FROM products WHERE ProductID = ?";
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, productId);
            
            rs = pstmt.executeQuery();
            exists = rs.next();
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            closeStatement(pstmt);
        }
        
        return exists;
    }
    
    private static void displayProductHeader() {
        System.out.println("-----------------------------------------------------------------");
        System.out.printf("%-10s %-30s %-11s %-10s\n", "ID", "Product Name", "Price", "Quantity");
        System.out.println("-----------------------------------------------------------------");
    }
    
    private static int getIntInput(String prompt) {
        int input = 0;
        boolean valid = false;
        
        while (!valid) {
            try {
                System.out.print(prompt);
                input = Integer.parseInt(scanner.nextLine().trim());
                valid = true;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        
        return input;
    }
    
    private static double getDoubleInput(String prompt) {
        double input = 0.0;
        boolean valid = false;
        
        while (!valid) {
            try {
                System.out.print(prompt);
                input = Double.parseDouble(scanner.nextLine().trim());
                valid = true;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        
        return input;
    }
    
    private static String getStringInput(String prompt) {
        String input = "";
        boolean valid = false;
        
        while (!valid) {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            
            if (!input.isEmpty()) {
                valid = true;
            } else {
                System.out.println("Input cannot be empty.");
            }
        }
        
        return input;
    }
    
    private static String getStringInputOrEmpty(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private static void closeStatement(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}