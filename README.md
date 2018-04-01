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
 
 - **`API_GATEWAY_ENDPOINT`** - The base url of the api gateway - default should be **`http://localhost:8001`**
 
 - **`AUTHORIZATION_SERVICE_ENDPOINT`** - The base end point for the auth microservice - e.g. **`http://localhost:3000`**
 
 - **`EMAIL_SERVICE_ENDPOINT`** - The base end point for the to microservice - e.g. **`http://localhost:3000`**
 
 - **`LOGZIO_TOKEN`** - The [Logz.io](http://logz.io) token to use to send logs to [Logz.io](http://logz.io)
 

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

## Configuring the Quantal Logger
 The **`Quantal Logger`** wraps around the  [GoDaddy Logger](https://github.com/godaddy/godaddy-logger) and the [Logzio Java Sender](https://github.com/logzio/logzio-java-sender)
 and makes simple it for us to create structured logs that are sent to [Logz.io](http://logz.io). Among other things, the **`Quantal Logger`**  allows you to 
 define common log fields that should be included in all logs and also define fields that are required in all logs before the log is considered
 a valid log line (**`traceId`** and **`event`** by default).The **`LoggerConfig`** object is used to set configurations for the logger. For example 
 if you do not want the **`traceId`** and **`event`** as required log fields in every log, you can set  **`eventIsRequired`** and **`traceIdIsRequired`** to `false`. As another example if you wanted tp configure
 other custom required fields, you can add these new custom required fields to the **`requiredLogFields`** map on the **`LoggerConfig`** object.  The [GoDaddy Logger](https://github.com/godaddy/godaddy-logger) is itself an 
 implementation of **`org.slf4j.Logger`**. You can configure the **`Quantal Logger`** manually using the **`com.quantal.javashared.logger.QuantalLoggerFactory`** for  each class.
 However, you create the configuration that is used globally by the  **`Quantal Logger`** and then
 use  **`@InjectLogger`** annotation inject a class specific logger. The **`@InjectLogger`** annotation is is implemented as a bean post processor which
 needs to be configured. The bean post processor that is used to by the **`@InjectLogger`** annotation is 
 implemented in **`com.quantal.javashared.beanpostprocessors.LoggerInjectorBeanPostProcessor`.**. To configure the **`com.quantal.javashared.beanpostprocessors.LoggerInjectorBeanPostProcessor`** and make the 
 **`@InjectLogger`** annotation functional , copy and paste the following into **`@Configuration`** annotated classes
 
 ```java

    @Bean
    public CommonLogFields commonLogFields() throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        return new CommonLogFields(
                "java",
                "spring-boot",
                springVersion,
                moduleName,
                hostname,
                moduleVersion,
                Locale.UK.toString(),
                Instant.now().toString()
                );
    }

    @Bean
    public LogzioConfig logzioConfig(@Value("${logzio.token}") String logzioToken) {
        return QuantalLoggerFactory.createDefaultLogzioConfig(logzioToken, Optional.empty(), Optional.empty());
    }

    @Bean
    public LoggerConfig loggerConfig(LogzioConfig logzioConfig, CommonLogFields commonLogFields){
        LoggerConfig loggerConfig = LoggerConfig.builder().build();
        loggerConfig.setLogzioConfig(logzioConfig);
        loggerConfig.setCommonLogFields(commonLogFields);
        return loggerConfig;
    }
    @Bean
    public LoggerInjectorBeanPostProcessor loggerInjectorBeanPostProcessor(CommonLogFields commonLogFields, LogzioConfig logzioConfig){
        return new LoggerInjectorBeanPostProcessor(commonLogFields, logzioConfig);
    }
```

To use **`@InjectLogger`** annotation, you will do something akin to the following 

```java

@org.springframework.stereotype.Component
public class MyClass{
     @com.quantal.javashared.annotations.logger.InjectLogger
      private QuantalLogger logger;
     
     public void myMethod(){
         logger.with("traceId", "SomeTraceId")
                .with("event", "SOME_EVENT")
                .debug("doing some task");
     }
}

```

  **Manual Configuration**
  
  ```java

CommonLogFields commonLogFields =  new CommonLogFields();
		commonLogFields.setFrameworkVersion(new String("1.0.1"));
		commonLogFields.setFramework(1.00);
		LoggerConfig loggerConfig = LoggerConfig.builder()

												.commonLogFields(commonLogFields)
				  								.logzioConfig(QuantalLoggerFactory.createDefaultLogzioConfig("MY_LOGZIO_TOKEN", Optional.of(true), Optional.empty()))
												.build();
		QuantalLogger logger = QuantalLoggerFactory.getLogzioLogger(MyClass.class, loggerConfig);
		Exception nullPointerEx = new NullPointerException("Some NullPointerException");
		logger.throwing(nullPointerEx, new LogEvent("EXCEPTION_EVENT"), new LogField(SUB_EVENT_KEY, String.format("SOME_EX_SUBEVENT %s", nullPointerEx.getMessage())), new LogTraceId("TEST_EX_TRACE_ID"));
		logger.error(new ObjectAppendingMarker("TestMarker", "testMarkerFieldName"), "test markerMsg", new NullPointerException(), new LogTraceId("TEST_TRACE_ID"), new LogEvent("TEST_EVENT"));
		logger.with(new LogTraceId("TEST_TRACE_ID")).with(new LogEvent("TEST_EVENT")).with("StringTest1").with("StringTest2").info("Some string message");
```
  

  ### Asynchronous Controllers Returning CompletableFutures
  
  Async controllers that return **`CompletableFutures`** should extend **`BaseControllerAsync`**. This **`BaseControllerAsync`**
  makes a available the methods **`applyJsonView`** and **`applyJsonViewAsync`**. These allow us to apply **`@JsonView`** annotations to controller methods 
  that return   **`CompletableFutures`** . For example
  
  ```java
  
    @GetMapping(value="/{userId}")
    public CompletableFuture<ResponseEntity> findUserbyId(@PathVariable Long userId){
      return userManagementFacade
              .findUserById(userId)
              .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
    }
   ```

## resources/db/migration
 This is a **`required directory`**.  This contains SQL for both migrations and seed files

 
  ### The @EnforceRequiredHeaders Annotation and the RetrofitRequiredHeadersEnforcerAspectJAspect Aspect
  
  The **`@EnforceRequiredHeaders`** annotation is used to enforce the existence of headers on **`Retrofit`**
  service / api methods. The  **`@EnforceRequiredHeaders`** makes sure that if a **`Retrofit`**
  service / api methods is declared, the headers which are supplied via the **`value array`** attribute on the
  **`@EnforceRequiredHeaders`** annotation or via the  **`defaultHeadersToCheckFor`** param in the **`RetrofitRequiredHeadersEnforcerAspectJAspectJAspect`** 
  constructor are present as method parameters via parametres annotated with the **`retrofit2.http.Header`** annotation
  on the **`Retrofit`** service / api methods. If the headers are not available, then a **`HeaderNotFoundException`** will be thrown.
  By default if no values are provided in the **`value array`** attribute, then the default headers which will be required / used
  are **`X-TraceId`** and **`X-Event`**. By default, if we provide headers via the  **`value array`** in the **`@EnforceRequiredHeaders`** annotation,
  the new headers will replace the default headers i.e. **`X-TraceId`** and **`X-Event`**. If this behaviour
  is not desirable (i.e. you want the new headers to be used in addition to the default headers,
  then you should also set the **`replaceDefaults`** attribute in the **`@EnforceRequiredHeaders`** annotation to **`false`**.
  
  Declaring, the **`@EnforceRequiredHeaders`** annotation on a method or a class by itself does nothing and does not 
  enable the above behaviour. The above behaviour is actually enabled via the **`RetrofitRequiredHeadersEnforcerAspectJAspect`** aspect.
 
  The **`RetrofitRequiredHeadersEnforcerAspectJAspect` has two constructors 
  i.e. the default no-arg constructor and a second constructor that takes two parameters. 
  With regards to the constructor taking two paramters, the parameters are 
  - **`Set<String> defaultHeadersToCheckFor`** - This contains the names of headers that will be used as default header names to check for
  - **`Set<String> apiServiesPackagesRegexPatterns`** - Regexes matching package names of **`Retrofit`** service / api methods 
  
  When the default **`no arg`** constructor is used, **`defaultHeadersToCheckFor`** will have values  **`X-TraceId`** and **`X-Event` as the default headers
  and **`apiServiesPackagesRegexPatterns`** will the value **`"com.quantal.*"`** to match the **`Retrofit`** service / api package names  
  
  The **`RetrofitRequiredHeadersEnforcerAspectJAspect`** works by advising **`Retrofit`** jdk dynamics proxies to enable the required behaviour.
  This means that  **`RetrofitRequiredHeadersEnforcerAspectJAspect`** must be woven into the  **`Retrofit`** classes
  which are required as dependencies (see the **`pom`** below) and hence necessarily means that  **`RetrofitRequiredHeadersEnforcerAspectJAspect`**
  MUST be configured as an **`AspectJ`** aspect and not a **`Spring AOP`** aspect.
  
  The fact that the  **`RetrofitRequiredHeadersEnforcerAspectJAspect`** is an **`AspectJ`** and **NOT** a **`Spring AOP`** aspect 
  has some implications namely, one must use the **`AspectJ`** weaver to weave the aspect at load time
  (i.e. Load Time Weaving) in order to enable it. This means that the **`AspectJ`** weaver must be specified as a javaagent on the
  command line when the application using the **`RetrofitRequiredHeadersEnforcerAspectJAspect`** is started. 
  The snippet below shows how the maven spring boot and spring boot maven plugin configuration used to weave the **`RetrofitRequiredHeadersEnforcerAspectJAspect`** 
  assuming that the **`RetrofitRequiredHeadersEnforcerAspectJAspect`**  is declared in an external jar / as dependency
  to the application using it 
  
  #### The bean definition
  
  ```java
    @Bean
      public RetrofitRequiredHeadersEnforcerAspectJAspect requestHeadersAspect(){
          RetrofitRequiredHeadersEnforcerAspectJAspect requestHeadersAspect = Aspects.aspectOf(RetrofitRequiredHeadersEnforcerAspectJAspect.class);
          return  requestHeadersAspect;
      }

```
   ##### The POM
  
  ```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.quantal.exhange</groupId>
	<artifactId>quantalex-users</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>jar</packaging>

	<name>quantalex-users</name>
	<description>Quantal LHS Exchange Microservice</description>


	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.5.2.RELEASE</version>
		<relativePath/> <!- - lookup parent from repository - ->
	</parent>

	<properties>
		<java.version>1.8</java.version>
		<thin-jar.version>1.0.5.RELEASE</thin-jar.version>
		<tracer.version>0.14.0</tracer.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
			<exclusions>
				<exclusion>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-starter-logging</artifactId>
				</exclusion>
			</exclusions>
		</dependency>

	


		<dependency>
			<groupId>org.aspectj</groupId>
			<artifactId>aspectjrt</artifactId>
			<version>${aspectj.version}</version>
		</dependency>
		<dependency>
			<groupId>org.aspectj</groupId>
			<artifactId>aspectjweaver</artifactId>
			<version>${aspectj.version}</version>
		</dependency>
		<dependency>
			<groupId>org.aspectj</groupId>
			<artifactId>aspectjtools</artifactId>
			<version>${aspectj.version}</version>
		</dependency>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-aop</artifactId>
		</dependency>
	
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>


        <dependency>
            <groupId>com.squareup.retrofit2</groupId>
            <artifactId>retrofit</artifactId>
            <version>2.2.0</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.retrofit2</groupId>
            <artifactId>converter-gson</artifactId>
            <version>2.2.0</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.retrofit2</groupId>
            <artifactId>converter-jackson</artifactId>
            <version>2.2.0</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.retrofit2</groupId>
            <artifactId>adapter-java8</artifactId>
            <version>2.2.0</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.retrofit2</groupId>
            <artifactId>adapter-guava</artifactId>
            <version>2.2.0</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.retrofit2</groupId>
            <artifactId>converter-scalars</artifactId>
            <version>2.2.0</version>
        </dependency>

		<dependency>
			<groupId>com.github.quophyie</groupId>
			<artifactId>javashared</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>

	</dependencies>

	<build>
		<sourceDirectory>${project.build.directory}/generated-sources/delombok</sourceDirectory>
		<resources>
			<resource>
				<directory>src/main/resources</directory>
				<filtering>true</filtering>
			</resource>
		</resources>
		<pluginManagement>
			<plugins>
				<plugin>
					<groupId>org.projectlombok</groupId>
					<artifactId>lombok-maven-plugin</artifactId>
					<version>1.16.16.0</version>
					<executions>
						<execution>
							<phase>generate-sources</phase>
							<goals>
								<goal>delombok</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
						<addOutputDirectory>false</addOutputDirectory>
						<sourceDirectory>src/main/java</sourceDirectory>
					</configuration>
				</plugin>

				<plugin>
					<groupId>org.codehaus.mojo</groupId>
					<artifactId>aspectj-maven-plugin</artifactId>
					<configuration>
						<showWeaveInfo/>
						<forceAjcCompile>true</forceAjcCompile>
						<complianceLevel>1.8</complianceLevel>
						<source>1.8</source>
						<target>1.8</target>
						<verbose>true</verbose>
						<XnoInline>true</XnoInline>
						<aspectLibraries>
                            <!-- THIS declares the dependency that contains the RetrofitRequiredHeadersEnforcerAspectJAspect 
                            This must also be declared as dependecies in the dependencies section
                            -->
							<aspectLibrary>
								<groupId>com.github.quophyie</groupId>
								<artifactId>javashared</artifactId>
							</aspectLibrary>
                        </aspectLibraries>
						 <weaveDependencies>
                            <!-- THIS declares the retrofit classes that are to be woven by RetrofitRequiredHeadersEnforcerAspectJAspect
                              The must also be declared as dependecies in the dependencies section
                              -->
                            <dependency>
								<groupId>com.squareup.retrofit2</groupId>
								<artifactId>retrofit</artifactId>
							</dependency>

							<dependency>
								<groupId>com.squareup.retrofit2</groupId>
								<artifactId>converter-gson</artifactId>
							</dependency>

							<dependency>
								<groupId>com.squareup.retrofit2</groupId>
								<artifactId>converter-jackson</artifactId>
							</dependency>

							<dependency>
								<groupId>com.squareup.retrofit2</groupId>
								<artifactId>adapter-java8</artifactId>
							</dependency>

							<dependency>
								<groupId>com.squareup.retrofit2</groupId>
								<artifactId>adapter-guava</artifactId>
							</dependency>

							<dependency>
								<groupId>com.squareup.retrofit2</groupId>
								<artifactId>converter-scalars</artifactId>
							</dependency>                   
                        </weaveDependencies>
					</configuration>
					<executions>
						<execution>
							<goals>
								<goal>compile</goal>
								<goal>test-compile</goal>
							</goals>
						</execution>
					</executions>
				</plugin>

				<plugin>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-maven-plugin</artifactId>
					<configuration>
						<jvmArguments>

							-javaagent:${user.home}/.m2/repository/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar
							-javaagent:${user.home}/.m2/repository/org/springframework/spring-instrument/${spring.version}/spring-instrument-${spring.version}.jar

						</jvmArguments>
					</configuration>
				</plugin>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-surefire-plugin</artifactId>
					<configuration>
						<argLine>-javaagent:${user.home}/.m2/repository/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar</argLine>
					</configuration>
				</plugin>
			</plugins>
		</pluginManagement>
		<plugins>
			<plugin>
				<groupId>org.projectlombok</groupId>
				<artifactId>lombok-maven-plugin</artifactId>
			</plugin>

			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>aspectj-maven-plugin</artifactId>
			</plugin>
		</plugins>

	</build>

	<repositories>
		<repository>
			<id>sonatype-nexus-snapshots</id>
			<name>Sonatype Nexus Snapshots</name>
			<url>https://oss.sonatype.org/content/repositories/snapshots</url>
			<releases>
				<enabled>false</enabled>
			</releases>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
		</repository>
	</repositories>

</project>

```

   ##### The aop.xml (resources/META-INF/aop.xml)
   ```xml
    <!DOCTYPE aspectj PUBLIC "-//AspectJ//DTD//EN" "http://www.eclipse.org/aspectj/dtd/aspectj.dtd">
    <aspectj>
        <weaver options="-verbose -showWeaveInfo -debug">
            <!--
                Only weave classes in our application-specific packages.
                This should encompass any class that wants to utilize the aspects,
                and must  encompass the aspects themselves.
            -->
            <include within="com.quantal.javashared.aspects.RetrofitRequiredHeadersEnforcerAspectJAspect" />
        <!-- <include within="com.quantal.exchange.users.services.api.AuthorizationApiService"/> -->
        </weaver>
        <aspects>
            <!-- declare aspects to the weaver -->
            <aspect name="com.quantal.javashared.aspects.RetrofitRequiredHeadersEnforcerAspectJAspect" />
        </aspects>
    </aspectj>
   ```
