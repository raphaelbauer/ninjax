# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NinjaX is a modern, full-stack Java 25 web framework emphasizing simplicity, performance, and explicitness. Unlike traditional Java frameworks, it deliberately avoids dependency injection containers, annotations for configuration, and "magic" conventions. The framework prioritizes immutability, type safety via records, and manual dependency composition.

**Repository:** https://github.com/raphaelbauer/ninjax
**Version:** 10.0-SNAPSHOT
**Java Version:** 25 (uses modern Java features extensively)

## Build and Test Commands

### Maven Commands
```bash
# Full build with all tests
mvn clean install

# Compile all modules
mvn clean compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName

# Build JARs without running tests
mvn package -DskipTests

# Deploy to Maven Central (requires release profile)
mvn deploy -Prelease
```

### Running the Demo Application
```bash
# Build and run the todo demo (JDK HttpServer)
cd ninjax-demo-todo
mvn clean package
mvn exec:java

# The same demo on Jetty lives in ninjax-jetty-demo-todo
```

The demo runs on http://localhost:8081 by default.

### Module-Specific Testing
```bash
# Test specific module
cd ninjax-core && mvn test

# Build specific module (plus the modules it depends on)
mvn package -pl ninjax-core -am
```

## Module Architecture

This is a multi-module Maven project with clear separation of concerns:

