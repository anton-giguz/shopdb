import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import javax.json.*;


public class QueryDatabase {

    private static String command;
    private static String inputJson;
    private static String outputJson;
    private static JsonObjectBuilder builder;
    private static Connection connection;
    private static SimpleDateFormat format;
    private static Date startDate;
    private static Date endDate;

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


    private static JsonArray getCriteriaArray() throws IOException, CriteriaException {
        JsonReader reader = Json.createReader(Files.newBufferedReader(Paths.get(inputJson)));
        JsonObject object = reader.readObject();
        JsonValue criterias = object.get("criterias");
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


    private static void writeJson() throws IOException {
        JsonWriter writer = Json.createWriter(Files.newBufferedWriter(Paths.get(outputJson)));
        writer.writeObject(builder.build());
        writer.close();
    }


    private static void doSearch() throws IOException, SQLException, CriteriaException {
        JsonArrayBuilder results = Json.createArrayBuilder();

        for (JsonValue criteria : getCriteriaArray()) {
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
    }


    private static void readDates() throws IOException, CriteriaException {
        JsonReader reader = Json.createReader(Files.newBufferedReader(Paths.get(inputJson)));
        JsonObject object = reader.readObject();
        JsonValue startValue = object.get("startDate");
        JsonValue endValue   = object.get("endDate");
        reader.close();

        if (startValue == null || startValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new CriteriaException("No start date");
        }
        if (endValue   == null || endValue.getValueType()   != JsonValue.ValueType.STRING) {
            throw new CriteriaException("No end date");
        }

        format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            startDate = format.parse( ((JsonString)startValue).getString() );
            endDate   = format.parse( ((JsonString)endValue  ).getString() );
        } catch (ParseException e) {
            throw new CriteriaException("Wrong date format: " + e.getMessage());
        }

        if (startDate.after(endDate)) {
            throw new CriteriaException("Start date is greater than end date");
        }
    }


    private static void doStat() throws IOException, SQLException, CriteriaException {
        readDates();

        PreparedStatement ps = connection.prepareStatement("SELECT count(*) AS total_days " +
            "FROM generate_series(?::timestamp, ?::timestamp, '1 day'::interval) AS day WHERE extract(isodow from day) < 6");
        ps.setString(1, format.format(startDate));
        ps.setString(2, format.format(endDate));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            builder.add("totalDays", rs.getInt("total_days"));
        }

        JsonArrayBuilder customers = Json.createArrayBuilder();

        ps = connection.prepareStatement("SELECT last_name, first_name, sum(products.price) AS total_expenses " +
            "FROM customers JOIN purchases ON (customers.id = purchases.customer) JOIN products ON (purchases.product = products.id) " +
            "WHERE purchase_date BETWEEN ?::date AND ?::date AND extract(isodow from purchase_date) < 6 GROUP BY last_name, first_name, customers.id " +
            "ORDER BY total_expenses DESC, last_name, first_name, customers.id;");
        ps.setString(1, format.format(startDate));
        ps.setString(2, format.format(endDate));
        rs = ps.executeQuery();
        while (rs.next()) {
            JsonObjectBuilder customer = Json.createObjectBuilder();
            customer.add("name", rs.getString("last_name") + " " + rs.getString("first_name"));
            customer.add("totalExpenses", rs.getInt("total_expenses"));
            customers.add(customer);
        }

        builder.add("customers", customers);
    }


    public static void main(String[] args) {
        if (args.length < 3 || !args[0].equals("search") && !args[0].equals("stat")) {
            System.err.println("Usage:");
            System.err.println("java -jar shopdb.jar search <input>.json <output>.json");
            System.err.println("or");
            System.err.println("java -jar shopdb.jar stat <input>.json <output>.json");
            System.exit(1);
        }
        command    = args[0];
        inputJson  = args[1];
        outputJson = args[2];

        builder = Json.createObjectBuilder();
        builder.add("type", command);

        try {
            connection = getConnection();

            if (command.equals("search")) {
                doSearch();
            } else if (command.equals("stat")) {
                doStat();
            }
        } catch (Exception e) {
            builder = Json.createObjectBuilder();
            builder.add("type", "error");
            builder.add("message", e.toString());
        }

        try {
            writeJson();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } 
    }

}
