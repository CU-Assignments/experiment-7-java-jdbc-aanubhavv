import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EmployeeDataFetcher {
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/databases";
    private static final String USER = "root";
    private static final String PASSWORD = "anubhav@123";

    public static void main(String[] args) {
        // Load the MySQL driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            return;
        }

        // Try-with-resources to ensure resources are closed automatically
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            
            System.out.println("Connected to the database successfully!");
            
            // SQL query to retrieve all employee records
            String sql = "SELECT EmpID, Name, Salary FROM employees";
            
            // Execute query and get result set
            ResultSet resultSet = statement.executeQuery(sql);
            
            // Display table header
            System.out.println("\nEmployee Records:");
            System.out.println("+--------+--------------------+------------+");
            System.out.println("| EmplID | Name               | Salary     |");
            System.out.println("+--------+--------------------+------------+");
            
            // Process the result set
            while (resultSet.next()) {
                int emplId = resultSet.getInt("EmpID");
                String name = resultSet.getString("Name");
                double salary = resultSet.getDouble("Salary");
                
                // Format and display each record
                System.out.printf("| %-6d | %-18s | $%-9.2f |\n", emplId, name, salary);
            }
            
            System.out.println("+--------+--------------------+------------+");
            
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}