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

## MySQL Cloud en Aiven: perfil opcional `mysql`

La configuración preparada en `src/main/resources/application-mysql.properties` **no activa el perfil**. Sin `mysql`, el BFF conserva H2 en memoria y su comportamiento local. El perfil `prod` de PostgreSQL también se conserva; no combinar `mysql` con `prod`, porque ambos configuran el mismo DataSource.

Se añadió `com.mysql:mysql-connector-j` con scope `runtime`, sin fijar una versión fuera del BOM: Spring Boot 3.3.5 administra Connector/J 8.3.0. Se mantienen H2, PostgreSQL y Java 17. No se cambian `Pedido`, `PedidoRepository`, los controladores, JWT, Entra ID, CORS ni RabbitMQ. Hibernate detecta el dialecto a partir de la conexión; no se fija un dialecto de H2 ni se fuerza uno antiguo de MySQL.

### Variables necesarias

Todas se establecen en la terminal que posteriormente iniciará el BFF. No hay credenciales reales ni valores predeterminados para el acceso MySQL.

| Variable | Valor que debes proporcionar |
| --- | --- |
| `MYSQL_HOST` | Hostname DNS exacto de Aiven, sin protocolo ni ruta. No sustituirlo por una IP: debe coincidir con el certificado. |
| `MYSQL_PORT` | Puerto MySQL que aparece en Aiven; no asumir 3306. |
| `MYSQL_DATABASE` | `pedidos360`, base ya creada. |
| `MYSQL_USERNAME` | Usuario MySQL con permisos sobre esa base. |
| `MYSQL_PASSWORD` | Contraseña privada del usuario MySQL. |
| `MYSQL_TRUSTSTORE_URL` | URL `file:/C:/.../aiven-mysql-truststore.p12` del truststore local. Usa `/`; codifica espacios como `%20`. |
| `MYSQL_TRUSTSTORE_PASSWORD` | Contraseña privada del truststore PKCS12. Es independiente de la contraseña MySQL. |

La conexión utiliza `sslMode=VERIFY_IDENTITY`, que exige TLS, valida la cadena de confianza y verifica el hostname. Se admiten TLS 1.2/1.3. Los timeouts JDBC son 10 segundos para conexión y 30 segundos para lectura. No se usa `trustServerCertificate=true`, no se permite degradar a una conexión sin validación y no se solicita crear la base automáticamente.

### Preparar la CA de Aiven sin conectar el backend

Aiven indica que sus conexiones MySQL con `VERIFY_IDENTITY` necesitan la CA del proyecto. Descárgala desde **Overview > Connection information > CA Certificate** del servicio correcto. Esto es un paso manual pendiente; el desarrollo no descargó certificados privados ni contactó tu servicio MySQL.

1. Desde la carpeta del BFF, crea `.local` y guarda el certificado como `.local\aiven-ca.pem`. No lo pegues en código ni en propiedades versionadas.
2. Con Java 17 disponible en `JAVA_HOME`, inspecciona el certificado y verifica origen, vigencia y huella antes de confiar en él:

   ```cmd
   "%JAVA_HOME%\bin\keytool.exe" -printcert -file ".local\aiven-ca.pem"
   ```

3. Establece la contraseña privada del nuevo truststore y ejecuta la importación. `keytool` pedirá confirmar la confianza; no aceptes un certificado que no hayas verificado.

   ```cmd
   set /p "MYSQL_TRUSTSTORE_PASSWORD=Contrasena privada del truststore: "
   "%JAVA_HOME%\bin\keytool.exe" -importcert -alias aiven-mysql-ca -file ".local\aiven-ca.pem" -keystore ".local\aiven-mysql-truststore.p12" -storetype PKCS12 -storepass:env MYSQL_TRUSTSTORE_PASSWORD
   "%JAVA_HOME%\bin\keytool.exe" -list -keystore ".local\aiven-mysql-truststore.p12" -storetype PKCS12 -storepass:env MYSQL_TRUSTSTORE_PASSWORD
   ```

4. Configura `MYSQL_TRUSTSTORE_URL` como URL absoluta del archivo. Para la ruta de este proyecto, sin espacios:

   ```cmd
   set "MYSQL_TRUSTSTORE_URL=file:/%CD:\=/%/.local/aiven-mysql-truststore.p12"
   ```

El truststore se aplica **solo a Connector/J**, mediante `spring.datasource.hikari.data-source-properties`. Se usa `fallbackToSystemTrustStore=false`; no cambies el `cacerts` de Java ni establezcas `javax.net.ssl.trustStore` globalmente. Así no se modifica la validación HTTPS de Microsoft Entra ID ni otros clientes de la JVM.

