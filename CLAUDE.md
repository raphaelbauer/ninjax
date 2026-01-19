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
# Build and run the todo demo
cd ninja-demo-todo
mvn clean package
mvn exec:java

# Or use the test script
./test-demo.sh
```

The demo runs on http://localhost:8081 by default.

### Module-Specific Testing
```bash
# Test specific module
cd ninja-core && mvn test

# Build specific module
mvn package -pl ninja-core
```

## Module Architecture

This is a multi-module Maven project with clear separation of concerns:

### Core Modules
- **ninja-core** - Framework core (Router, Request, Result, NinjaJetty, sessions)
- **ninja-template** - Juckula HTML templating system
- **ninja-maven-plugin** - Maven tooling for Ninja applications

### Database Modules
- **ninja-db-common** - Shared datasource configuration and models
- **ninja-db-hikari** - HikariCP connection pool integration
- **ninja-db-flyway** - Flyway database migration support
- **ninja-db-jdbi** - JDBI3 SQL Objects integration

### Testing Modules
- **ninja-test-utils** - Test utilities for Ninja applications
- **ninja-test-db-utils** - Database testing utilities with Testcontainers

### Demo
- **ninja-demo-todo** - Working todo list application demonstrating framework usage

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
    public final NinjaJetty ninja = new NinjaJetty(router, ninjaProperties);
}
```

### 2. Route-Based Configuration (No Annotations)
Routes are defined programmatically in the Router using a fluent API:

```java
router.GET("/").with(controller::index);
router.POST("/tasks").with(controller::addTask);
router.GET("/user/{id}").with(controller::getUser);
router.GET("/api/{id: [0-9]+}").with(controller::getById); // Path param with regex
```

### 3. Immutable Request/Result Pattern
- **Request**: Immutable record containing all HTTP request data (headers, params, body, session)
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
[NinjaJetty.NinjaServletFilter]
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

## Templating System (Juckula)

Juckula is a minimal, programmatic HTML templating system with two components:

1. **JuckulaCompositionTemplate** - Builder for composing HTML programmatically
2. **JuckulaTool** - Utilities for placeholder replacement and resource loading

### Usage Patterns

**Pattern 1: Direct Composition**
```java
JuckulaCompositionTemplate template = new JuckulaCompositionTemplate();
template.html("<h1>Hello</h1>");
template.html("<p>World</p>");
String html = template.toString();
```

**Pattern 2: Resource Files with Placeholders**
```java
String templateHtml = JuckulaTool.readResourceFile(MyTemplate.class); // Loads MyTemplate.html
Map<String, String> params = Map.of("title", "Page", "content", dynamicContent);
String rendered = JuckulaTool.replacePlaceholders(templateHtml, params);
```

Resource files use `{{key}}` syntax for placeholders and must be next to the Java class.

**XSS Prevention:** Use `JuckulaCompositionTemplate.escapeUnsafe(userInput)` for user-provided content.

## Code Style and Conventions

### Modern Java Features
- **Records** for immutable data (Request, Result, NinjaSession, etc.)
- **Sealed interfaces** for exhaustive pattern matching (NinjaSessionState)
- **Text blocks** (`"""..."""`) for multi-line strings
- **Optional<T>** instead of null - use extensively
- **Streams** for collection processing

### Naming Conventions
- Packages: `org.ninja.*` (lowercase)
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
- **JUnit 5.11.0** for test structure
- **Google Truth 1.4.4** for assertions (`assertThat(x).isEqualTo(y)`)
- **Testcontainers 1.20.4** for database testing
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
- **Web Server**: Eclipse Jetty 11.0.25
- **JSON**: Jackson 2.20.0 (with JSR310 and JDK8 modules)
- **Database**: JDBI 3.51.0, HikariCP 7.0.2, Flyway 11.12.0, H2 2.3.232
- **Authentication**: JJWT 0.13.0
- **Logging**: SLF4J 2.0.17 + Logback 1.5.18
- **Utilities**: Google Guava 33.3.0
- **Testing**: JUnit 5.11.0, Google Truth 1.4.4, Testcontainers 1.20.4, Mockito 5.21.0

## Configuration

Application configuration lives in `src/main/resources/conf/application.conf` (properties format).

**Required Properties:**
- `application.secret` - Secret key for JWT session signing (base64-encoded)

**Optional Properties:**
- `ninja.port` - HTTP server port (default: 8080)
- `application.session.expire_time_in_seconds` - Session expiration
- `application.session.cookie.secure` - Secure flag for session cookie

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
5. Add tests using JUnit 5 and Google Truth
6. Keep it simple - this framework breaks from Java tradition intentionally
