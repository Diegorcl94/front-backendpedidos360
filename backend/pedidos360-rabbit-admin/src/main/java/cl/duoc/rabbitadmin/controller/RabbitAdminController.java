package cl.duoc.rabbitadmin.controller;

import cl.duoc.rabbitadmin.dto.CrearBindingRequest;
import cl.duoc.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.rabbitadmin.dto.CrearExchangeRequest;
import cl.duoc.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {
    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    public ResponseEntity<?> crearCola(@Valid @RequestBody CrearColaRequest request) {
        service.crearCola(request.name());
        return ResponseEntity.created(URI.create("/api/admin/queues/" + request.name()))
                .body(Map.of("name", request.name(), "durable", true));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<?> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        service.crearExchange(request.name());
        return ResponseEntity.created(URI.create("/api/admin/exchanges/" + request.name()))
                .body(Map.of("name", request.name(), "type", "direct", "durable", true));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<CrearBindingRequest> crearBinding(@Valid @RequestBody CrearBindingRequest request) {
        service.crearBinding(request);
        return ResponseEntity.created(bindingUri(request)).body(request);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @ModelAttribute CrearBindingRequest request) {
        service.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "pedidos360-rabbit-admin");
    }

    private static URI bindingUri(CrearBindingRequest request) {
        return URI.create("/api/admin/bindings?queue=" + request.queue()
                + "&exchange=" + request.exchange() + "&routingKey=" + request.routingKey());
    }
}
