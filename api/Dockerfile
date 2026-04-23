# We hebben alleen de Java runtime nodig, geen Maven
FROM eclipse-temurin:21-jdk
WORKDIR /app

# We kopiëren de jar die we via SCP naar de 'target' map hebben gestuurd
# In de build context van docker-compose is dit api/target/*.jar
COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
