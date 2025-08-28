# Use an official OpenJDK runtime as a parent image
FROM --platform=linux/amd64 openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the Spring Boot jar file into the container
COPY build/libs/alzi-0.0.1-SNAPSHOT.jar .

# Copy the type-image directory for citizen diplomat type images
COPY type-image/ type-image/

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "alzi-0.0.1-SNAPSHOT.jar"]