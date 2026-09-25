FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 国内网络加速：Maven Central 直连过慢，走阿里云公共仓库（含 central + jcenter 汇聚）
RUN mkdir -p /root/.m2 && printf '<?xml version="1.0" encoding="UTF-8"?>\n<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">\n  <mirrors>\n    <mirror>\n      <id>aliyun-public</id>\n      <mirrorOf>central</mirrorOf>\n      <url>https://maven.aliyun.com/repository/public</url>\n    </mirror>\n  </mirrors>\n</settings>\n' > /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
# 注意：JVM 选项必须写在 -jar 之前。
# 写成 java -jar -Xms256m -Xmx512m app.jar 时，-jar 会把紧随其后的 -Xms256m 当作 jar 文件名，
# 启动即报 "Unable to access jarfile -Xms256m"，容器崩溃重启。
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
