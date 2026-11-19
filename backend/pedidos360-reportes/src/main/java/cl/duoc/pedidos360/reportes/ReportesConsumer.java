package cl.duoc.pedidos360.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReportesConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(ReportesConsumer.class);

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    public ReportesConsumer(ObjectMapper objectMapper,
                            JdbcTemplate jdbc) {
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    @KafkaListener(
        topics = "orders.events",
        groupId = "pedidos360-reportes"
    )
    public void consumir(ConsumerRecord<String, String> mensaje)
            throws Exception {

        PedidoEvento evento = objectMapper.readValue(
                mensaje.value(), PedidoEvento.class);

        if (evento.eventId() == null ||
            evento.eventId().isBlank() ||
            evento.pedidoId() == null ||
            evento.producto() == null ||
            evento.producto().isBlank() ||
            evento.cantidad() == null ||
            evento.cantidad() <= 0 ||
            evento.total() == null ||
            evento.total() < 0 ||
            evento.estado() == null ||
            evento.timestamp() == null ||
            !"OrderCreated".equals(evento.eventType())) {

            throw new IllegalArgumentException("Evento invalido");
        }

        jdbc.update("""
            MERGE INTO reporte_eventos
            (event_id, pedido_id, producto, cantidad,
             total, estado, fecha)
            KEY(event_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            evento.eventId(),
            evento.pedidoId(),
            evento.producto(),
            evento.cantidad(),
            evento.total(),
            evento.estado(),
            evento.timestamp().toString()
        );

        log.info("[REPORTES] Pedido procesado: {} | Producto: {} | Total: {} | Particion: {} | Offset: {}",
                evento.pedidoId(),
                evento.producto(),
                evento.total(),
                mensaje.partition(),
                mensaje.offset());
    }

    public record PedidoEvento(
        String eventId,
        String eventType,
        Instant timestamp,
        Long pedidoId,
        String producto,
        Integer cantidad,
        Long total,
        String estado,
        String actor
    ) {}
}
