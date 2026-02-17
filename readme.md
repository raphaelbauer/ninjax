# NinjaX

[![Java CI with Maven](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml/badge.svg)](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml)

## About

NinjaX is a full-stack web framework for modern Java 25+.
Rock solid, fast, and super productive.

## Background

### Rails as a Role Model for Web Development
In the second half of the 2000s, Rails took the web development community by storm. Rails made it very simple to ship full-stack web apps. Rails did not work against HTTP, but with it, which resulted in very clear request → do stuff → response cycles.

### Java Web Development Sucked in the 2000s. Hard.
Java back then was… well… different. There was the Servlet API. There was J2EE. There were session beans. Entity beans. Lots of magic in how HTTP mapped to Java code and persistence.  
All of that tried to hide HTTP from the developer, with varying degrees of success.

In hindsight, it was a very academic approach to wrap something into “object-oriented dogmas” that worked much better when used plainly (HTTP). IMHO.

### Ninja Set Out to Combine the Best of Both Worlds in 2010
So we started the Ninja web framework around 2010 - heavily influenced by Rails, but with the ecosystem, IDEs, and language we loved so much: Java. We also added lots of innovations: stateless, scalable apps; SuperDevMode, which made developing in Java as productive as in a scripting language; and many more things.

Ninja rose to some prominence, with a couple of larger companies using it. But when Spring Boot was released, it became the clear path for most Java companies.

### Spring Boot Entering the Arena
Spring Boot offered a lightweight migration from both J2EE (which was almost dead) and Spring-based applications. And today, Spring Boot is the #1 web framework for Java.

### Java Web Development in 2026 Can Be Different. And More Beautiful.
From 2012, when Ninja started, to 2026, an awful lot has happened in Javaland.

Just to name a few:
- EJBs, WAR files, and Enterprise Java are dead.
- Servlets and servlet containers are no longer important. (Okay - people now use something conceptually similar called Kubernetes, but that’s a different story.)
- Null is (almost) dead.
- There’s a big push for immutability and the use of Optional.
- Streams and mapping were introduced and add a nice functional flavor to Java.
- Easy-to-use lambdas make calling functionals syntactically nice - finally.
- Records are an interesting way to get immutable data classes with `hashCode` and `equals` implemented out of the box.
- We got multiline strings. (Okay - we still don’t have template strings.)
- Messages did not support UTF-8.
- ...and many more things.

At the same time, IDE support is still amazing. Java is more fun to code than ever.

So the big question is: How would a Java web framework look in 2026 - without any baggage from the past, using all the goodies of 2026?

This is what NinjaX is about. And to be frank: it is no longer an experiment, but something based on stable tech and already used in production.

## Philosophy and Guiding North Star

### Goals and Non-Goals for NinjaX
- Be obvious. No annotations or hidden logic (e.g., aspects).
- Prefer immutability wherever possible.
- No nulls. Never use null.
- Minimize dependencies. Use as few libraries and external dependencies as possible (e.g., no Mockito, no matcher library).
- No dependency injection.
- Prefer composition over inheritance (in most cases). It’s generally easier to understand than inheritance.
- No exposure of the Servlet API whatsoever.
- One way to do things (e.g., not routing via files and annotations).
- Trading a bit of boilerplate for clarity (ease of use / debugging) is OK.
  - Validation should be done via an explicit function. No magic validation in controllers.
  - No magic injection into controller functions. All controller methods receive a request and are explicit.
  - No dependency injection. It leads to more boilerplate, but it’s more obvious and enables fast startups.

### Things that are not supported in V1
- Injection priorities won’t be part of NinjaX. If instantiation in the Assembly is done correctly, you don’t need priorities.
- No support for circular dependencies. If you have circular dependencies, you’re doing it wrong.
- A scheduler won’t be part of NinjaX. This can be done separately.
- Freemarker won’t be part of NinjaX—this is replaced by NinjaX Templates.
- HTTPS is not part of NinjaX.
- No exception-based error handling to generate results.
- Changing the server is not a goal for V1. Using Jetty for now.