Si Aiven entrega un bundle con varias CA durante una rotación, importa cada certificado CA como una entrada de confianza distinta (alias diferentes); no supongas que una única importación de un bundle añadió todas las raíces. Actualiza el truststore cuando Aiven anuncie una rotación. Un fallo de CA o hostname debe corregirse con la CA y el DNS correctos, nunca desactivando `VERIFY_IDENTITY`.

### Comandos CMD para una futura ejecución con MySQL

**Estos comandos son instrucciones para después de revisar y aprobar la configuración. El perfil MySQL no se ejecutó durante la implementación.**

Desde una nueva terminal CMD:

```cmd
cd /d "C:\Users\diego\Desktop\front-backendpedidos360-main\backend\pedidos360-bff"

set /p "MYSQL_HOST=Hostname DNS de Aiven: "
set /p "MYSQL_PORT=Puerto MySQL de Aiven: "
set "MYSQL_DATABASE=pedidos360"
set /p "MYSQL_USERNAME=Usuario MySQL: "
set /p "MYSQL_PASSWORD=Contrasena MySQL: "
set /p "MYSQL_TRUSTSTORE_PASSWORD=Contrasena del truststore: "
set "MYSQL_TRUSTSTORE_URL=file:/%CD:\=/%/.local/aiven-mysql-truststore.p12"

"C:\Tools\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=mysql
```

`set /p` permite introducir los valores sin dejarlos escritos como literales en el comando, pero **CMD muestra lo que escribes**: introdúcelos fuera de capturas y sesiones compartidas. No incluyas contraseñas en `JAVA_TOOL_OPTIONS`, argumentos JDBC, capturas, scripts versionados o GitHub. Los `.env` no se cargan automáticamente. El archivo `.gitignore` excluye `.local/`, certificados, truststores y archivos de entorno locales.

Las configuraciones comunes de Azure/JWT, CORS, RabbitMQ, Publisher Confirms y Returns siguen viniendo de `application.properties`. RabbitMQ debe seguir disponible para el runner de conexión ya existente; esta integración no modifica ese requisito ni la seguridad.

### Hibernate y carga inicial

- En MySQL se usa **`spring.jpa.hibernate.ddl-auto=update` exclusivamente para esta demostración académica**. El perfil deshabilita la consola H2. Nunca usa `create` ni `create-drop` sobre MySQL.
- `update` puede alterar el esquema al primer arranque: revisa tablas/permisos y realiza una copia de seguridad antes de la futura conexión. En producción, emplea migraciones controladas con **Flyway o Liquibase** y normalmente `ddl-auto=validate`; no dependas de `update` para migrar datos.
- El modelo existente usa `GenerationType.IDENTITY`, `Long`, `Integer` y `String`, compatibles con MySQL y H2. No fue necesario cambiar entidades ni repositorios.
- La carga inicial existente ejecuta `repository.count() == 0`: inserta los tres pedidos de demostración solo si la tabla está vacía. Con MySQL persistente, un reinicio normal con datos no vuelve a insertarlos. Las pruebas verifican dos ejecuciones consecutivas y una tabla previamente poblada.
- Esa comprobación no es un mecanismo de coordinación entre varias réplicas concurrentes ni una migración de datos; para producción se debe reemplazar por inicialización/migraciones controladas. Si vacías la tabla manualmente, el siguiente arranque vuelve a cargar la demostración.
- H2 sigue siendo en memoria: al reiniciar el proceso se crea una base nueva y se cargan otra vez los datos de demostración. Esto no representa duplicación de registros persistentes.
- No se migran automáticamente a Aiven los pedidos actuales de H2. Una eventual transferencia de datos debe planificarse por separado.

### Verificación sin MySQL ni Aiven

```cmd
"C:\Tools\apache-maven-3.9.16\bin\mvn.cmd" -B verify
```

Las pruebas de persistencia utilizan una H2 aislada y transacciones con rollback, sin configuración RabbitMQ/JWT. Las pruebas del archivo MySQL solo leen y enlazan propiedades con valores ficticios: no activan el perfil `mysql`, no crean un DataSource MySQL y no abren sockets a Aiven. Una prueba de configuración mantiene comprobado que el perfil predeterminado apunta a H2. La integración real, los permisos y el handshake TLS con Aiven quedan pendientes hasta tu revisión.

Referencias: [TLS y CA de Aiven](https://aiven.io/docs/platform/concepts/tls-ssl-certificates), [validación de servidor en Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-server-authentication.html).
