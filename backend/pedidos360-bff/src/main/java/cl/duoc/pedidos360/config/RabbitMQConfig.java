package cl.duoc.pedidos360.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Configuración anterior - Notificaciones
    public static final String EXCHANGE = "pedidos.exchange";
    public static final String QUEUE = "pedidos.notificaciones.queue";
    public static final String ROUTING_KEY = "pedido.creado";

    public static final String DLX = "pedidos.dlx";
    public static final String DLQ = "pedidos.dlq";
    public static final String DLQ_ROUTING_KEY = "pedido.error";

    // EV3 - Exchange Topic
    public static final String TOPIC_EXCHANGE = "pedidos.topic";

    // EV3 - Cocina
    public static final String COCINA_QUEUE = "pedidos.cocina.queue";
    public static final String COCINA_DLQ = "pedidos.cocina.dlq";
    public static final String COCINA_KEY = "pedido.cocina.ticket";
    public static final String COCINA_ERROR = "cocina.error";

    // EV3 - Facturación
    public static final String FACTURACION_QUEUE = "pedidos.facturacion.queue";
    public static final String FACTURACION_DLQ = "pedidos.facturacion.dlq";
    public static final String FACTURACION_KEY = "pedido.facturacion.generar";
    public static final String FACTURACION_ERROR = "facturacion.error";

    // Exchange Direct existente
    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ, true);
    }

    @Bean
    public Binding pedidosBinding() {
        return BindingBuilder.bind(notificacionesQueue())
                .to(pedidosExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    // EV3 - Nuevas colas, DLQ y Exchange Topic
    @Bean
    public Declarables topologiaEV3() {

        TopicExchange topic = new TopicExchange(
                TOPIC_EXCHANGE, true, false);

        Queue cocina = QueueBuilder.durable(COCINA_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(COCINA_ERROR)
                .build();

        Queue cocinaDLQ = new Queue(COCINA_DLQ, true);

        Queue facturacion = QueueBuilder.durable(FACTURACION_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(FACTURACION_ERROR)
                .build();

        Queue facturacionDLQ = new Queue(FACTURACION_DLQ, true);

        return new Declarables(
                topic,
                cocina,
                cocinaDLQ,
                facturacion,
                facturacionDLQ,

                BindingBuilder.bind(cocina)
                        .to(topic)
                        .with("pedido.cocina.#"),

                BindingBuilder.bind(facturacion)
                        .to(topic)
                        .with("pedido.facturacion.#"),

                BindingBuilder.bind(cocinaDLQ)
                        .to(deadLetterExchange())
                        .with(COCINA_ERROR),

                BindingBuilder.bind(facturacionDLQ)
                        .to(deadLetterExchange())
                        .with(FACTURACION_ERROR)
        );
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public ApplicationRunner verificarRabbitMQ(
            ConnectionFactory connectionFactory) {

        return args -> {
            System.out.println("Conectando Pedidos360 con RabbitMQ...");

            try (var connection = connectionFactory.createConnection()) {
                System.out.println("RabbitMQ conectado correctamente.");
            }
        };
    }
}