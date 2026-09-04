# Pedidos360 BFF

Backend mínimo en Spring Boot para demostrar validación de JWT emitidos por Microsoft Entra ID.

## Requisitos

- Java 17 o superior.
- Maven 3.9+.

## Configurar Windows CMD

```bat
set AZURE_TENANT_ID=TU_TENANT_ID
set AZURE_API_CLIENT_ID=TU_BACKEND_CLIENT_ID
mvn spring-boot:run
```

## Configurar PowerShell

```powershell
$env:AZURE_TENANT_ID="TU_TENANT_ID"
$env:AZURE_API_CLIENT_ID="TU_BACKEND_CLIENT_ID"
mvn spring-boot:run
```

## Endpoints

- `GET /api/health`: público.
- `GET /api/pedidos`: requiere Access Token válido y scope `Pedidos.Read`.
- `GET /api/pedidos/{id}`: requiere Access Token válido y scope `Pedidos.Read`.
- `GET /api/admin/resumen`: además requiere app role `ADMIN`.

## Qué valida el JWT

Spring Security Resource Server + Nimbus:

1. firma mediante JWKS de Microsoft;
2. issuer `https://login.microsoftonline.com/<tenant>/v2.0`;
3. audience igual al Client ID de `Pedidos360-API` (acepta también `api://<client-id>`);
4. tiempo de vigencia (`exp` / `nbf`);
5. transforma scopes y roles en authorities de Spring Security.

## Base de datos

Localmente usa H2 en memoria para mantener el laboratorio simple.

El perfil `prod` incluye configuración por variables para PostgreSQL/RDS:

```text
DB_URL=jdbc:postgresql://HOST:5432/pedidos360
DB_USERNAME=...
DB_PASSWORD=...
```

## Generar JAR para EC2

```bash
mvn clean package
java -jar target/pedidos360-bff-0.0.1-SNAPSHOT.jar
```

En EC2 debes definir `AZURE_TENANT_ID`, `AZURE_API_CLIENT_ID` y, si corresponde, las variables de RDS.
