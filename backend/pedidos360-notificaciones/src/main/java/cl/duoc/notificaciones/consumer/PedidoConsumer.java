package cl.duoc.notificaciones.consumer;

import com.rabbitmq.client.Channel;
import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoConsumer {

    private static final int MAX_INTENTOS = 3;

    @RabbitListener(queues = "pedidos.notificaciones.queue")
    public void recibirPedido(
            String mensaje,
            Message message,
            Channel channel) throws IOException {

        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        boolean procesado = false;

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {

            try {
                System.out.println(
                    "[CONSUMER] Intento " + intento
                    + " de " + MAX_INTENTOS
                    + ": " + mensaje
                );

                // Error definitivo: mensaje invalido
                if (mensaje.contains("ERROR_PRUEBA")) {
                    throw new IllegalArgumentException(
                        "Mensaje invalido de prueba"
                    );
                }

                // Error temporal simulado
                if (mensaje.contains("ERROR_TEMPORAL")) {
                    throw new IllegalStateException(
                        "Servicio temporalmente no disponible"
                    );
                }

                System.out.println(
                    "[CONSUMER] Procesamiento exitoso"
                );

                procesado = true;
                break;

            } catch (IllegalArgumentException error) {

                System.err.println(
                    "[ERROR DEFINITIVO] " + error.getMessage()
                );

                // No tiene sentido reintentar
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
                "[ACK] Mensaje confirmado correctamente."
            );

        } else {

            channel.basicNack(deliveryTag, false, false);

            System.err.println(
                "[NACK] Intentos agotados o mensaje invalido."
            );
        }
    }
}
