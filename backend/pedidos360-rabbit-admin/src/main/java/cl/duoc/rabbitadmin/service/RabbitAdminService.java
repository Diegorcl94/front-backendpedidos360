package cl.duoc.rabbitadmin.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import cl.duoc.rabbitadmin.dto.CrearBindingRequest;
import cl.duoc.rabbitadmin.exception.AdminOperationException;

@Service
public class RabbitAdminService {
    private static final Set<String> PROTECTED = Set.of(
            "pedidos.exchange", "pedidos.notificaciones.queue", "pedidos.dlx", "pedidos.dlq");
    private static final ParameterizedTypeReference<List<BindingInfo>> BINDINGS = new ParameterizedTypeReference<>() {};
    private final AmqpAdmin admin;
    private final RabbitTemplate rabbitTemplate;
    private final RestClient management;
    private final String vhost;

    public RabbitAdminService(AmqpAdmin admin, RabbitTemplate rabbitTemplate,
                              RestClient rabbitManagementClient,
                              @Value("${spring.rabbitmq.virtual-host}") String vhost) {
        this.admin = admin;
        this.rabbitTemplate = rabbitTemplate;
        this.management = rabbitManagementClient;
        this.vhost = vhost;
    }

    // Serializa solicitudes dentro de esta instancia. El broker conserva las comprobaciones atómicas.
    public synchronized void crearCola(String name) {
        mutable(name);
        if (queue(name).isPresent()) {
            throw conflict("La cola ya existe; no se modificó su configuración.");
        }
        String declared = admin.declareQueue(new Queue(name, true, false, false));
        if (!name.equals(declared)) {
            throw unavailable("RabbitMQ no confirmó la declaración de la cola.");
        }
    }

    public synchronized void crearExchange(String name) {
        mutable(name);
        if (exchange(name).isPresent()) {
            throw conflict("El exchange ya existe; no se modificó su configuración.");
        }
        admin.declareExchange(new DirectExchange(name, true, false));
    }

    public synchronized void eliminarCola(String name) {
        mutable(name);
        QueueInfo info = requireQueue(name);
        if (info.messages() > 0 || info.consumers() > 0) {
            throw conflict("No se elimina una cola con mensajes o consumidores.");
        }
        if (bindings("/api/queues/{vhost}/{name}/bindings", vhost, name).stream()
                .anyMatch(binding -> !binding.source().isEmpty())) {
            throw conflict("Elimina primero los bindings explícitos de la cola.");
        }
        // El broker vuelve a comprobar uso y mensajes: nunca se purga una cola.
        admin.deleteQueue(name, true, true);
        if (queue(name).isPresent()) {
            throw conflict("No se pudo verificar la eliminación de la cola.");
        }
    }

    public synchronized void eliminarExchange(String name) {
        mutable(name);
        requireExchange(name);
        if (!bindings("/api/exchanges/{vhost}/{name}/bindings/source", vhost, name).isEmpty()
                || !bindings("/api/exchanges/{vhost}/{name}/bindings/destination", vhost, name).isEmpty()) {
            throw conflict("Elimina primero los bindings del exchange.");
        }
        // AmqpAdmin.deleteExchange no tiene opción ifUnused. Se usa el canal AMQP con esa protección.
        rabbitTemplate.execute(channel -> channel.exchangeDelete(name, true));
        if (exchange(name).isPresent()) {
            throw conflict("No se pudo verificar la eliminación del exchange.");
        }
    }

    public synchronized void crearBinding(CrearBindingRequest request) {
        validateBinding(request);
        requireQueue(request.queue());
        requireExchange(request.exchange());
        if (hasBinding(request)) {
            throw conflict("El binding ya existe.");
        }
        admin.declareBinding(binding(request));
    }

    public synchronized void eliminarBinding(CrearBindingRequest request) {
        validateBinding(request);
        requireQueue(request.queue());
        requireExchange(request.exchange());
        if (!hasBinding(request)) {
            throw new AdminOperationException(HttpStatus.NOT_FOUND, "El binding no existe.");
        }
        admin.removeBinding(binding(request));
        if (hasBinding(request)) {
            throw conflict("No se pudo verificar la eliminación del binding.");
        }
    }

