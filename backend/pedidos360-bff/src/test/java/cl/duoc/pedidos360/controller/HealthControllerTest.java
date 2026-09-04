package cl.duoc.pedidos360.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void healthDebeResponderOk() {
        HealthController controller = new HealthController();
        assertEquals("OK", controller.health().get("status"));
    }
}
