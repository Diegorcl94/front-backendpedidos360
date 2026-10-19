# Pedidos360 Rabbit Admin

Microservicio académico con Java 17 y Spring Boot 4.0.8 para administrar recursos de prueba de RabbitMQ. Escucha únicamente en **127.0.0.1:8082**. No necesita una base de datos.

## Alcance y seguridad

- Todas las solicitudes, incluso `GET /api/admin/health`, necesitan `X-Admin-Api-Key`.
- La clave se obtiene de `RABBIT_ADMIN_API_KEY`. Es obligatoria, de 32 a 512 caracteres sin espacios. Sin una clave válida la aplicación no arranca. No hay clave predeterminada.
- El filtro compara hashes SHA-256 en tiempo constante, rechaza conexiones que no sean loopback e ignora cabeceras de proxy para decidir el origen. No se habilita CORS; se rechazan solicitudes con cabecera `Origin`.
- Solo se administran nombres con **prefijo `demo.`**, por ejemplo los de la evaluación. Se rechazan `pedidos.exchange`, `pedidos.notificaciones.queue`, `pedidos.dlx`, `pedidos.dlq`, nombres `amq.*` y cualquier nombre fuera del espacio `demo.`. La protección se aplica a creación, eliminación y ambos extremos de los bindings.
- Nombres: 1–127 caracteres ASCII: letras, números, `.`, `_` y `-`, empezando por letra o número. Routing keys: mismo alfabeto, 1–255 caracteres. No se admiten espacios ni controles.
- Los parámetros de durabilidad y tipo son fijos: colas durables, no exclusivas y sin auto-delete; exchanges direct durables, no internos y sin auto-delete. La API no permite argumentos arbitrarios ni políticas.
- Nunca se purgan colas. Para borrar una cola debe estar vacía, sin consumidores y sin bindings explícitos. El binding automático del exchange predeterminado no impide la eliminación.
- Antes de borrar un exchange se exige retirar sus bindings de origen y destino. Además, la eliminación AMQP utiliza `ifUnused=true`.
- No se declaran recursos al arrancar ni se recrean automáticamente al reconectar. La política `pedidos-retencion` no se consulta ni modifica.

Estas protecciones son para un laboratorio local, no una solución de administración pública. No cambiar `server.address` ni publicar el puerto 8082 en Docker, proxies o redes externas.

## Arquitectura y archivos

Todos los paquetes pertenecen a `cl.duoc.rabbitadmin`.

| Archivo | Propósito |
| --- | --- |
| `pom.xml` | Dependencias Initializr existentes y compilación `release 17`; nombre y descripción del servicio. |
| `src/main/resources/application.properties` | Puerto, loopback, RabbitMQ, API key por entorno, timeouts y errores sin detalles internos. |
| `config/RabbitAdminConfig.java` | Beans de `RabbitAdmin` y cliente HTTP de consulta; sin declaraciones automáticas. |
| `config/AdminSecurityProperties.java` | Configuración validada de la clave obligatoria; representación de texto enmascarada. |
| `config/AdminApiKeyFilter.java` | Autenticación de todas las operaciones, restricción de origen y respuestas 401/403. |
| `controller/RabbitAdminController.java` | Endpoints REST, validación de DTO y respuestas HTTP. |
| `service/RabbitAdminService.java` | Todas las decisiones de administración, verificaciones de existencia, protección y mutaciones AMQP. Incluye records de metadatos del broker. |
| `dto/CrearColaRequest.java` | Nombre validado de una cola. |
| `dto/CrearExchangeRequest.java` | Nombre validado de un exchange. |
| `dto/CrearBindingRequest.java` | Cola, exchange y routing key; reutilizado para el DELETE por query parameters. |
| `exception/AdminOperationException.java` | Errores controlados con estado HTTP. |
| `exception/GlobalExceptionHandler.java` | Respuestas JSON seguras para validaciones, conflictos, conexión y errores inesperados. |
| `src/test/java/cl/duoc/rabbitadmin/AdminSecurityPropertiesTest.java` | Verifica que una clave ausente o corta impida arrancar. |
| `src/test/java/cl/duoc/rabbitadmin/Pedidos360RabbitAdminApplicationTests.java` | Contexto real, salud autenticada y protección de recursos; broker apuntado a un puerto inaccesible para aislar el laboratorio. |
| `src/test/java/cl/duoc/rabbitadmin/RabbitAdminControllerTest.java` | Contrato HTTP, validación, autenticación, origen y mensajes de error. |
| `src/test/java/cl/duoc/rabbitadmin/RabbitAdminServiceTest.java` | AMQP simulado y Management simulado: recursos protegidos, declaraciones, eliminaciones, bindings y fallos. |
| `postman/Pedidos360-Rabbit-Admin.postman_collection.json` | Solicitudes manuales para la evaluación; sin secretos ni ejecuciones automáticas de operaciones adicionales. |
| `.gitignore` | Excluye compilados, secretos, logs, archivos temporales y herramientas locales. |

