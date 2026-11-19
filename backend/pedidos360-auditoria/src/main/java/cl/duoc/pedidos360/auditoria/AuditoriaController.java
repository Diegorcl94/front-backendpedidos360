package cl.duoc.pedidos360.auditoria;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final JdbcTemplate jdbc;

    public AuditoriaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/eventos")
    public List<Map<String, Object>> eventos() {

        return jdbc.queryForList("""
            SELECT
                event_id AS "eventId",
                tipo_evento AS "tipoEvento",
                pedido_id AS "pedidoId",
                producto AS "producto",
                estado AS "estado",
                actor AS "actor",
                fecha AS "fecha",
                particion AS "particion",
                kafka_offset AS "offset"
            FROM auditoria_eventos
            ORDER BY fecha DESC
            LIMIT 100
            """);
    }
}
