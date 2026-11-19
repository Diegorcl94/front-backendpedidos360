package cl.duoc.pedidos360.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditoriaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaConsumer.class);

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    public AuditoriaConsumer(ObjectMapper objectMapper,
                             JdbcTemplate jdbc) {
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    @KafkaListener(
        topics = "audit.timeline",
        groupId = "pedidos360-auditoria"
    )
    public void consumir(ConsumerRecord<String, String> mensaje)
            throws Exception {

        PedidoEvento evento = objectMapper.readValue(
                mensaje.value(), PedidoEvento.class);

        if (evento == null ||
            evento.eventId() == null ||
            evento.eventId().isBlank() ||
            evento.eventType() == null ||
            evento.eventType().isBlank() ||
            evento.pedidoId() == null ||
            evento.producto() == null ||
            evento.estado() == null ||
            evento.actor() == null ||
            evento.actor().isBlank() ||
            evento.timestamp() == null) {

            throw new IllegalArgumentException(
                    "Evento de auditoria invalido");
        }

        jdbc.update("""
            MERGE INTO auditoria_eventos
            (event_id, tipo_evento, pedido_id, producto,
             estado, actor, fecha, particion, kafka_offset)
            KEY(event_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            evento.eventId(),
            evento.eventType(),
            evento.pedidoId(),
            evento.producto(),
            evento.estado(),
            evento.actor(),
            evento.timestamp().toString(),
            mensaje.partition(),
            mensaje.offset()
        );

        log.info("[AUDITORIA] Evento: {} | Pedido: {} | Actor: {} | Particion: {} | Offset: {}",
                evento.eventType(),
                evento.pedidoId(),
                evento.actor(),
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
