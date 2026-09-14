# Doces Yncantos App

Projeto "Monte seu Bolo": aplicativo Android para montar pedidos de bolo, com uma API REST em Spring Boot.

> Status: em desenvolvimento (estrutura inicial).

## Estrutura

```
.
├── API/   # Backend REST (Spring Boot + MySQL + JWT)
└── APP/   # Aplicativo Android (Java)
```

## API

**Stack:** Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security, MySQL, JWT (jjwt 0.12.6), Swagger/OpenAPI (springdoc 2.6.0).

### Pré-requisitos

- JDK 17
- MySQL rodando em `localhost:3306`

### Configuração

Edite `API/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/monte_seu_bolo?useSSL=false&serverTimezone=UTC
spring.datasource.username=SEU_USUARIO_AQUI
spring.datasource.password=SUA_SENHA_AQUI
app.jwt.secret=TROQUE_ESTE_SECRET_POR_UM_VALOR_SEGURO_DE_NO_MINIMO_256_BITS
```

> Não faça commit de credenciais reais.

Crie o banco antes de rodar:

```sql
CREATE DATABASE monte_seu_bolo;
```

### Executar

```bash
cd API
./mvnw spring-boot:run      # Linux/macOS
mvnw.cmd spring-boot:run    # Windows
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### Endpoints

| Recurso  | Rota            |
|----------|-----------------|
| Usuários | `/api/usuarios` |

## APP (Android)

**Stack:** Java 17, Android SDK 34 (minSdk 24), Gradle.

### Executar

1. Abra a pasta `APP/` no Android Studio.
2. Aguarde o sync do Gradle (o `local.properties` é gerado automaticamente).
3. Rode em um emulador ou dispositivo.

Via linha de comando:

```bash
cd APP
./gradlew assembleDebug      # Linux/macOS
gradlew.bat assembleDebug    # Windows
```
