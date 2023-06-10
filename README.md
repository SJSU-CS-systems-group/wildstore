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
