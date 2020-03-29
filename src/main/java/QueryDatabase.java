import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;


public class QueryDatabase {

    private static Connection getConnection() throws IOException, SQLException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(Paths.get("database.properties")));

        String url      = properties.getProperty("url");
        String user     = properties.getProperty("user");
        String password = properties.getProperty("password");

        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) {
        try {
            Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT last_name, first_name FROM customers ORDER BY last_name, first_name, id;");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String lastName  = rs.getString("last_name");
                String firstName = rs.getString("first_name");
                System.out.println(lastName + " " + firstName);
            }
        } catch (Exception e) {
            System.err.println(e);
        } 
    }

}
