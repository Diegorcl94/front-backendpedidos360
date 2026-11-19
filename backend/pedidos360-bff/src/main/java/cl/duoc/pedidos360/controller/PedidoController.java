package cl.duoc.pedidos360.controller;

import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import cl.duoc.pedidos360.messaging.PedidoProducer;
import cl.duoc.pedidos360.messaging.KafkaProducer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository repository;
    private final PedidoProducer producer;
    private final KafkaProducer kafkaProducer;

    public PedidoController(
            PedidoRepository repository,
            PedidoProducer producer,
            KafkaProducer kafkaProducer) {

        this.repository = repository;
        this.producer = producer;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(@PathVariable Long id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(
            @RequestBody Pedido pedido,
            Authentication authentication) {

        pedido.setId(null);
        pedido.setEstado("PENDIENTE");

        Pedido guardado = repository.save(pedido);

        // RabbitMQ - Evaluacion anterior
        producer.publicarPedidoCreado(guardado.getId());

        // Kafka - EV3
        kafkaProducer.publicarPedidoCreado(
                guardado,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(guardado);
    }
}
