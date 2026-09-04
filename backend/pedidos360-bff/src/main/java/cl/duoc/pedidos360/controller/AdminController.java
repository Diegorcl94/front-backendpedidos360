package cl.duoc.pedidos360.controller;

import cl.duoc.pedidos360.repository.PedidoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PedidoRepository repository;

    public AdminController(PedidoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> resumen() {
        return Map.of(
                "mensaje", "Endpoint autorizado por rol ADMIN",
                "totalPedidos", repository.count()
        );
    }
}
