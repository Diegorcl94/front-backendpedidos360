package cl.duoc.rabbitadmin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {
    private final byte[] expectedHash;

    public AdminApiKeyFilter(AdminSecurityProperties properties) {
        this.expectedHash = hash(properties.apiKey());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // No se confía en X-Forwarded-For; esta API tampoco admite clientes web de otro origen.
        if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()
                || request.getHeader("Origin") != null) {
            reject(response, 403, "Acceso permitido solo desde localhost y sin origen web externo.");
            return;
        }
        String provided = request.getHeader("X-Admin-Api-Key");
        if (provided == null || provided.length() > 512
                || !MessageDigest.isEqual(expectedHash, hash(provided))) {
            response.setHeader("WWW-Authenticate", "ApiKey realm=\"Pedidos360 Rabbit Admin\"");
            reject(response, 401, "Credencial administrativa ausente o invalida.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Mensajes constantes: nunca se incluye la clave ni entradas del cliente.
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
