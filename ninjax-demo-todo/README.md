# NinjaX Todo Demo

A simple task list application demonstrating the NinjaX Java web framework with JDBI, H2 database, and HTML templating.

## Features

- ✅ Add new tasks via HTML form
- 🗑️ Delete tasks
- 📋 View all tasks in a clean web interface
- 🔗 JSON API endpoint at `/tasks.json`

## Architecture

- **Framework**: NinjaX (modern Java web framework)
- **Database**: H2 in-memory database with JDBI
- **Migrations**: Flyway database migrations
- **Template**: Inline HTML generation
- **JSON**: Jackson for JSON serialization
- **Server**: JDK built-in HttpServer (default) or Jetty
- **Logging**: java.util.logging, configured in `conf/logging.properties`

## Running the Application

### Using Maven

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw exec:java
```

The application will start on `http://localhost:8081`

### Choosing the server

The demo runs on two servers. `TodoApplication` picks one with the property `ninja.server`:

- `jdk` (default) - `NinjaHttpServer`, based on the HttpServer built into the JDK
- `jetty` - `NinjaJetty`, based on Eclipse Jetty

Set the default in `conf/application.conf` or override it on the command line:

```bash
./mvnw exec:java -Dninja.server=jetty

# With SuperDevMode the property has to be passed to the forked JVM
./mvnw ninjax:run -Dninja.jvmArgs=-Dninja.server=jetty
```

The integration test `TodoApplicationIntegrationTest` runs every test against both servers.

### Using the Application

1. Open `http://localhost:8081` in your browser
2. Add tasks using the form at the top
3. Delete tasks using the delete button next to each task
4. Access the JSON API at `http://localhost:8081/tasks.json`
