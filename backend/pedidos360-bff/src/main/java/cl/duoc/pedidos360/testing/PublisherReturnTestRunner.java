package cl.duoc.pedidos360.testing;

import cl.duoc.pedidos360.config.RabbitMQConfig;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Profile("publisher-return-test")
public class PublisherReturnTestRunner implements ApplicationRunner {

    private final RabbitTemplate rabbitTemplate;

    public PublisherReturnTestRunner(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        CorrelationData correlation = new CorrelationData(
                UUID.randomUUID().toString()
        );

        String mensaje = "PRUEBA_RETURN";

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                "pedido.inexistente",
                mensaje,
                correlation
        );

        CorrelationData.Confirm confirm =
                correlation.getFuture().get(10, TimeUnit.SECONDS);

        if (confirm.isAck()) {
            System.out.println(
                    "[PUBLISHER ACK] Exchange confirmo la publicacion."
            );
        } else {
            System.out.println(
                    "[PUBLISHER NACK] " + confirm.getReason()
            );
        }

        var returned = correlation.getReturned();

        if (returned != null) {
            System.out.println(
                    "[PUBLISHER RETURN] Mensaje sin destino."
            );

            System.out.println(
                    "[RETURN] Reply code: " + returned.getReplyCode()
            );

            System.out.println(
                    "[RETURN] Reply text: " + returned.getReplyText()
            );

            System.out.println(
                    "[RETURN] Routing key: " + returned.getRoutingKey()
            );
        } else {
            System.out.println(
                    "[RETURN] No se recibio devolucion."
            );
        }
    }
}
