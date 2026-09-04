# Pedidos360 - base mínima para Evaluación Parcial N°1

Este ZIP contiene **dos proyectos separados** y simples, listos para usarse como base de la evaluación:

- `frontend/pedidos360-frontend`: Angular + MSAL + Microsoft Entra ID.
- `backend/pedidos360-bff`: Spring Boot + JWT Resource Server + H2 (local) + PostgreSQL/RDS (perfil prod).

La idea es mantener el caso **Pedidos360** muy básico para concentrarse en lo que evalúa la actividad: autenticación con MSAL, envío del **Access Token** al backend y validación real del JWT en el BFF.

## Flujo que se demuestra

```text
Usuario
  -> Angular (localhost:4200)
  -> MSAL / Microsoft Entra ID
  -> Access Token
  -> GET /api/pedidos
  -> Spring Boot BFF (localhost:8080)
  -> valida firma + issuer + audience + expiración
  -> H2 / PostgreSQL
  -> respuesta JSON con pedidos
```

En AWS, la URL local del backend se reemplaza por la URL pública del **API Gateway**, que redirige a la instancia EC2 donde se ejecuta el JAR de Spring Boot.

## Qué trae el caso

El backend maneja una entidad muy simple `Pedido` con:

- id
- cliente
- producto
- cantidad
- total
- estado

Al iniciar se cargan 3 pedidos de ejemplo en H2. El frontend muestra esos pedidos solo después de autenticarse y obtener un token válido.

## Orden recomendado

1. Configurar Microsoft Entra ID siguiendo `CONFIGURACION_ENTRA_ID.md`.
2. Editar los IDs en `frontend/.../src/environments/environment.ts`.
3. Configurar las variables del backend.
4. Ejecutar backend.
5. Ejecutar frontend.
6. Probar login y carga de pedidos.
7. Cuando funcione localmente, cambiar `apiBaseUrl` por el endpoint de AWS API Gateway.

## Repositorios GitHub

La evaluación pide enlaces de código. Por eso cada carpeta está pensada como repositorio independiente:

- Repositorio 1: `pedidos360-frontend`
- Repositorio 2: `pedidos360-bff`

Ambos ya incluyen `.gitignore`.

## Evidencias fáciles de sacar

1. Frontend sin autenticar.
2. Login de Microsoft Entra ID.
3. Frontend autenticado.
4. Ruta `/pedidos` cargando datos reales del backend.
5. Consola del backend ejecutándose en puerto 8080.
6. `http://localhost:8080/api/health` respondiendo OK.
7. Acceso directo a `/api/pedidos` sin token devolviendo 401.
8. Pestaña Network del navegador mostrando la llamada a `/api/pedidos` (no publiques el token completo).
9. Posteriormente: API Gateway y EC2.

