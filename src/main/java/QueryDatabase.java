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
        if (args.length < 2 || !args[0].equals("search")) {
            System.err.println("Usage: java -jar shopdb.jar search <last_name>");
            System.exit(1);
        }
        String lastName = args[1];

        try {
            Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT first_name FROM customers WHERE last_name = ? ORDER BY first_name, id;");
            ps.setString(1, lastName);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String firstName = rs.getString("first_name");
                System.out.println(lastName + " " + firstName);
            }
        } catch (Exception e) {
            System.err.println(e);
        } 
    }

}
