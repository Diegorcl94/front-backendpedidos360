package cl.duoc.rabbitadmin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Pedidos360RabbitAdminApplicationTests {

    private static final String KEY = UUID.randomUUID().toString();
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("admin.security.api-key", () -> KEY);
        registry.add("spring.rabbitmq.port", () -> 1);
        registry.add("admin.rabbit-management-url", () -> "http://127.0.0.1:1");
    }

    @Test
    void saludExigeClaveYNoDependeDelBroker() throws Exception {
        mvc.perform(get("/api/admin/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/health").header("X-Admin-Api-Key", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void recursosCriticosSeRechazanAntesDeContactarRabbit() throws Exception {
        for (String name : new String[]{"pedidos.dlq", "pedidos.notificaciones.queue", "pedidos.exchange", "pedidos.dlx"}) {
            mvc.perform(delete("/api/admin/queues/{name}", name).header("X-Admin-Api-Key", KEY))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/admin/exchanges/{name}", name).header("X-Admin-Api-Key", KEY))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/admin/bindings").header("X-Admin-Api-Key", KEY)
                .contentType("application/json").content("""
                        {"queue":"pedidos.dlq","exchange":"demo.evaluacion.exchange","routingKey":"pedido.error"}
                        """))
                .andExpect(status().isForbidden());
    }

	@Test
	void contextLoads() {
	}

}
