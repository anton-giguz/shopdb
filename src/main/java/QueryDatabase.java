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


    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equals("search")) {
            System.err.println("Usage: java -jar shopdb.jar search <input>.json");
            System.exit(1);
        }
        String inputJson = args[1];

        try {
            Connection connection = getConnection();
            for (JsonValue criteria : getCriteriaArray(inputJson)) {
                JsonObject item = getCriteriaItem(criteria);
                Set<String> keys = item.keySet();
                if (keys.contains("lastName") && keys.size() == 1) {
                    String lastName = item.getString("lastName");
                    PreparedStatement ps = connection.prepareStatement("SELECT first_name FROM customers WHERE last_name = ? ORDER BY first_name, id;");
                    ps.setString(1, lastName);

                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        String firstName = rs.getString("first_name");
                        System.out.println(lastName + " " + firstName);
                    }
                } else {
                    throw new CriteriaException("Unknown criteria");
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        } 
    }

}
