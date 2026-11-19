package cl.duoc.pedidos360.reportes;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final JdbcTemplate jdbc;

    public ReportesController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {

        return jdbc.queryForMap("""
            SELECT
                COUNT(*) AS "pedidosProcesados",
                COALESCE(SUM(cantidad), 0) AS "unidadesSolicitadas",
                COALESCE(SUM(total), 0) AS "montoTotal"
            FROM reporte_eventos
            """);
    }
}
