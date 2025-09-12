## To start the MongoDB server through Terminal
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

## To start the MongoDB server through MongoDB Compass
Add a new connection with URI
```
mongodb://localhost:<port_number>
```
Click on the connect button next to the port number

## To start the Spring server on <port_number>
1. Run
```
mvn package
```
2. Run the generated jar \
   If using database authentication:
```
   java -jar wildstore-meta/target/wildstore-meta.jar --spring.data.mongodb.uri=mongodb://username:password@localhost:27017/wildfire?authSource=admin --server.port=<port_number> --custom.frontendUrl=<frontend_url> --custom.allowedCorsOrigins=<comma_separated_urls>
```
Without authentication:
```
   java -jar wildstore-meta/target/wildstore-meta.jar --spring.data.mongodb.uri=mongodb://localhost:27017/wildfire --server.port=<port_number> --custom.frontendUrl=<frontend_url> --custom.allowedCorsOrigins=<comma_separated_urls>
```

## To start the File server
```
   java -jar wildstore-fileserve/target/wildstore-fileserve.jar --custom.metadataServer=http://127.0.0.1:<port_number> --server.port=<share_file_port_number>
```

## To start the React dev server
1. Make sure to pass the `--custom.frontendUrl` argument as the react dev server url when starting the spring server or specify it in the properties.yml.
2. Make sure to pass the `--custom.allowedCorsOrigins` argument as a comma-separated list of origin URLs (`--custom.allowedCorsOrigins=http://localhost:3000`) to the spring server or specify it in the properties.yml.
3. Create a .env file under `wildstore-meta/app` with the following contents and the Google Maps API key.

4. Run the following commands
```
cd ./wildstore-meta/app
npm install
npm start
```


Sample .env file
```
REACT_APP_GOOGLE_MAPS_API_KEY = 'YOUR_GOOGLE_MAPS_API_KEY'
```

you are going to need to setup the OAuth2 credentials for Google and GitHub.
you can find detailed instructions here: https://spring.io/guides/tutorials/spring-boot-oauth2/
to setup quick test credentials, go to https://console.cloud.google.com/apis/credentials
create a credential of type OAuth2 Client ID, select application type Web Application,
and add Authorized redirect URIs: `http://localhost:27777/login/oauth2/code/google`
(assuming spring.port is 27777 for the meta-data server.)

Sample properties.yml:
```
spring:
  data:
    mongodb:
      uri: <MONGODB_URI>
  security:
    oauth2:
      client:
        registration:
          github:
            clientId: <CLIENT_ID>
            clientSecret: <CLIENT_SECRET>
          google:
            clientId: <CLIENT_ID>
            clientSecret: <CLIENT_SECRET>
custom:
  fileServer: http://localhost:1000
  frontendUrl: http://localhost:3000
  allowedCorsOrigins: http://localhost:3000,http://localhost:3001
  expireAfterSeconds: 292000
logging:
  level:
    org:
      springframework:
        security: TRACE
wildstore:
    initialAdmins: <ADMIN_EMAIL_ADDRESS>
    backupDirectory: /path/to/backup/directory
```

## To load the frontend and backend together
To load the frontend and backend together on the same port, run `mvn package` and start the server. 

## Running the crawler
```
java -jar wildstore-crawl/target/wildstore-crawl-1.0-SNAPSHOT.jar --metaURL=<server host:port> --tokenFile=</path/to/token.yml> <"all" or "basic"> <path to file with NetCDF filenames or directory containing NetCDF files>
```

## Running the CLI
Search MongoDB document by filename
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar datasetInfo <netcdf file name> --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```
Search based on query
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search <search query> --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```

### Example 1 - Query Variables
1. Query NetCDF variables using `VAR.` prefix followed by the variable name followed by a `.` and the aggregated value: `minValue` or `maxValue` or `average`.
2. Other available query fields for variables include: `varDimensionList`, `attributeList` and `type`.
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search "VAR.RAINC.minValue < 5" --metaURL=http://localhost:8080 --tokenFile=/Users/username/Documents/token.yml
```

### Example 2 - Query Attributes
1. Query NetCDF attributes using `ATTR.` prefix followed by the attribute name followed by a `.` and `value` or `type`.
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search "ATTR.ISURBAN.value = 1" --metaURL=http://localhost:8080 --tokenFile=/Users/username/Documents/token.yml
```

### Example 3 - Query based on Time
Use `TIMESTAMP'yyyy-MM-dd HH:mm:ss'` to query date values.
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search "ATTR.StartDate.value >= TIMESTAMP'2019-06-21 00:00:00'" --metaURL=http://localhost:8080 --tokenFile=/Users/username/Documents/token.yml
```

### Example 4 - Query based on Location
Use `LOCATION` with the `IN` operator to query files within a polygon. Specify the polygon coordinates (double) as a list of tuples within parenthesis.
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search "LOCATION IN ((0.0,0.0), (0.0,10.0), (20.0,20.0))" --metaURL=http://localhost:8080 --tokenFile=/Users/username/Documents/token.yml
```

### Example 5 - Query based on Wind
Query the following VARIABLES: `NorthWind`, `EastWind`, `SouthWind`, `WestWind`, `NorthEastWind`, `SouthEastWind`, `SouthWestWind`, `NorthWestWind`
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search "VAR.NorthEastWind > 100.0" --metaURL=http://localhost:8080 --tokenFile=/Users/username/Documents/token.yml
```

### Example 6
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar search "LOCATION IN ((0.0,0.0), (0.0,10.0), (20.0,20.0)) AND ATTR.ISURBAN.value IN (1,2,3) AND (VAR.FMOIST.minValue >= 10 OR VAR.RH_FIRE.maxValue < 20.0) AND VAR.SouthEastWind.average > 10.0" --metaURL=http://localhost:8080 --tokenFile=/Users/username/Documents/token.yml
```

### Example 7
Regex based matching with LIKE
```
search "<attr/var> LIKE '<regex string>'" <hostname> --offset=0 --limit=100 --tokenFile="<token>"
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

## Generate Share Link via CLI
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar share --email=<email(s) to share with separated by comma> --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```

## Clean Metadata
DRYRUN: To print all metadata documents to be cleaned up,
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar clean --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```
To clean up metadata documents of files that were deleted,
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar clean --no-dryrun --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```

## Clean Share Links
DRYRUN: To print all expired share links,
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar cleanlinks --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```
To clean up all expired share links,
```
java -jar wildstore-cli/target/wildstore-cli-1.0-SNAPSHOT.jar cleanlinks --no-dryrun --metaURL=<server host:port> --tokenFile=</path/to/token.yml>
```

## Restore Collection from Backup
To restore .json backup file to the MongoDB collection,
```
mongoimport --uri=mongodb://localhost:27017/wildstore --collection=<collection_name> <.json file path>
```




