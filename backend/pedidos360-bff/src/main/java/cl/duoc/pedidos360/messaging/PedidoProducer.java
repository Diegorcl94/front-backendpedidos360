package cl.duoc.pedidos360.messaging;

import cl.duoc.pedidos360.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PedidoProducer {

    private final RabbitTemplate rabbitTemplate;

    public PedidoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoCreado(Long pedidoId) {

        // Notificaciones - Exchange Direct
        publicar(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                "Pedido creado: " + pedidoId,
                "pedido-" + pedidoId + "-notificacion"
        );

        // Cocina - Exchange Topic
        publicar(
                RabbitMQConfig.TOPIC_EXCHANGE,
                RabbitMQConfig.COCINA_KEY,
                "Ticket de cocina para pedido: " + pedidoId,
                "pedido-" + pedidoId + "-cocina"
        );

        // Facturación - Exchange Topic
        publicar(
                RabbitMQConfig.TOPIC_EXCHANGE,
                RabbitMQConfig.FACTURACION_KEY,
                "Comprobante para pedido: " + pedidoId,
                "pedido-" + pedidoId + "-facturacion"
        );
    }

    private void publicar(
            String exchange,
            String routingKey,
            String mensaje,
            String correlationId) {

        CorrelationData correlationData =
                new CorrelationData(correlationId);

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    mensaje,
                    correlationData
            );

            CorrelationData.Confirm confirm =
                    correlationData.getFuture()
                            .get(10, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ rechazo la publicacion: "
                        + confirm.getReason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "El mensaje no pudo enrutarse a una cola."
                );
            }

            System.out.println(
                    "[PUBLISHER ACK] RabbitMQ confirmo: "
                    + mensaje
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Publicacion interrumpida", e
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error publicando: " + mensaje, e
            );
        }
    }
}