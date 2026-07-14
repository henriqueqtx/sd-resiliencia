# Puxa um sistema Linux bem leve que já vem com o Java 21 instalado
FROM eclipse-temurin:21-jdk-alpine

# Copia o seu arquivo gerado no passo anterior para dentro do contêiner
COPY build/libs/*.jar app.jar

# O comando que o contêiner vai executar assim que for ligado
ENTRYPOINT ["java", "-jar", "/app.jar"]