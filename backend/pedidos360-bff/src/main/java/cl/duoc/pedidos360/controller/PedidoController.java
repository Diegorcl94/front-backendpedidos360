package cl.duoc.pedidos360.controller;

import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import cl.duoc.pedidos360.messaging.PedidoProducer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository repository;
    private final PedidoProducer producer;

    public PedidoController(
            PedidoRepository repository,
            PedidoProducer producer) {

        this.repository = repository;
        this.producer = producer;
    }

    // LISTAR PEDIDOS
    @GetMapping
    public List<Pedido> listar() {
        return repository.findAll();
    }

    // BUSCAR PEDIDO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(
            @PathVariable Long id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    // CREAR PEDIDO Y PUBLICAR MENSAJE
    @PostMapping
    public ResponseEntity<Pedido> crear(
            @RequestBody Pedido pedido) {

        pedido.setId(null);
        pedido.setEstado("PENDIENTE");

        Pedido guardado = repository.save(pedido);

        producer.publicarPedidoCreado(
                guardado.getId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(guardado);
    }
}
