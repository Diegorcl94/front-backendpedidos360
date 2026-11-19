package cl.duoc.notificaciones.consumer;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoConsumer {

    private static final int MAX_INTENTOS = 3;

    @RabbitListener(queues = {
            "pedidos.notificaciones.queue",
            "pedidos.cocina.queue",
            "pedidos.facturacion.queue"
    })
    public void recibirPedido(
            String mensaje,
            Message message,
            Channel channel) throws IOException {

        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        String cola =
                message.getMessageProperties().getConsumerQueue();

        boolean procesado = false;

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {

            try {
                System.out.println(
                        "[CONSUMER] Cola: " + cola
                        + " | Intento: " + intento
                        + " | Mensaje: " + mensaje
                );

                if (mensaje == null || mensaje.isBlank()
                        || mensaje.contains("ERROR_PRUEBA")) {
                    throw new IllegalArgumentException(
                            "Mensaje invalido de prueba"
                    );
                }

                if (mensaje.contains("ERROR_TEMPORAL")) {
                    throw new IllegalStateException(
                            "Servicio temporalmente no disponible"
                    );
                }

                switch (cola) {
                    case "pedidos.notificaciones.queue":
                        System.out.println(
                                "[NOTIFICACIONES] " + mensaje);
                        break;

                    case "pedidos.cocina.queue":
                        System.out.println(
                                "[COCINA] " + mensaje);
                        break;

                    case "pedidos.facturacion.queue":
                        System.out.println(
                                "[FACTURACION] " + mensaje);
                        break;

                    default:
                        throw new IllegalArgumentException(
                                "Cola desconocida");
                }

                procesado = true;
                break;

            } catch (IllegalArgumentException error) {
                System.err.println(
                        "[ERROR DEFINITIVO] " + error.getMessage()
                );
                break;

            } catch (IllegalStateException error) {
                System.err.println(
                        "[ERROR TEMPORAL] " + error.getMessage()
                );

                if (intento < MAX_INTENTOS) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (procesado) {
            channel.basicAck(deliveryTag, false);

            System.out.println(
                    "[ACK] Mensaje procesado correctamente: " + cola
            );

        } else {
            channel.basicNack(deliveryTag, false, false);

            System.err.println(
                    "[NACK] Mensaje enviado a la DLQ: " + cola
            );
        }
    }
}