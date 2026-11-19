package cl.duoc.pedidos360.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class EstadisticasController {

    private final RestClient cliente;

    @Value("${app.reportes-url:http://127.0.0.1:8083}")
    private String reportesUrl;

    @Value("${app.auditoria-url:http://127.0.0.1:8084}")
    private String auditoriaUrl;

    public EstadisticasController() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.cliente = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @GetMapping("/reportes/resumen")
    public JsonNode reportes() {
        return consultar(reportesUrl + "/api/reportes/resumen");
    }

    @GetMapping("/auditoria/eventos")
    public JsonNode auditoria() {
        return consultar(auditoriaUrl + "/api/auditoria/eventos");
    }

    private JsonNode consultar(String url) {
        try {
            return cliente.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Microservicio no disponible"
            );
        }
    }
}