    private QueueInfo requireQueue(String name) {
        QueueInfo info = queue(name).orElseThrow(() ->
                new AdminOperationException(HttpStatus.NOT_FOUND, "La cola no existe."));
        if (!info.durable() || info.auto_delete() || info.exclusive()) {
            throw conflict("La cola existente no es durable, no exclusiva y sin auto-delete.");
        }
        return info;
    }

    private ExchangeInfo requireExchange(String name) {
        ExchangeInfo info = exchange(name).orElseThrow(() ->
                new AdminOperationException(HttpStatus.NOT_FOUND, "El exchange no existe."));
        if (!info.durable() || info.auto_delete() || info.internal() || !"direct".equals(info.type())) {
            throw conflict("El exchange existente no es direct, durable, público y sin auto-delete.");
        }
        return info;
    }

    private Optional<QueueInfo> queue(String name) {
        return lookup("/api/queues/{vhost}/{name}", QueueInfo.class, vhost, name);
    }

    private Optional<ExchangeInfo> exchange(String name) {
        return lookup("/api/exchanges/{vhost}/{name}", ExchangeInfo.class, vhost, name);
    }

    private <T> Optional<T> lookup(String path, Class<T> type, Object... variables) {
        try {
            T result = management.get().uri(path, variables).retrieve().body(type);
            if (result == null) {
                throw unavailable("RabbitMQ Management devolvió una respuesta vacía.");
            }
            return Optional.of(result);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw unavailable("No se pudo consultar RabbitMQ Management; revisa conexión y permisos.");
        } catch (RestClientException ex) {
            throw unavailable("No se pudo consultar RabbitMQ Management.");
        }
    }

    private List<BindingInfo> bindings(String path, Object... variables) {
        try {
            var result = management.get().uri(path, variables).retrieve().body(BINDINGS);
            if (result == null) {
                throw unavailable("RabbitMQ Management devolvió una respuesta vacía.");
            }
            return result;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new AdminOperationException(HttpStatus.NOT_FOUND, "No existe el recurso del binding.");
            }
            throw unavailable("No se pudieron comprobar los bindings de RabbitMQ.");
        } catch (RestClientException ex) {
            throw unavailable("No se pudieron comprobar los bindings de RabbitMQ.");
        }
    }

    private boolean hasBinding(CrearBindingRequest request) {
        return bindings("/api/bindings/{vhost}/e/{exchange}/q/{queue}",
                vhost, request.exchange(), request.queue()).stream()
                .anyMatch(info -> request.routingKey().equals(info.routing_key())
                        && (info.arguments() == null || info.arguments().isEmpty()));
    }

    private static Binding binding(CrearBindingRequest request) {
        return new Binding(request.queue(), Binding.DestinationType.QUEUE,
                request.exchange(), request.routingKey(), Map.of());
    }

    private static void validateBinding(CrearBindingRequest request) {
        mutable(request.queue());
        mutable(request.exchange());
        validName(request.routingKey(), 255);
    }

    private static void mutable(String name) {
        validName(name, 127);
        if (PROTECTED.contains(name) || name.startsWith("amq.") || !name.startsWith("demo.")) {
            throw new AdminOperationException(HttpStatus.FORBIDDEN,
                    "Recurso protegido: esta API solo administra recursos de prueba con prefijo demo.");
        }
    }

    private static void validName(String value, int maxLength) {
        if (value == null || value.length() > maxLength || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new AdminOperationException(HttpStatus.BAD_REQUEST,
                    "Nombre o routing key inválido: usa letras ASCII, números, puntos, guiones o guiones bajos.");
        }
    }

    private static AdminOperationException conflict(String message) {
        return new AdminOperationException(HttpStatus.CONFLICT, message);
    }

    private static AdminOperationException unavailable(String message) {
        return new AdminOperationException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueueInfo(boolean durable, boolean auto_delete, boolean exclusive, long messages, long consumers) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExchangeInfo(String type, boolean durable, boolean auto_delete, boolean internal) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BindingInfo(String source, String routing_key, Map<String, Object> arguments) {}
}