## Getting Started

You'll need just 1 thing to develop with NinjaX:

- JDK (Java Development Kit), version 25 and above

Note: NinjaX is compatible with Java 25 and we'll support future Java versions with long term support.

### Installing Java
NinjaX is using the Java as programming language and the Java Virtual Machine to run your applications. 
You have to make sure that you are running at least Java in version 25.

You can check that by executing the following command:

    java -version

Which prints out the following:

    openjdk version "25.0.1" 2025-10-21

If you are using an older version please install the latest Java version from or via apt-get or brew.

### Create your first application

The most simple way to kickstart your NinjaX application is to download our archetype from here.

You can also use the command line like so:

```bash
wget https://github.com/raphaelbauer/ninja-demo-todo/archive/refs/heads/main.zip
unzip main.zip
```

This will create a directory called `ninja-demo-todo` which contains a full NinjaX project that is ready to go.

### Running the application

Starting the project is simple:

```bash
cd ninja-demo-todo
./mvnw clean install     # to generate the compiled classes the first time
./mvnw ninjax:run         # to start Ninja's SuperDevMode
```

This starts Ninja's SuperDevMode. Simply open http://localhost:8080 in your browser. 
You'll see the NinjaX demo project ready to work on.

> [!NOTE]  
> We think that fast and responsive development cycles are a key success factor for software projects. 
> SuperDevMode is our answer to that challenge. Say goodbye to long and time consuming deployment cycles while developing.
> Make sure that your IDE is compiling changes as you make edits. That way Ninja's SuperDevMode will restart and pick-up all changes.

