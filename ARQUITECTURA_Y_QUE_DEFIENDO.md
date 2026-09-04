# Arquitectura mínima que puedes explicar al profesor

## Antes

En la práctica anterior el flujo terminaba en Angular: el usuario iniciaba sesión con MSAL/Entra ID, `MsalGuard` protegía una ruta y se observaban claims.

## Ahora

El flujo continúa hasta un backend real:

1. Angular solicita autenticación con MSAL.
2. Microsoft Entra ID autentica al usuario.
3. MSAL obtiene un **Access Token** para el scope `Pedidos.Read`.
4. `MsalInterceptor` agrega `Authorization: Bearer ...` al request.
5. La petición llega al BFF Spring Boot (localmente directo; en AWS mediante API Gateway).
6. Spring Boot no confía en que el token "se vea bien": valida firma, issuer, audience y expiración.
7. Si es válido responde con pedidos; si no es válido responde 401/403.

## Frase corta para la presentación

> MsalGuard protege la navegación del frontend, pero la seguridad real de la API se aplica nuevamente en el backend validando criptográficamente el Access Token.

## Por qué el backend se llama BFF

Para esta base mínima se usa un solo Spring Boot como Backend For Frontend: Angular consume una API adaptada a sus necesidades. Esto permite demostrar el criterio de seguridad sin crear lógica de negocio compleja.

## AWS

En la entrega final el flujo puede quedar:

`Angular -> Entra ID -> API Gateway -> EC2 (Spring Boot BFF) -> RDS`

El código no depende de la URL local: en Angular solo se cambia `apiBaseUrl` a la URL Invoke de API Gateway.
