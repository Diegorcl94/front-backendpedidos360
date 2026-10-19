package cl.duoc.rabbitadmin;

import cl.duoc.rabbitadmin.dto.CrearBindingRequest;
import cl.duoc.rabbitadmin.exception.AdminOperationException;
import cl.duoc.rabbitadmin.service.RabbitAdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RabbitAdminServiceTest {
    private static final String BASE = "http://localhost:15672";
    private static final String Q = "/api/queues/%2F/demo.q";
    private static final String E = "/api/exchanges/%2F/demo.e";
    private static final String B = "/api/bindings/%2F/e/demo.e/q/demo.q";
    private static final String QUEUE = "{\"durable\":true,\"auto_delete\":false,\"exclusive\":false,\"messages\":0,\"consumers\":0}";
    private static final String EXCHANGE = "{\"type\":\"direct\",\"durable\":true,\"auto_delete\":false,\"internal\":false}";
    private static final String BINDING = "[{\"source\":\"demo.e\",\"routing_key\":\"demo.key\",\"arguments\":{}}]";
    private final CrearBindingRequest request = new CrearBindingRequest("demo.q", "demo.e", "demo.key");
    private AmqpAdmin admin;
    private RabbitTemplate template;
    private RabbitAdminService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        admin = mock(AmqpAdmin.class);
        template = mock(RabbitTemplate.class);
        var builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        service = new RabbitAdminService(admin, template, builder.build(), "/");
    }

    private void json(String path, String body) {
        server.expect(requestTo(BASE + path)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void absent(String path) {
        server.expect(requestTo(BASE + path)).andExpect(method(HttpMethod.GET)).andRespond(withResourceNotFound());
    }

    private void status(HttpStatus status, org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(AdminOperationException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(status));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pedidos.exchange", "pedidos.notificaciones.queue", "pedidos.dlx", "pedidos.dlq", "amq.direct", "otra.cola"})
    void nuncaModificaRecursosProtegidos(String name) {
        for (var action : List.<Runnable>of(() -> service.crearCola(name), () -> service.crearExchange(name),
                () -> service.eliminarCola(name), () -> service.eliminarExchange(name),
                () -> service.crearBinding(new CrearBindingRequest(name, "demo.e", "key")),
                () -> service.crearBinding(new CrearBindingRequest("demo.q", name, "key")),
                () -> service.eliminarBinding(new CrearBindingRequest(name, "demo.e", "key")),
                () -> service.eliminarBinding(new CrearBindingRequest("demo.q", name, "key")))) {
            status(HttpStatus.FORBIDDEN, action::run);
        }
        verifyNoInteractions(admin, template);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "demo. con espacio", "demo.\ncontrol", "demo/a"})
    void servicioTambienRechazaNombresInvalidos(String name) {
        status(HttpStatus.BAD_REQUEST, () -> service.eliminarCola(name));
        verifyNoInteractions(admin, template);
    }

    @Test
    void declaraRecursosDurablesSinAutodelete() {
        absent(Q); absent(E);
        when(admin.declareQueue(any(Queue.class))).thenReturn("demo.q");
        service.crearCola("demo.q"); service.crearExchange("demo.e");
        var queue = ArgumentCaptor.forClass(Queue.class);
        verify(admin).declareQueue(queue.capture());
        assertThat(queue.getValue().isDurable()).isTrue();
        assertThat(queue.getValue().isExclusive()).isFalse();
        assertThat(queue.getValue().isAutoDelete()).isFalse();
        var exchange = ArgumentCaptor.forClass(DirectExchange.class);
        verify(admin).declareExchange(exchange.capture());
        assertThat(exchange.getValue().isDurable()).isTrue();
        assertThat(exchange.getValue().getType()).isEqualTo("direct");
        server.verify();
    }

    @Test
    void noRedeclaraRecursosExistentes() {
        json(Q, QUEUE); json(E, EXCHANGE);
        status(HttpStatus.CONFLICT, () -> service.crearCola("demo.q"));
        status(HttpStatus.CONFLICT, () -> service.crearExchange("demo.e"));
        verifyNoInteractions(admin, template); server.verify();
    }

    @Test
    void declaracionSinConfirmacionNoEsExito() {
        absent(Q);
        status(HttpStatus.SERVICE_UNAVAILABLE, () -> service.crearCola("demo.q"));
    }

    @Test
    void eliminaSoloColaVaciaSinUsoYVerificaAusencia() {
        json(Q, QUEUE);
        json(Q + "/bindings", "[{\"source\":\"\",\"routing_key\":\"demo.q\",\"arguments\":{}}]");
        absent(Q);
        service.eliminarCola("demo.q");
        verify(admin).deleteQueue("demo.q", true, true);
        verify(admin, never()).deleteQueue(anyString()); server.verify();
    }

    @Test
    void colaConMensajesOConsumidoresNoSeBorra() {
        json(Q, QUEUE.replace("\"messages\":0", "\"messages\":1"));
        json(Q, QUEUE.replace("\"consumers\":0", "\"consumers\":1"));
        status(HttpStatus.CONFLICT, () -> service.eliminarCola("demo.q"));
        status(HttpStatus.CONFLICT, () -> service.eliminarCola("demo.q"));
        verifyNoInteractions(admin, template); server.verify();
    }

    @Test
    void colaConBindingsNoSeBorra() {
        json(Q, QUEUE); json(Q + "/bindings", BINDING);
        status(HttpStatus.CONFLICT, () -> service.eliminarCola("demo.q"));
        verifyNoInteractions(admin, template);
    }

    @Test
    void ausentesDevuelven404() {
        absent(Q); absent(E);
        status(HttpStatus.NOT_FOUND, () -> service.eliminarCola("demo.q"));
        status(HttpStatus.NOT_FOUND, () -> service.eliminarExchange("demo.e"));
        verifyNoInteractions(admin, template);
    }

    @Test
    void exchangeSeEliminaConIfUnused() throws Exception {
        json(E, EXCHANGE); json(E + "/bindings/source", "[]"); json(E + "/bindings/destination", "[]"); absent(E);
        var channel = mock(com.rabbitmq.client.Channel.class);
        when(template.execute(any())).thenAnswer(invocation -> {
            ChannelCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(channel);
        });
        service.eliminarExchange("demo.e");
        verify(channel).exchangeDelete("demo.e", true);
        verifyNoInteractions(admin); server.verify();
    }

    @Test
    void exchangeConBindingNoSeBorra() {
        json(E, EXCHANGE); json(E + "/bindings/source", BINDING);
        status(HttpStatus.CONFLICT, () -> service.eliminarExchange("demo.e"));
        verifyNoInteractions(admin, template);
    }

    @Test
    void tiposIncompatiblesNoSeModifican() {
        json(Q, QUEUE); json(E, EXCHANGE.replace("direct", "topic"));
        status(HttpStatus.CONFLICT, () -> service.crearBinding(request));
        verifyNoInteractions(admin, template);
    }

    @Test
    void creaYEliminaElBindingExacto() {
        json(Q, QUEUE); json(E, EXCHANGE); json(B, "[]");
        json(Q, QUEUE); json(E, EXCHANGE); json(B, BINDING); json(B, "[]");
        service.crearBinding(request); service.eliminarBinding(request);
        var binding = ArgumentCaptor.forClass(Binding.class);
        verify(admin).declareBinding(binding.capture());
        assertThat(binding.getValue().getDestination()).isEqualTo("demo.q");
        assertThat(binding.getValue().getExchange()).isEqualTo("demo.e");
        assertThat(binding.getValue().getRoutingKey()).isEqualTo("demo.key");
        verify(admin).removeBinding(any(Binding.class)); server.verify();
    }

    @Test
    void bindingDuplicado409YBindingInexistente404() {
        json(Q, QUEUE); json(E, EXCHANGE); json(B, BINDING);
        json(Q, QUEUE); json(E, EXCHANGE); json(B, "[]");
        status(HttpStatus.CONFLICT, () -> service.crearBinding(request));
        status(HttpStatus.NOT_FOUND, () -> service.eliminarBinding(request));
        verifyNoInteractions(admin, template);
    }

    @Test
    void noInforma204SiElRecursoSigueExistiendo() {
        json(Q, QUEUE); json(Q + "/bindings", "[]"); json(Q, QUEUE);
        status(HttpStatus.CONFLICT, () -> service.eliminarCola("demo.q"));
    }

    @Test
    void managementInaccesibleNoProvocaMutaciones() {
        server.expect(requestTo(BASE + Q)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        status(HttpStatus.SERVICE_UNAVAILABLE, () -> service.crearCola("demo.q"));
        verifyNoInteractions(admin, template);
    }
}
