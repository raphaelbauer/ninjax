# NinjaX Framework - Agent Development Guidelines

This document provides essential information for agentic coding agents working on the NinjaX Java web framework codebase.

## Project Overview

NinjaX is a modern Java 25 web framework that prioritizes simplicity, performance, and explicitness over magic and convention. It's a multi-module Maven project with no dependency injection, no annotations, and heavy use of modern Java features.

## Build Commands

### Essential Maven Commands
```bash
# Compile all modules
mvn clean compile

# Run all tests  
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName

# Build JARs
mvn package

# Full build with tests
mvn clean install

# Deploy to Maven Central (release)
mvn deploy -Prelease
```

### CI Build Command
```bash
mvn -B package --file pom.xml
```

## Code Style Guidelines

### Naming Conventions
- **Packages:** `org.r10r.ninjax.*` (lowercase)
- **Classes:** PascalCase (e.g., `NinjaJetty`, `Request`, `Result`)
- **Methods:** camelCase (e.g., `getNinjaSession`, `build`)
- **Constants:** SCREAMING_SNAKE_CASE
- **Variables:** camelCase, prefer `final` by default

### Import Organization
1. Standard Java imports first
2. Third-party imports alphabetically  
3. No static imports (except in tests)
4. No unused imports

### Code Structure
- **Indentation:** 4 spaces
- **Line length:** ~120 characters
- **Brace style:** Standard Java style (opening brace same line)
- **Records:** Use modern Java record syntax with compact constructors
- **Text blocks:** Use Java 15+ `"""..."""` for multi-line strings

## Architecture Patterns

### Core Principles
1. **No Dependency Injection** - Manual dependency assembly via `Assembly` classes
2. **Composition over Inheritance** - Explicit composition preferred
3. **No Annotations** - Route-based configuration only
4. **Immutable by Default** - Use records and defensive copying
5. **No Nulls** - Use `Optional<T>` extensively

### Example Patterns
```java
// Records for immutable data
public record Request(
    Router.Route route,
    String requestPath,
    InputStreamGetter inputStreamGetter
) {
    // Compact constructor for validation
    public Request {
        Objects.requireNonNull(route);
        Objects.requireNonNull(requestPath);
    }
}

// Optional usage instead of null
public Optional<A> getJsonBody() {
    try (var inputStream = inputStreamGetter.get()) {
        return Optional.of(Json.objectMapper.readValue(inputStream, new TypeReference<A>() {}));
    } catch (IOException ex) {
        logger.error("Failed to parse JSON", ex);
        return Optional.empty();
    }
}
```

## Error Handling

### Response Handling
- Return `Result` objects with HTTP status codes instead of throwing exceptions
- Use `Optional<T>` for operations that may not return values
- Leverage sealed interfaces for state management (`NinjaSessionState`)

### Exception Handling
- Log errors appropriately with SLF4J
- Never let exceptions bubble up to user code
- Use functional error handling patterns

## Testing Guidelines

### Framework
- **JUnit 5.11.0** for test structure
- **Google Truth 1.4.4** for assertions
- No mocking framework (use manual test doubles)

### Test Structure
```java
@Test
void descriptiveMethodName() {
    // Given
    JuckulaCompositionTemplate t = new JuckulaCompositionTemplate();
    
    // When  
    t.html("Hello", " ", "World");
    
    // Then
    assertThat(t.toString()).isEqualTo("Hello World\n");
}
```

### Test Patterns
- Mirror production package structure
- Use Given-When-Then pattern
- Property-based testing via JUnit parameterized tests
- No mocks - use real implementations or manual test doubles

## Technology Stack

### Core Dependencies
- **Java:** 25 (use modern features)
- **Web Server:** Eclipse Jetty 11.0.25
- **JSON:** Jackson 2.20.0
- **Database:** H2, JDBI 3.49.5, HikariCP 7.0.2, Flyway 11.12.0
- **Authentication:** JJWT 0.13.0
- **Logging:** SLF4J 2.0.17 + Logback 1.5.18
- **Utilities:** Google Guava 33.3.0

### Module Structure
- `ninja-core/` - Core framework
- `ninja-db-*` - Database integrations
- `ninja-template/` - Juckula templating

## Key Design Decisions

### What to Avoid
- ❌ Dependency injection frameworks
- ❌ Annotations for configuration
- ❌ Null references
- ❌ Exception-based control flow
- ❌ Mocking frameworks in tests

### What to Embrace
- ✅ Modern Java features (records, text blocks, streams)
- ✅ Explicit dependency management
- ✅ Immutable data structures
- ✅ Optional-based error handling
- ✅ Route-based configuration
- ✅ Manual test doubles

## Running Single Tests

### Quick Test Execution
```bash
# Test specific class
mvn test -Dtest=RequestTest

# Test specific method
mvn test -Dtest=RequestTest#shouldParseValidJson

# Test in specific module
cd ninja-core && mvn test -Dtest=RequestTest
```

## Performance Considerations

- Framework prioritizes fast startup and minimal dependencies
- Direct Jetty usage (no abstraction layers)
- Efficient connection pooling with HikariCP
- Lazy initialization where appropriate
- Memory-efficient stream operations

This framework intentionally breaks from Java tradition (Spring, etc.) - embrace the explicit, modern, and simple approach.