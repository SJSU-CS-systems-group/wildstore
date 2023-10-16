## To start the MongoDB server
With authentication
```
sudo mongod --dbpath <dbpath> --port <mongo_port_number> --auth
```
Without authentication
```
sudo mongod --dbpath <dbpath> --port <mongo_port_number>
```
Create admin user for auth
```
$ mongosh "mongodb://localhost:27017"

> use admin
> db.createUser(
{
    user: "user",
    pwd: "pwd",
    roles: [ "root" ]
})
```

## To start the Spring server on <port_number>
1. Run
```
mvn package
```
2. Run the generated jar \
   If using database authentication:
```
   java -jar wildfirestorage-spring/target/wildfirestorage-spring.jar --spring.data.mongodb.uri=mongodb://username:password@localhost:27017/wildfire?authSource=admin --server.port=<port_number>
```
Without authentication:
```
   java -jar wildfirestorage-spring/target/wildfirestorage-spring.jar --spring.data.mongodb.uri=mongodb://localhost:27017/wildfire --server.port=<port_number>
```

## Running the crawler
```
java -jar wildfirestorage-core/target/wildfirestorage-core-1.0-SNAPSHOT.jar --hostname=<optional server host:port> <"all" or "basic"> <path of NetCDF file>
```

## Running the CLI
Search MongoDB document by filename
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar datasetInfo <netcdf file name> <hostname>
```
Search based on query
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar search <search query> <hostname>
```

### Example 1 - Query Variables
1. Query NetCDF variables using `VAR.` prefix followed by the variable name followed by a `.` and the aggregated value: `minValue` or `maxValue` or `average`.
2. Other available query fields for variables include: `varDimensionList`, `attributeList` and `type`.
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar search "VAR.RAINC.minValue < 5" http://localhost:8080
```

### Example 2 - Query Attributes
1. Query NetCDF attributes using `ATTR.` prefix followed by the attribute name followed by a `.` and `value` or `type`.
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar search "ATTR.ISURBAN.value = 1" http://localhost:8080
```

### Example 3 - Query based on Time
Use `TIMESTAMP'yyyy-MM-dd HH:mm:ss'` to query date values.
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar search "ATTR.StartDate.value >= TIMESTAMP'2019-06-21 00:00:00'" http://localhost:8080
```

### Example 4 - Query based on Location
Use `LOCATION` with the `IN` operator to query files within a polygon. Specify the polygon coordinates (double) as a list of tuples within parenthesis.
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar search "LOCATION IN ((0.0,0.0), (0.0,10.0), (20.0,20.0))" http://localhost:8080
```

### Example 5
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar search "LOCATION IN ((0.0,0.0), (0.0,10.0), (20.0,20.0)) AND ATTR.ISURBAN.value IN (1,2,3) AND (VAR.FMOIST.minValue >= 10 OR VAR.RH_FIRE.maxValue < 20.0)" http://localhost:8080
```

### Supported Query Operators
```
 AND
 OR
 =
 !=
 <>
 >
 >=
 <
 <=
 IN
```

## Clean
To clean up metadata documents of files that were deleted,
```
java -jar wildfirestorage-cli/target/wildfirestorage-cli-1.0-SNAPSHOT.jar clean <LIMIT: Number of records to delete at a time> <hostname>
```

## Gateway
To start the gateway server,
```
java -jar wildfirestorage-gateway/target/wildfirestorage-gateway-1.0-SNAPSHOT.jar --server.port=<PORT>  --spring.config.location=<PATH_TO_application.yml>
```

### Sample application.yml for gateway
```
spring:
  cloud:
    gateway:
      routes:
        - id: route_metadata
          uri: http://localhost:8080
          predicates:
            - Path=/api/metadata*
        - id: route_files
          uri: http://localhost:100
          predicates:
            - Path=/api/file/*

```
