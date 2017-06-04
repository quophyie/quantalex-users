# Quantal Exchange Users

This microservice manages users in Quantal Exchange
## base package name


In your POM, update the **`artifactId`** and **`name`** to values of your choice

Refactor the base package name (i.e. **`com.quantal.quantalmicroservicetemplate`**) to a name of your choice
 
## Environment Variables
 - **`PASSWORD_SALT`** - The salt that is used to hash passwords. 
 This is a required env variables 
 
 - **`JWT_SECRET`** - The JWT secret that is used to sign JWTs 
 
 - **`JWT_TYPE`** - The JWT type that is set in the JWT header - default should be **`JWT`** 
 
 - **`JWT_ALGORITHM`** - The JWT header that is set in the JWT header - default should be **`HS256`**

## application.properties  (located at src/main/java/resource)
  
  Change the following to the correct values
  
 - **`spring.datasource.url`**
 - **`spring.datasource.url`**
 - **`spring.datasource.username`**
 - **` spring.datasource.platform`**
 - **` spring.datasource.initialize`**
 - **`flyway.url`**
 - **`flyway.user`**
 
## JpaStartupConfig.java  (/src/main/java/com/quantal/quantalmicroservicetemplate/config/jpa/) 

 - Change the base package in the annotation **`@EnableJpaRepositories`** to your desired package
 - Change the base package in the annotation **`@EntityScan`** to your desired package
 - Change the base package in the method call to  **`factory.setPackagesToScan("com.quantal.quantalmicroservicetemplate.models")`** to your desired package
 
## SharedConfig.java (/src/main/java/com/quantal/quantalmicroservicetemplate/config/jpa/) 

 This contains bean configuratons that do not belong in either **`JpaStartupConfig.java`**
 **`ApiConfig.java`** and  **`WebStartupConfig.java`**

## resources/db/migration
 This is a **`required directory`**.  This contains SQL for both migrations and seed files
