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

        String mensaje = "Pedido creado: " + pedidoId;

        CorrelationData correlationData =
                new CorrelationData("pedido-" + pedidoId);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
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
                    "No se pudo confirmar la publicacion de "
                    + mensaje, e
            );
        }
    }
}
