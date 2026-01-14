# NinjaX Todo Demo

A simple task list application demonstrating the NinjaX Java web framework with JDBI, H2 database, and HTML templating.

## Features

- ✅ Add new tasks via HTML form
- 🗑️ Delete tasks
- 📋 View all tasks in a clean web interface
- 🔗 JSON API endpoint at `/tasks.json`
- 🚀 Built with modern Java 25 features

## Architecture

- **Framework**: NinjaX (modern Java web framework)
- **Database**: H2 in-memory database with JDBI
- **Migrations**: Flyway database migrations
- **Template**: Inline HTML generation
- **JSON**: Jackson for JSON serialization

## Running the Application

### Using Maven

```bash
# Build the project
mvn clean package -pl ninja-demo-todo

# Run the application
cd ninja-demo-todo
mvn exec:java
```

The application will start on `http://localhost:8080`

### Using the Application

1. Open `http://localhost:8080` in your browser
2. Add tasks using the form at the top
3. Delete tasks using the delete button next to each task
4. Access the JSON API at `http://localhost:8080/tasks.json`

## Endpoints

- `GET /` - Main HTML interface
- `POST /tasks` - Add a new task
- `POST /tasks/delete` - Delete a task
- `GET /tasks.json` - JSON API with all tasks

## Project Structure

```
ninja-demo-todo/
├── src/main/java/org/ninjax/demo/todo/
│   ├── Task.java                 # Task record/model
│   ├── TaskService.java          # Service layer with JDBI
│   ├── TodoController.java        # Web controller
│   └── TodoApplication.java      # Main application class
├── src/main/resources/
│   ├── conf/application.conf      # Configuration
│   └── migrations/default/
│       └── V1__Create_tasks_table.sql  # Database migration
└── pom.xml                      # Maven configuration
```

## Key NinjaX Concepts Demonstrated

- **Manual Dependency Injection** - No magic, explicit assembly in TodoApplication
- **Modern Java Records** - Immutable Task model with compact constructors
- **Route-based Configuration** - No annotations, pure Java routing
- **Explicit Error Handling** - Result-based responses instead of exceptions
- **Database Integration** - JDBI with SQL objects and Flyway migrations