### Core Modules
- **ninjax-core** - Framework core (Router, Request, Result, sessions, JWT, NinjaHttpServer on the JDK's built-in HttpServer)
- **ninjax-jetty** - Optional Jetty 12 based server (`NinjaJetty`), same Router/Request/Result as the JDK server
- **ninjax-json-jackson** - JSON rendering and parsing with Jackson 3 (`Json`)
- **ninjax-template** - HTML templating (`NinjaHtmlTemplate`, `NinjaHtmlTemplateTool`, `Html`)
- **ninjax-maven-plugin** - Maven tooling (`mvn ninjax:run` with restart on change, `mvn ninjax:generateSecret`)

### Database Modules
- **ninjax-db-common** - Shared datasource configuration and models
- **ninjax-db-hikari** - HikariCP connection pool integration
- **ninjax-db-flyway** - Flyway database migration support
- **ninjax-db-jdbi** - JDBI3 SQL Objects integration

### Testing Modules
- **ninjax-test-utils** - Test utilities (`TestRequest`, `TestRequestBuilder`, `ResultAssertions`, `HttpTestClient`)

### Demo
- **ninjax-demo-todo** - Working todo list application on the JDK server
- **ninjax-jetty-demo-todo** - The same application on Jetty

## Core Architecture Principles

### 1. No Dependency Injection Framework
Dependencies are manually composed in an "Assembly" class (typically the main application class). All dependencies are declared as final fields initialized in declaration order.

**Example:**
```java
public class TodoApplication {
    public final NinjaProperties ninjaProperties = new NinjaProperties();
    final private NinjaDatasourcePropertiesExtractor datasourceExtractor =
        new NinjaDatasourcePropertiesExtractor(ninjaProperties);
    final private NinjaFlywayMigrator migrator =
        new NinjaFlywayMigrator(datasourceExtractor.get());
    public final Router router = new Router();
    public final NinjaHttpServer server = new NinjaHttpServer(router, ninjaProperties); // or NinjaJetty
}
```

### 2. Route-Based Configuration (No Annotations)
Routes are defined programmatically in the Router using a fluent API:

```java
router.GET("/").with(controller::index);
router.POST("/tasks").with(controller::addTask);
router.GET("/user/{id}").with(controller::getUser);
router.GET("/api/{id: [0-9]+}").with(controller::getById); // Path param with regex
router.PUT("/tasks/{id}").with(controller::replaceTask);   // also PATCH, DELETE and HEAD
```

A HEAD request without an explicit `router.HEAD(...)` route falls back to the GET route (in `RouteFinder`).
Both servers (`NinjaHttpServer`, `NinjaJetty`) then send status and headers but never invoke the body renderer.

### 3. Immutable Request/Result Pattern
- **Request**: Immutable class (with builder) containing all HTTP request data (headers, params, body, session, locale)
- **Result**: Immutable record representing HTTP response (status, content, cookies, session state)
- Controllers are pure functions: `Request → Result`

### 4. Functional Filter Chain
Filters implement `NinjaFilter` interface and form a chain of responsibility. Each filter can inspect the request, modify it, and either continue the chain or short-circuit with a Result.

### 5. JWT-Based Sessions
Sessions are stateless JWT tokens stored in cookies. Session data is serialized as JWT claims with HMAC SHA256 signatures. This enables horizontal scaling without session stores.

## Request/Response Flow

```
HTTP Request
    ↓
[NinjaHttpServer.NinjaHandler or NinjaJetty.NinjaServletFilter]
    ├─ Extract: method, path, headers, cookies, body
    ├─ Parse session from JWT cookie
    ├─ Build immutable Request object
    ↓
[RouteFinder]
    └─ Match route by method + path regex
    ↓
[FilterChain]
    ├─ Filter[0].doFilter() → chain.doFilter()
    ├─ Filter[1].doFilter() → chain.doFilter()
    └─ ControllerMethod.executeMethod(Request) → Result
    ↓
[Result Processing]
    ├─ Set status + content-type + headers
    ├─ Handle session state (sign JWT / delete cookie / ignore)
    ├─ Stream body via OutputStreamRenderer
    ↓
HTTP Response
```

## Database Integration Pattern

Database setup follows this initialization sequence:

1. **NinjaProperties** - Load `conf/application.conf`
2. **NinjaDatasourcePropertiesExtractor** - Parse datasource configs
3. **NinjaFlywayMigrator** - Run pending database migrations
4. **NinjaDbHikariProvider** - Create HikariCP connection pools
5. **NinjaJdbiImpl** - Wrap pools with JDBI3 SQL Objects
6. **Repositories** - JDBI interface definitions with @SqlQuery/@SqlUpdate
7. **Services** - Business logic using repositories

### Database Configuration
Configuration in `conf/application.conf`:
```properties
application.datasource.default.driver=org.h2.Driver
application.datasource.default.url=jdbc:h2:./target/tmp_db
application.datasource.default.username=sa
application.datasource.default.password=
application.datasource.default.migration.enabled=true
```

### JDBI Usage Pattern
Define SQL interfaces:
```java
public interface TaskRepositoryInterface {
    @SqlQuery("SELECT id, title, description FROM tasks")
    @RegisterConstructorMapper(Task.class)
    List<Task> findAll();

    @SqlUpdate("INSERT INTO tasks (title) VALUES (:title)")
    @GetGeneratedKeys
    long insert(@BindBean Task task);
}
```

Use via on-demand proxy (recommended - handles connections automatically):
```java
public TaskRepository(NinjaJdbi ninjaJdbi) {
    this.taskRepo = ninjaJdbi.getJdbi("default").onDemand(TaskRepositoryInterface.class);
}
```

### Flyway Migrations
Migrations located at: `src/main/resources/migrations/{datasource_name}/`
Named: `V{version}__{description}.sql` (e.g., `V1__Create_tasks_table.sql`)

## Templating System (ninjax-template)

A minimal, programmatic HTML templating system in `org.r10r.ninjax.htmltemplate`:

1. **NinjaHtmlTemplate** - Builder for composing HTML programmatically
2. **NinjaHtmlTemplateTool** - Placeholder replacement and loading templates from resource files
3. **Html** - Wrapper marking a String as trusted raw HTML

### Usage Patterns

**Pattern 1: Direct Composition**
```java
NinjaHtmlTemplate template = new NinjaHtmlTemplate();
template.appendHtml("<h1>Hello</h1>");   // trusted HTML, not escaped
template.append(userInput);              // escaped
String html = template.toString();
```

**Pattern 2: Resource Files with Placeholders**
```java
String templateHtml = NinjaHtmlTemplateTool.readResourceFile(MyTemplate.class); // Loads MyTemplate.html
Map<String, Object> params = Map.of("title", "Page", "content", new Html(trustedHtml));
String rendered = NinjaHtmlTemplateTool.replacePlaceholders(templateHtml, params);
```

Resource files use `{{key}}` syntax for placeholders and must be next to the Java class.

**XSS Prevention:** Strings are HTML-escaped by default (`append(String)` and placeholder values).
Only `appendHtml(...)`, `Html` and nested `NinjaHtmlTemplate` values are inserted unescaped, so use them
for trusted content only. `NinjaHtmlTemplate.escapeUnsafe(...)` escapes manually.

## Code Style and Conventions

### Modern Java Features
- **Records** for immutable data (Result, NinjaSession, NinjaCookie, etc.)
- **Sealed interfaces** for exhaustive pattern matching (NinjaSessionState)
- **Text blocks** (`"""..."""`) for multi-line strings
- **Optional<T>** instead of null - use extensively
- **Streams** for collection processing

### Naming Conventions
- Packages: `org.r10r.ninjax.*` (lowercase)
- Classes: PascalCase
- Methods: camelCase
- Constants: SCREAMING_SNAKE_CASE
- Variables: camelCase, prefer `final` by default

### Code Structure
- Indentation: 4 spaces
- Line length: ~120 characters
- Brace style: Opening brace on same line
- No unused imports
- Import order: Java stdlib → Third-party → NinjaX

### What to Avoid
- Dependency injection frameworks/containers
- Annotations for configuration (use programmatic routing)
- Null references (use Optional)
- Exception-based control flow (return Result with status codes)
- Mocking frameworks in tests (use manual test doubles or real implementations)

### What to Embrace
- Modern Java features (records, text blocks, sealed types)
- Explicit dependency management via Assembly pattern
- Immutable data structures
- Functional programming patterns
- Route-based configuration
- Constructor-based dependency passing

## Testing

### Framework
- **JUnit 6.1.3** (Jupiter) for test structure
- **Google Truth 1.4.5** for assertions (`assertThat(x).isEqualTo(y)`)
- **Testcontainers 2.0.5** for database testing
- No mocking frameworks - use real implementations or manual test doubles

### Test Structure
Follow Given-When-Then pattern:
```java
@Test
void shouldParseJsonBody() {
    // Given
    Request request = Request.builder()
        .inputStreamGetter(() -> jsonStream)
        .build();

    // When
    Optional<User> user = request.getJsonBody();

    // Then
    assertThat(user.isPresent()).isTrue();
    assertThat(user.get().name()).isEqualTo("Alice");
}
```

### Test Package Structure
Mirror production package structure in `src/test/java`.

## Technology Stack

- **Java**: 25
- **Web Server**: JDK built-in `com.sun.net.httpserver` (ninjax-core) or Eclipse Jetty 12.1.13 ee10 (ninjax-jetty)
- **JSON**: Jackson 3.2.2 (`tools.jackson.*` packages; java.time and Optional support built in)
- **Database**: JDBI 3.54.0, HikariCP 7.1.0, Flyway 13.7.0, H2 2.5.250
- **Sessions**: own minimal HS256 JWT implementation in `org.r10r.ninjax.core.jwt` (no JJWT)
- **Logging**: java.util.logging in the framework; the demos route SLF4J (Jetty, HikariCP, Flyway, JDBI) via slf4j-jdk14 (JDK demo) or Logback (Jetty demo)
- **Utilities**: Google Guava 33.7.1
- **Testing**: JUnit 6.1.3, Google Truth 1.4.5; Testcontainers 2.0.5 and Mockito 5.23.0 are managed in the root pom (Mockito is only used by older demo tests, prefer manual test doubles)

## Configuration

Application configuration lives in `conf/application.conf` on the classpath (properties format). The demos keep it in `src/main/java/conf/application.conf`.

**Required Properties:**
- `application.secret` - Secret key for JWT session signing (base64-encoded)

**Optional Properties:**
- `ninja.port` - HTTP server port (default: 8080)
- `application.session.expire_time_in_seconds` - Session expiration
- `application.session.cookie.secure` - Secure flag for session cookie
- `application.session.cookie.same_site` - SameSite attribute for session cookie: Strict|Lax|None, case-insensitive (default: Lax). Invalid values and `None` without `secure=true` fail at startup

## Key Design Patterns

1. **Builder Pattern** - Request, Result, NinjaCookie use fluent builder APIs
2. **Chain of Responsibility** - FilterChain for middleware processing
3. **Assembly Pattern** - Manual dependency composition in application class
4. **Functional Interface** - ControllerMethod enables lambda-based routing
5. **Record (Java 16+)** - Immutable data structures throughout
6. **Sealed Interface** - NinjaSessionState for compile-time exhaustiveness
7. **Lazy Evaluation** - Functional getters in Request (InputStreamGetter, etc.)

## Performance Characteristics

- Fast startup (no classpath scanning, no DI container)
- Direct Jetty usage (minimal abstraction overhead)
- Efficient connection pooling via HikariCP
- Stateless JWT sessions (horizontal scaling ready)
- Memory-efficient streaming for large responses

## Working with This Codebase

When making changes:
1. Read existing code first to understand patterns
2. Maintain immutability - use records where possible
3. Avoid introducing nulls - use Optional instead
4. Follow explicit dependency composition pattern
5. Add tests using JUnit (Jupiter) and Google Truth
6. Keep it simple - this framework breaks from Java tradition intentionally