Las rutas abreviadas `config/`, `dto/`, etc. están bajo `src/main/java/cl/duoc/rabbitadmin/`. La clase principal Initializr `Pedidos360RabbitAdminApplication.java` se conserva sin cambios.

## AMQP y RabbitMQ Management

Las declaraciones usan `AmqpAdmin`/`RabbitAdmin`, `Queue`, `DirectExchange` y `Binding`. La eliminación del exchange usa el canal de `RabbitTemplate`, porque `AmqpAdmin.deleteExchange` no ofrece la opción `ifUnused`.

Se realizan **solo GET** contra RabbitMQ Management para verificar existencia, configuración y bindings. AMQP no ofrece una operación general para enumerar bindings. Sin Management disponible se responde 503 y no se inicia la mutación. Las URI codifican el virtual host `/` como `%2F`.

Es necesario que el plugin Management esté habilitado y que sus credenciales correspondan al mismo broker y virtual host AMQP. El usuario RabbitMQ necesita permisos de configuración sobre los recursos de prueba y acceso a metadatos mediante Management. `guest` sirve para el laboratorio local ya configurado.

Referencias: [HTTP API oficial de RabbitMQ](https://www.rabbitmq.com/docs/http-api-reference), [RabbitAdmin de Spring AMQP](https://docs.spring.io/spring-amqp/api/org/springframework/amqp/rabbit/core/RabbitAdmin.html).

## Variables de entorno

| Variable | Obligatoria | Valor predeterminado |
| --- | --- | --- |
| `RABBIT_ADMIN_API_KEY` | Sí, clave aleatoria privada de al menos 32 caracteres | Ninguno |
| `RABBITMQ_HOST` | No | `localhost` |
| `RABBITMQ_PORT` | No | `5672` |
| `RABBITMQ_USERNAME` | No | `guest`, solo demostración local |
| `RABBITMQ_PASSWORD` | No | `guest`, solo demostración local |
| `RABBITMQ_VHOST` | No | `/` |
| `RABBITMQ_MANAGEMENT_URL` | No | `http://localhost:15672` |

No guardar la API key en archivos versionados, argumentos de ejecución ni exportaciones de Postman. Un archivo `.env` no se carga automáticamente: usa variables de la terminal que inicia Java.

## Compilar y ejecutar en CMD de Windows

Abrir una terminal **CMD**, no PowerShell:

```cmd
cd /d "C:\Users\diego\Desktop\front-backendpedidos360-main\backend\pedidos360-rabbit-admin"
set "JAVA_HOME=%CD%\.local-tools\jdk-17.0.20.1+1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
"C:\Tools\apache-maven-3.9.16\bin\mvn.cmd" -B clean verify
```

Se dejó ese JDK Temurin 17 en `.local-tools/`, excluido de Git, descargado del proveedor y con checksum SHA-256 verificado. En otra máquina instala Java 17 y ajusta `JAVA_HOME` a su carpeta. No se modificó la instalación global de Java. Maven compila también los tests y empaqueta el JAR con `verify`.

Generar una API key para esta terminal (no se imprime):

```cmd
for /f %K in ('powershell -NoProfile -Command "([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))"') do set "RABBIT_ADMIN_API_KEY=%K"
```

En un archivo `.bat` se utiliza `%%K` en lugar de `%K`. Guarda la clave en tu gestor de secretos si necesitas reutilizarla. Para introducir una clave propia ya generada, puedes usar `set /p "RABBIT_ADMIN_API_KEY=Clave administrativa: "`.

Ejecutar, con RabbitMQ disponible:

```cmd
"C:\Tools\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

O ejecutar el JAR, después de `verify`, desde la misma terminal:

```cmd
java -jar target\pedidos360-rabbit-admin-0.0.1-SNAPSHOT.jar
```

No ejecutar ambas formas al mismo tiempo. Detener con `Ctrl+C`. El servicio no consume ni publica mensajes al arrancar.

## Endpoints

Base: `http://127.0.0.1:8082`. En todas las solicitudes: `X-Admin-Api-Key: <tu clave>`.

| Método | Ruta | Resultado exitoso |
| --- | --- | --- |
| GET | `/api/admin/health` | 200, estado del microservicio; no certifica disponibilidad del broker |
| POST | `/api/admin/queues` | 201 + `Location`; body `{"name":"demo.evaluacion.queue"}` |
| DELETE | `/api/admin/queues/{name}` | 204, sin body |
| POST | `/api/admin/exchanges` | 201 + `Location`; body `{"name":"demo.evaluacion.exchange"}` |
| DELETE | `/api/admin/exchanges/{name}` | 204, sin body |
| POST | `/api/admin/bindings` | 201 + `Location`; body con cola, exchange y routing key |
| DELETE | `/api/admin/bindings?queue=...&exchange=...&routingKey=...` | 204, sin body |

Estados de error: 400 solicitud inválida; 401 clave ausente/incorrecta; 403 recurso protegido u origen no permitido; 404 recurso inexistente; 409 existente/incompatible/ocupado o eliminación no verificable; 503 fallo de AMQP/Management; 500 error inesperado. Método no permitido devuelve 405 y Content-Type incorrecto 415. Nunca se devuelven credenciales, respuestas crudas del broker ni stack traces.

## Prueba manual en Postman y capturas

Importa la colección de `postman/`. Configura `baseUrl=http://127.0.0.1:8082` y `adminApiKey` como variable **local y secreta** con el mismo valor de la terminal. No exportes ni sincronices su valor. Para copiar tu variable sin imprimirla en pantalla, puedes usar en tu CMD `powershell -NoProfile -Command "Set-Clipboard -Value $env:RABBIT_ADMIN_API_KEY"` y pegarla en Postman; después limpia el portapapeles. Se recomienda Postman Desktop o su agente local.

Ejecuta cada solicitud manualmente; no ejecutes la colección completa sin revisar sus DELETE. Las pruebas incluidas en Postman solo comprueban respuestas y no envían solicitudes adicionales.

1. GET `/api/admin/health`: 200 con clave; sin clave o con una incorrecta: 401.
2. POST `/api/admin/queues`, Body > raw > JSON:

   ```json
   {"name":"demo.evaluacion.queue"}
   ```

3. POST `/api/admin/exchanges`:

   ```json
   {"name":"demo.evaluacion.exchange"}
   ```

4. POST `/api/admin/bindings`:

   ```json
   {
     "queue":"demo.evaluacion.queue",
     "exchange":"demo.evaluacion.exchange",
     "routingKey":"demo.evaluacion"
   }
   ```

5. En `http://localhost:15672`, captura la cola durable y el exchange direct durable, y el binding `demo.evaluacion`. No publiques mensajes a esa cola si vas a probar su eliminación.
6. Repetir los POST debe devolver 409 y no cambiar el recurso existente.
7. POST de cola con `{"name":"   "}`, `{"name":"demo. con espacio"}` o `{"name":"demo.\ncontrol"}`: 400. Binding con routing key vacío: 400.
8. DELETE `/api/admin/queues/pedidos.dlq`: 403. Verifica visualmente que `pedidos.dlq` sigue intacta. Intentar crear bindings que involucren recursos protegidos también devuelve 403.
9. **Solo al terminar las capturas**, DELETE del binding:

   ```text
   /api/admin/bindings?queue=demo.evaluacion.queue&exchange=demo.evaluacion.exchange&routingKey=demo.evaluacion
   ```

10. DELETE `/api/admin/queues/demo.evaluacion.queue`: 204 si está vacía y sin uso.
11. DELETE `/api/admin/exchanges/demo.evaluacion.exchange`: 204 si no quedan bindings.
12. Repetir los DELETE devuelve 404. Si aparece 409 revisa mensajes, consumidores, bindings o el estado actual; no purgues colas para forzar el resultado.

Todos los DELETE de esta lista son instrucciones para la ejecución manual; no se ejecutaron durante el desarrollo.

## Pruebas automatizadas y limitaciones

```cmd
"C:\Tools\apache-maven-3.9.16\bin\mvn.cmd" -B test
```

Los tests generan sus propias claves efímeras y no necesitan variables reales ni un RabbitMQ activo. Las pruebas HTTP usan MockMvc. Las pruebas del servicio simulan `AmqpAdmin` y `RabbitTemplate`, y las consultas Management utilizan `MockRestServiceServer` (sin red). El contexto completo configura puertos inaccesibles y comprueba que las protecciones se aplican antes de contactar al broker.

La evidencia de la ejecución se documenta en `VALIDACION.md`. No se ejecutó integración real contra RabbitMQ ni se crearon/eliminaron sus recursos durante la implementación.

Las verificaciones de metadatos y la mutación AMQP son operaciones separadas, no una transacción distribuida. Las peticiones se serializan en esta instancia y el broker comprueba precondiciones; otro administrador externo aún puede cambiar recursos entre operaciones. En un fallo de conexión tras enviar una orden, inspecciona el broker antes de reintentar: puede haberse aplicado. Esta API no reintenta automáticamente y no coordina múltiples réplicas. Si Management falla tras una eliminación no se devuelve 204 sin haber comprobado la ausencia; revisa el broker antes de repetir.
