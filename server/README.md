Configuration
=============
Suggested JDK version: 11

The following environment variables must be set:
* AWS_ACCESS_KEY_ID
* AWS_SECRET_ACCESS_KEY
* ELASTIC_SEARCH_HOST
* ELASTIC_SEARCH_INDEX
* COMMON_CRAWL_FILENAME (Upload Data needed)

To Upload Data
==============
```
$ mvn package exec:java -Dexec.mainClass="search.expose.App"
```

To Run Server
=============
```
$ mvn clean package tomcat7:run
```
or to use Tomcat 8.5:
```
$ mvn clean package cargo:run
```

References
==========
https://www.mkyong.com/webservices/jax-rs/jersey-hello-world-example/
