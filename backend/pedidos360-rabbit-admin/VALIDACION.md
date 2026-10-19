# Evidencia de validación

Fecha: 17 de octubre de 2026 (America/Santiago).

## Entorno comprobado

- Eclipse Temurin OpenJDK **17.0.20.1+1**, descargado del proveedor Adoptium y verificado mediante SHA-256.
- Maven **3.9.16**.
- Spring Boot **4.0.8**, conservado del proyecto Initializr.
- Compilación `javac release 17`; `javap -verbose` confirma **major version 61**.
- JDK y logs de trabajo en `.local-tools/`, excluidos de Git. No se modificó Java globalmente.

## Resultado real

Se realizó una compilación limpia con `mvn -B clean verify` usando Java 17. Tras ampliar las pruebas de errores del broker, la ejecución final fue:

```text
mvn -B verify
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Clase | Pruebas | Fallos / errores |
| --- | ---: | --- |
| `AdminSecurityPropertiesTest` | 1 | 0 / 0 |
| `Pedidos360RabbitAdminApplicationTests` | 3 | 0 / 0 |
| `RabbitAdminControllerTest` | 15 | 0 / 0 |
| `RabbitAdminServiceTest` | 25 | 0 / 0 |

Se generó `target/pedidos360-rabbit-admin-0.0.1-SNAPSHOT.jar`. Informes reproducibles en `target/surefire-reports/` después de ejecutar Maven.

Los avisos de error de vinculación de configuración en `AdminSecurityPropertiesTest` son esperados: esas pruebas verifican que una clave ausente o corta impida arrancar.

## Qué se comprobó

- Validación de nombres, controles, espacios, longitudes, JSON y bindings por body/query.
- Autenticación en todas las operaciones, incluida salud; rechazo de origen remoto y de cabeceras de origen web.
- Protección de los cuatro recursos críticos, nombres reservados y recursos fuera de `demo.`.
- Creación durable de colas y exchanges direct; bloqueo de redeclaraciones existentes.
- Eliminación segura con comprobación de uso, mensajes y bindings; precondiciones del broker y verificación de ausencia.
- Creación/eliminación del binding exacto y errores 404/409 cuando corresponde.
- Traducción de errores AMQP 404/405/406 y fallos de conexión, sin revelar detalles internos.
- Respuestas 201, 204, 400, 401, 403, 404, 409, 500 y 503 en los casos cubiertos.

## Qué no se ejecutó

- **No se crearon ni eliminaron colas, exchanges, bindings o mensajes reales.**
- No se ejecutaron pruebas de integración contra RabbitMQ real ni contra su Management real.
- Las respuestas HTTP de los tests se verificaron con MockMvc. AMQP y Management se simularon en los tests de servicio.
- El test de contexto apunta explícitamente a puertos inaccesibles para no contactar el broker del laboratorio.
- La colección de Postman se creó, pero no se ejecutó; queda para las pruebas manuales y capturas del usuario.
- Una comprobación adicional de arranque temporal del JAR y salud por socket HTTP fue rechazada por la revisión automática de herramientas, con motivo `blocked by policy`. No se ejecutó ni se presenta como aprobada.

Toda la implementación y documentación se limitaron a este microservicio. El frontend, BFF y notificaciones no fueron modificados.
