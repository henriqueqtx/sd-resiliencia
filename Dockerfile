# Puxa um sistema Linux bem leve que já vem com o Java 21 instalado
FROM eclipse-temurin:21-jre-alpine

# Copia o seu arquivo gerado no passo anterior para dentro do contêiner
COPY build/libs/*.jar app.jar

# O comando que o contêiner vai executar assim que for ligado
# -XX:MaxRAMPercentage=75: Usa no máximo 75% da memória do container (384Mi de 512Mi)
# -XX:+UseSerialGC: GC leve ideal para containers com poucos recursos
# -XX:+TieredCompilation -XX:TieredStopAtLevel=1: Compila rápido no boot, sem otimizações pesadas
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-jar", "/app.jar"]