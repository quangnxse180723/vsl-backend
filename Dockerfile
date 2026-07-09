# ============================================================================
#  Stage 1: BUILD - bien dich va dong goi jar bang Maven (Java 21)
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy truoc pom + lombok.config de tan dung cache layer dependency
COPY pom.xml lombok.config ./
RUN mvn -B -q dependency:go-offline

# Copy ma nguon va build (bo qua test trong buoc dong goi image)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============================================================================
#  Stage 2: RUNTIME - chi chua JRE + jar, nhe & bao mat hon
#
#  Dung eclipse-temurin:21-jre-jammy (Ubuntu 22.04, glibc) thay vi alpine.
#  Ly do: ONNX Runtime & JavaCV native libs (.so) duoc compile cho glibc.
#  Alpine dung musl libc nen thieu libstdc++.so.6 → UnsatisfiedLinkError.
# ============================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Chay duoi user khong phai root (dung Debian/Ubuntu syntax)
RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# Chay JVM theo gio Viet Nam (UTC+7) de moc "ngay" cua streak & timestamp luyen tap
# khop voi ngay thuc te cua nguoi dung, khong bi lech sang ngay hom truoc luc rang sang.
# -Duser.timezone dung tzdb rieng cua JVM nen khong phu thuoc tzdata cua OS.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
