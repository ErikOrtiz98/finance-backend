echo "FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]" > Dockerfile

git add Dockerfile
git commit -m "Agregar Dockerfile"
git push