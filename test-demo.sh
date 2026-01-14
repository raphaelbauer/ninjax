#!/bin/bash
cd ninja-demo-todo
echo "Building application..."
mvn clean package -q

echo "Starting application in background..."
java -cp "target/classes:$(mvn dependency:build-classpath -q | tail -1)" org.ninjax.demo.todo.TodoApplication > app.log 2>&1 &
APP_PID=$!

echo "Waiting for server to start..."
sleep 5

echo "Testing HTTP connection..."
if curl -s -f http://localhost:8081/ > /dev/null; then
    echo "✅ Application is running successfully!"
    echo "Test with: curl http://localhost:8081/"
    echo "Or open: http://localhost:8081/ in your browser"
else
    echo "❌ Application failed to start properly"
    echo "Last 20 lines of logs:"
    tail -20 app.log
fi

echo "Cleaning up..."
kill $APP_PID 2>/dev/null || true