In Netbeans that feature is called ["compile on save"](https://netbeans.apache.org/tutorial/main/kb/docs/java/javase-intro/).

## Basic concepts

### A Hello World Example

A basic NinjaX application looks like this:

```java
public class Application {
    
    public void main() {
        // Read application properties from file conf/application.conf
        var ninjaProperties = new NinjaProperties();

        // Create router to handle incoming requests
        var router = new Router(); 
        
        // return "Hello World" to a request coming to http://localhost:8080/
        router.GET("/").with(request -> 
            Result.builder()
                .status(Result.SC_200_OK)
                .text("Hello World")
                .build()
        );
        
        // Start the server and handle all incoming requests...
        new NinjaJetty(router, ninjaProperties);
    }
}
```

### Real Application Layout

The core principles of NinjaX stay the same. For large and small applications alike:
- conf/application.conf contains all configuration as simple key value pairs
- A main application file (e.g. TodoApplication contains all routes and wires together
all application components (services, repositories etc).
- By convention ...Controller classes contain all controller like logic and render the output.
- html of view classes is next to their Java classes that render the output.
- Following a Domain Driven Design e.g. putting anything related to a domain into a package is recommended (see tasks).

In the demo project (with database and one domain) this looks like the following:

```bash
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   ├── conf
    │   │   │   └── application.conf
    │   │   ├── logback.xml
    │   │   ├── migrations
    │   │   │   └── default
    │   │   │       └── V1__Create_tasks_table.sql
    │   │   └── org
    │   │       └── r10r
    │   │           └── ninjax
    │   │               └── demo
    │   │                   └── todo
    │   │                       ├── TodoApplication.java
    │   │                       ├── tasks
    │   │                       │   ├── Task.java
    │   │                       │   ├── TaskRepository.java
    │   │                       │   ├── TaskService.java
    │   │                       │   ├── TodoController.java
    │   │                       │   └── views
    │   │                       │       ├── TaskFormTemplate.java
    │   │                       │       ├── TaskItemTemplate.html
    │   │                       │       ├── TaskItemTemplate.java
    │   │                       │       ├── TaskListTemplate.java
    │   │                       │       └── TodoTemplateService.java
    │   │                       └── views
    │   │                           ├── LayoutTemplate.html
    │   │                           └── LayoutTemplate.java
    │   └── resources
    └── test
        ├── java
        │   └── org
        │       └── r10r
        │           └── ninjax
        │               └── demo
        │                   └── todo
        │                       ├── TaskRepositoryTest.java
        │                       ├── TaskServiceTest.java
        │                       ├── TodoApplicationIntegrationTest.java
        │                       └── TodoControllerTest.java
        └── resources
            └── conf
                └── application.conf
```

### Configuration properties

#### Basics

`conf/application.conf` contains all application logic. There's no magic here. Just simple-value pairs.
If you want to override these properties, you can use Java system properties.

#### Configuration properties in production
Override properties in application.conf is needed when running a server in production
 and selectively overwriting e.g. port and setting credentials:

```bash
java -jar -Dninja.port=5000 \
          -Dapplication.secret=${APPLICATION_SECRET} \
          -Dapplication.datasource.default.url=${DATABASE_JDBC_URL} \
          -Dapplication.datasource.default.username=${DATABASE_USERNAME} \
          -Dapplication.datasource.default.password=${DATABASE_PASSWORD} \
          -Dapplication.datasource.default.migration.username=${DATABASE_USERNAME} \
          -Dapplication.datasource.default.migration.password=${DATABASE_PASSWORD} \
          target/app.jar
```

In that case `${APPLICATION_SECRET}` would be set by your container and used as a Java system propery.
It would override application.secret in your application.conf file.

#### Configuration Properties in tests

In tests you can use a file in test/resources/conf/application.conf that will take
predecence over the real application.conf file.

### HTML templating

NinjaX includes a typesafe, compiled HTML templating system called **NinjaX Templates**. 
It favors Java code over template logic.

Templates are basically Java classes that generate HTML strings. You can compose them easily.

What NinjaX Templates can do:
- Replacing of variables in .html file using {{...}}
- Escaping of unsafe content by default. Use new Html(...) to not escape it.

What NinjaX Templates can't do:
- No control logic. You can freely iterate and assemble your templates. But you have to do this
in the Java class that reads the html.

This simplicity allows Ninja Templates to live without any special compiler support. At the same time
they are really simple and easy to understand.

**Example View Composition:**

```html
<div class="task">
    <strong>{{completedText}} {{title}}</strong>
</div>
```

**Layout Template:**

```java
public class TaskItemTemplate {

    // Read template string only once
    private final static String TEMPLATE = NinjaHtmlTemplateTool.readResourceFile(TaskItemTemplate.class);

    public static NinjaHtmlTemplate render(Task task) {

        String completedText = task.completed() ? "✅ " : "⏳ ";

        var parameters = Map.of(
                "completedText", completedText,    // Add the text. Escaped by default.
                "title", new Html(task.title()),   // Do not escape the title. Only do this for strings you control. Never
                                                   // do it for customer supplied strings.
        );
        
        // Replace placeholders {{ ... }} with your parameters. Escaped by default. Unsafe when you use new Html(...)
        var templateWithVariables = NinjaHtmlTemplateTool.replacePlaceholders(TEMPLATE, parameters);

        var template = new NinjaHtmlTemplate();
        template.appendHtml(templateWithVariables);

        return template;

    }

}
```

### Working with JSON

NinjaX has built-in JSON support. You can easily serialize objects to JSON responses.

```java
import org.r10r.ninjax.json.Json;

public class TodoController {
    
    private final Json json;
    
    public TodoController(Json json) {
        this.json = json;
    }

    public Result getTasksJson(Request request) {
        List<Task> tasks = taskService.findAll();
        
        return Result.builder()
                .status(Result.SC_200_OK)
                .json(json.json(tasks))
                .build();
    }
}
```

## Advanced topics

### Validation
Validation in NinjaX is explicit. There is no "magic" validation behind annotations. You validate data inside your controller methods.

```java
public Result addTask(Request request) {
    String title = request.parameters().get("title").orElse("");
    
    if (title.trim().isEmpty()) {
        return Result.builder()
                .badRequest()
                .text("Title cannot be empty")
                .build();
    }
    
    // Proceed with logic...
}
```

### Uploading files
File uploads are supported via `Request.getFileItem()`. Ensure your form uses `enctype="multipart/form-data"`.

```java
public Result uploadFile(Request request) {
    Optional<FileItem> fileItem = request.getFileItem("profile_picture");
    
    if (fileItem.isPresent()) {
        FileItem file = fileItem.get();
        // Process input stream: file.getInputStream()
        // Check content type: file.getContentType()
    }
    
    return Result.ok().build();
}
```

### Working with relational DBs
NinjaX integrates well with `JDBI`, `HikariCP`, and `Flyway` for a robust database stack.

**Setup in `Application.java`:**

```java
// DB configuration
var ninjaDatasourceConfigProvider = new NinjaDatasourcePropertiesExtractor(ninjaProperties);
var ninjaFlywayMigrator = new NinjaFlywayMigrator(ninjaDatasourceConfigProvider.get());
var ninjaDbHikariProvider = new NinjaDbHikariProvider(ninjaDatasourceConfigProvider.get());
var ninjaJdbiImpl = new NinjaJdbiImpl(ninjaDbHikariProvider.get());

// Pass ninjaJdbiImpl to your repositories
var taskRepository = new TaskRepository(ninjaJdbiImpl);
```

**Repository Example (JDBI):**

```java
public class TaskRepository {
    private final Jdbi jdbi;

    public TaskRepository(NinjaJdbi ninjaJdbi) {
        this.jdbi = ninjaJdbi.getJdbi("default");
    }

    public List<Task> findAll() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM tasks")
                  .mapTo(Task.class)
                  .list()
        );
    }
}
```

### Internationalization
NinjaX supports I18N via message bundles.

- Messages bundles in "root" (Java convention) (e.g., `messages.properties`, `messages_de.properties`)
- Message bundles should be UTF-8.
- Define supported languages in `application.conf`: `application.languages=en,de`

```java
// In your controller
var locale = request.getLocale();
var message = ninjaMessages.getMessage("login.welcome", locale, "User");
```

### Static assets
Serving static assets (CSS, JS, Images) from the jar is not supported in V1.
It is recommended to serve static assets via a reverse proxy (like Nginx) or a CDN in production.

For development, you can use external services or CDN links in your templates.

### Logging
NinjaX uses SLF4J and Logback. You can configure logging via `src/main/resources/logback.xml`.

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

### Custom routing
Routing is defined explicitly in code using the `Router` class.

```java
var router = new Router();
router.GET("/").with(controller::index);
router.POST("/users").with(controller::createUser);

// Path parameters
router.GET("/users/{id}").with(controller::getUser);

// Regex constraints
router.GET("/users/{id: [0-9]+}").with(controller::getUserById);
```

### Testing
NinjaX applications are easy to test using `HttpTestClient` for integration tests.

```java
@Test
void testCreateTask() throws IOException {
    HttpTestClient client = HttpTestClient.localhost(TEST_PORT);
    
    var response = client.post("/tasks", Map.of("title", "Buy milk"));
    
    assertThat(response.statusCode()).isEqualTo(303);
}
```

### Debugging
Since NinjaX is just plain Java, you can debug it like any other Java application.
1. Run `TodoApplication.main()` in Debug mode in your IDE.
2. Set breakpoints in your controllers or services.
3. Enjoy full introspection.

## Deployment

NinjaX applications are packaged as standard FAT JARs (or simple JARs with dependencies).

1. Build the project:
   ```bash
   mvn clean package
   ```
2. Run the generated JAR:
   ```bash
   java -jar target/my-app-1.0-SNAPSHOT.jar
   ```

You can configure the port and secret via system properties:

```bash
java -Dninja.port=9000 -Dapplication.secret=prod_secret -jar my-app.jar
```


## Contributing
### Deployment to Maven Central

    # Make sure gpg is set up properly
    mvn -Prelease release:prepare release:perform
    # => Everything is released automatically and should be available after few minutes globally.

Log in to https://central.sonatype.com/ check releases.
