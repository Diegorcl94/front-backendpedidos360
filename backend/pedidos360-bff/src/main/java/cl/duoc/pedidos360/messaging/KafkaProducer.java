package cl.duoc.pedidos360.messaging;

import cl.duoc.pedidos360.dto.PedidoEvento;
import cl.duoc.pedidos360.model.Pedido;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class KafkaProducer {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarPedidoCreado(Pedido pedido, String actor) {

        publicarEvento(pedido, actor, "OrderCreated");
    }

    public void publicarEvento(
            Pedido pedido, String actor, String tipo) {

        PedidoEvento evento = new PedidoEvento(
                UUID.randomUUID().toString(),
                tipo,
                Instant.now(),
                pedido.getId(),
                pedido.getProducto(),
                pedido.getCantidad(),
                pedido.getTotal(),
                pedido.getEstado(),
                actor
        );

        try {
            String json = objectMapper.writeValueAsString(evento);

            enviar("orders.events",
                    pedido.getId().toString(), json);

            enviar("audit.timeline",
                    evento.eventId(), json);

        } catch (Exception e) {
            log.error("Error generando evento Kafka", e);
        }
    }

    private void enviar(
            String topic, String key, String json) {

        try {
            kafkaTemplate.send(topic, key, json)
                    .whenComplete((resultado, error) -> {

                        if (error != null) {
                            log.error("Error publicando en " + topic, error);
                        } else {
                            var metadata = resultado.getRecordMetadata();

                            log.info(
                                    "[KAFKA] Topic: {} | Particion: {} | Offset: {}",
                                    metadata.topic(),
                                    metadata.partition(),
                                    metadata.offset()
                            );
                        }
                    });

        } catch (Exception e) {
            log.error("Error enviando evento a " + topic, e);
        }
    }
}
