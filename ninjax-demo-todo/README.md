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

## Running the Application

### Using Maven

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw ninja:run
```

The application will start on `http://localhost:8080`

### Using the Application

1. Open `http://localhost:8080` in your browser
2. Add tasks using the form at the top
3. Delete tasks using the delete button next to each task
4. Access the JSON API at `http://localhost:8080/tasks.json`