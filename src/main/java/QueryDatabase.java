import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;
import java.util.Set;
import javax.json.*;


public class QueryDatabase {

    private static class CriteriaException extends Exception {
        public CriteriaException(String message) {
            super(message);
        }

        public String toString() {
            return getMessage();
        }
    }


    private static Connection getConnection() throws IOException, SQLException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(Paths.get("database.properties")));

        String url      = properties.getProperty("url");
        String user     = properties.getProperty("user");
        String password = properties.getProperty("password");

        return DriverManager.getConnection(url, user, password);
    }


    private static JsonArray getCriteriaArray(String filename) throws IOException, CriteriaException {
        JsonValue criterias = Json.createReader(Files.newBufferedReader(Paths.get(filename))).readObject().get("criterias");
        if (criterias == null || criterias.getValueType() != JsonValue.ValueType.ARRAY || ((JsonArray)criterias).isEmpty()) {
            throw new CriteriaException("No criterias");
        }
        return (JsonArray)criterias;
    }


    private static JsonObject getCriteriaItem(JsonValue criteria) throws CriteriaException {
        if (criteria.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new CriteriaException("Wrong criteria format");
        }
        return (JsonObject)criteria;
    }


    private static String getString(JsonObject item, String key) throws CriteriaException {
        JsonValue value = item.get(key);
        if (value == null || value.getValueType() != JsonValue.ValueType.STRING) {
            throw new CriteriaException("Wrong string format");
        }
        return item.getString(key);
    }


    private static int getInt(JsonObject item, String key) throws CriteriaException {
        JsonValue value = item.get(key);
        if (value == null || value.getValueType() != JsonValue.ValueType.NUMBER) {
            throw new CriteriaException("Wrong number format");
        }
        return item.getInt(key);
    }


    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equals("search")) {
            System.err.println("Usage: java -jar shopdb.jar search <input>.json");
            System.exit(1);
        }
        String inputJson = args[1];

        try {
            Connection connection = getConnection();

            for (JsonValue criteria : getCriteriaArray(inputJson)) {
                PreparedStatement ps = null;
                JsonObject item = getCriteriaItem(criteria);
                Set<String> keys = item.keySet();

                if (keys.contains("lastName") && keys.size() == 1) {
                    String lastName = getString(item, "lastName");
                    ps = connection.prepareStatement("SELECT last_name, first_name " +
                        "FROM customers WHERE last_name = ? ORDER BY last_name, first_name, id;");
                    ps.setString(1, lastName);
                } else if (keys.contains("productName") && keys.contains("minTimes") && keys.size() == 2) {
                    String productName = getString(item, "productName");
                    int minTimes = getInt(item, "minTimes");
                    ps = connection.prepareStatement("SELECT last_name, first_name " +
                        "FROM customers JOIN purchases ON (customers.id = purchases.customer) JOIN products ON (purchases.product = products.id) " +
                        "WHERE products.name = ? GROUP BY last_name, first_name, customers.id " +
                        "HAVING count(*) >= ? ORDER BY last_name, first_name, customers.id;");
                    ps.setString(1, productName);
                    ps.setInt(2, minTimes);
                } else {
                    throw new CriteriaException("Unknown criteria");
                }

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String lastName  = rs.getString("last_name");
                    String firstName = rs.getString("first_name");
                    System.out.println(lastName + " " + firstName);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        } 
    }

}
