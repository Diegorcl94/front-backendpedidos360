package cl.duoc.rabbitadmin;

import cl.duoc.rabbitadmin.config.AdminApiKeyFilter;
import cl.duoc.rabbitadmin.config.AdminSecurityProperties;
import cl.duoc.rabbitadmin.controller.RabbitAdminController;
import cl.duoc.rabbitadmin.exception.AdminOperationException;
import cl.duoc.rabbitadmin.exception.GlobalExceptionHandler;
import cl.duoc.rabbitadmin.service.RabbitAdminService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RabbitAdminControllerTest {
    private final String key = UUID.randomUUID().toString();
    private RabbitAdminService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(RabbitAdminService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RabbitAdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AdminApiKeyFilter(new AdminSecurityProperties(key))).build();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("X-Admin-Api-Key", key);
    }

    @Test
    void todasLasOperacionesExigenClave() throws Exception {
        for (var request : new MockHttpServletRequestBuilder[]{
                get("/api/admin/health"), post("/api/admin/queues"), post("/api/admin/exchanges"),
                post("/api/admin/bindings"), delete("/api/admin/queues/demo.q"),
                delete("/api/admin/exchanges/demo.e"), delete("/api/admin/bindings")}) {
            mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"));
        }
        mvc.perform(get("/api/admin/health").header("X-Admin-Api-Key", "incorrecta"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rechazaOrigenWebYOrigenRemotoAunqueExistaClave() throws Exception {
        mvc.perform(auth(get("/api/admin/health")).header("Origin", "https://externo.example"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(auth(get("/api/admin/health")).with(request -> {
            request.setRemoteAddr("192.0.2.1");
            request.addHeader("X-Forwarded-For", "127.0.0.1");
            return request;
        })).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"\"}", "{\"name\":\"   \"}", "{\"name\":\"demo. con espacio\"}",
            "{\"name\":\"demo.\\ncontrol\"}", "{\"name\":\"demo/otro\"}", "no-es-json"})
    void rechazaJsonYNombresInvalidos(String json) throws Exception {
        for (String endpoint : new String[]{"queues", "exchanges"}) {
            mvc.perform(auth(post("/api/admin/" + endpoint)).contentType("application/json").content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void validaLongitudesYBindingsEnBodyYQuery() throws Exception {
        mvc.perform(auth(post("/api/admin/queues")).contentType("application/json")
                .content("{\"name\":\"" + "a".repeat(128) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/admin/bindings")).contentType("application/json")
                .content("{\"queue\":\"demo.q\",\"exchange\":\"demo.e\",\"routingKey\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(delete("/api/admin/bindings")).param("queue", "demo.q"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(delete("/api/admin/bindings")).param("queue", "demo.q")
                .param("exchange", "demo.e").param("routingKey", "a".repeat(256)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void endpointsDevuelven201Y204() throws Exception {
        mvc.perform(auth(post("/api/admin/queues")).contentType("application/json").content("{\"name\":\"demo.q\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/admin/queues/demo.q"));
        mvc.perform(auth(post("/api/admin/exchanges")).contentType("application/json").content("{\"name\":\"demo.e\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("direct"));
        mvc.perform(auth(post("/api/admin/bindings")).contentType("application/json")
                .content("{\"queue\":\"demo.q\",\"exchange\":\"demo.e\",\"routingKey\":\"demo.key\"}"))
                .andExpect(status().isCreated());
        mvc.perform(auth(delete("/api/admin/bindings")).param("queue", "demo.q")
                .param("exchange", "demo.e").param("routingKey", "demo.key"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(auth(delete("/api/admin/queues/demo.q"))).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/admin/exchanges/demo.e"))).andExpect(status().isNoContent());
        verify(service).crearCola("demo.q");
        verify(service).eliminarCola("demo.q");
    }

    @Test
    void erroresNoExponenDetallesInternos() throws Exception {
        doThrow(new AdminOperationException(HttpStatus.NOT_FOUND, "La cola no existe."))
                .when(service).eliminarCola("demo.ausente");
        mvc.perform(auth(delete("/api/admin/queues/demo.ausente"))).andExpect(status().isNotFound());
        doThrow(new AdminOperationException(HttpStatus.CONFLICT, "Ya existe."))
                .when(service).crearCola("demo.existe");
        mvc.perform(auth(post("/api/admin/queues")).contentType("application/json")
                .content("{\"name\":\"demo.existe\"}")).andExpect(status().isConflict());
        doThrow(new AmqpConnectException(new java.net.ConnectException("INTERNAL_SECRET")))
                .when(service).crearCola("demo.offline");
        var brokerError = mvc.perform(auth(post("/api/admin/queues")).contentType("application/json")
                .content("{\"name\":\"demo.offline\"}")).andExpect(status().isServiceUnavailable()).andReturn();
        assertThat(brokerError.getResponse().getContentAsString()).doesNotContain("INTERNAL_SECRET", key, "stackTrace");
        doThrow(new IllegalStateException("INTERNAL_SECRET")).when(service).crearCola("demo.fallo");
        var unexpected = mvc.perform(auth(post("/api/admin/queues")).contentType("application/json")
                .content("{\"name\":\"demo.fallo\"}")).andExpect(status().isInternalServerError()).andReturn();
        assertThat(unexpected.getResponse().getContentAsString()).doesNotContain("INTERNAL_SECRET", key, "IllegalStateException");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"404,404", "405,409", "406,409"})
    void traducePrecondicionesAtomicasDelBroker(int replyCode, int expectedHttp) throws Exception {
        var reason = new com.rabbitmq.client.AMQP.Channel.Close.Builder()
                .replyCode(replyCode).replyText("INTERNAL_SECRET").classId(50).methodId(40).build();
        var shutdown = new com.rabbitmq.client.ShutdownSignalException(false, false, reason, null);
        doThrow(new org.springframework.amqp.AmqpIOException(new java.io.IOException(shutdown)))
                .when(service).eliminarCola("demo.q");
        var result = mvc.perform(auth(delete("/api/admin/queues/demo.q")))
                .andExpect(status().is(expectedHttp)).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("INTERNAL_SECRET");
    }
}
