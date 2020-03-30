1. Build (with Maven):
```
mvn package
```

2. Create tables in database and populate them with test data:
```
psql --file=dump.sql <dbname>
```

3. Edit connection information in `database.properties`

4. Start (with test data):
```
java -jar target\shopdb.jar search input-search.json output-search.json
java -jar target\shopdb.jar stat input-stat.json output-stat.json
```
