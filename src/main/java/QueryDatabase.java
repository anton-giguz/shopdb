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
        JsonReader reader = Json.createReader(Files.newBufferedReader(Paths.get(filename)));
        JsonValue criterias = reader.readObject().get("criterias");
        reader.close();

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


    private static void writeJson(String filename, JsonObject object) throws IOException {
        JsonWriter writer = Json.createWriter(Files.newBufferedWriter(Paths.get(filename)));
        writer.writeObject(object);
        writer.close();
    }


    public static void main(String[] args) {
        if (args.length < 3 || !args[0].equals("search")) {
            System.err.println("Usage: java -jar shopdb.jar search <input>.json <output>.json");
            System.exit(1);
        }
        String inputJson  = args[1];
        String outputJson = args[2];

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("type", "search");
        JsonArrayBuilder results = Json.createArrayBuilder();

        try {
            Connection connection = getConnection();

            for (JsonValue criteria : getCriteriaArray(inputJson)) {
                PreparedStatement ps = null;
                JsonObject item = getCriteriaItem(criteria);
                Set<String> keys = item.keySet();

                JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
                resultBuilder.add("criteria", criteria);
                JsonArrayBuilder rows = Json.createArrayBuilder();

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

                } else if (keys.contains("minExpenses") && keys.contains("maxExpenses") && keys.size() == 2) {
                    int minExpenses = getInt(item, "minExpenses");
                    int maxExpenses = getInt(item, "maxExpenses");
                    ps = connection.prepareStatement("SELECT last_name, first_name " +
                        "FROM customers JOIN purchases ON (customers.id = purchases.customer) JOIN products ON (purchases.product = products.id) " +
                        "GROUP BY last_name, first_name, customers.id " +
                        "HAVING sum(products.price) BETWEEN ? AND ? ORDER BY last_name, first_name, customers.id;");
                    ps.setInt(1, minExpenses);
                    ps.setInt(2, maxExpenses);

                } else if (keys.contains("badCustomers") && keys.size() == 1) {
                    int badCustomers = getInt(item, "badCustomers");
                    ps = connection.prepareStatement("SELECT last_name, first_name " +
                        "FROM customers JOIN purchases ON (customers.id = purchases.customer) " +
                        "GROUP BY last_name, first_name, customers.id " +
                        "ORDER BY count(*), last_name, first_name, customers.id LIMIT ?;");
                    ps.setInt(1, badCustomers);

                } else {
                    throw new CriteriaException("Unknown criteria");
                }

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    JsonObjectBuilder rowBuilder = Json.createObjectBuilder();
                    rowBuilder.add("lastName",  rs.getString("last_name"));
                    rowBuilder.add("firstName", rs.getString("first_name"));
                    rows.add(rowBuilder);
                }

                resultBuilder.add("results", rows);
                results.add(resultBuilder);
            }

            builder.add("results", results);
        } catch (Exception e) {
            builder = Json.createObjectBuilder();
            builder.add("type", "error");
            builder.add("message", e.toString());
        }

        try {
            writeJson(outputJson, builder.build());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } 
    }

}
