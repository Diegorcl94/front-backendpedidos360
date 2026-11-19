package cl.duoc.pedidos360.controller;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {

    private final AdminClient admin;

    public KafkaAdminController(KafkaAdmin kafkaAdmin) {
        this.admin = AdminClient.create(
            kafkaAdmin.getConfigurationProperties()
        );
    }

    @GetMapping("/topics")
    @PreAuthorize("hasAuthority('SCOPE_Pedidos.Read')")
    public List<TopicInfo> listarTopics() throws Exception {

        Set<String> nombres = admin.listTopics()
                .names().get(10, TimeUnit.SECONDS);

        List<TopicInfo> resultado = new ArrayList<>();

        for (String nombre : new TreeSet<>(nombres)) {

            TopicDescription descripcion = admin
                .describeTopics(List.of(nombre))
                .allTopicNames()
                .get(10, TimeUnit.SECONDS)
                .get(nombre);

            ConfigResource recurso = new ConfigResource(
                ConfigResource.Type.TOPIC, nombre
            );

            Config config = admin.describeConfigs(List.of(recurso))
                .all().get(10, TimeUnit.SECONDS)
                .get(recurso);

            int replicas = descripcion.partitions().isEmpty()
                ? 0
                : descripcion.partitions().get(0).replicas().size();

            resultado.add(new TopicInfo(
                nombre,
                descripcion.partitions().size(),
                replicas,
                config.get("min.insync.replicas").value(),
                config.get("retention.ms").value(),
                config.get("cleanup.policy").value()
            ));
        }

        return resultado;
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('SCOPE_Pedidos.Read')")
    public List<GroupInfo> listarGrupos() throws Exception {

        return admin.listConsumerGroups()
            .all().get(10, TimeUnit.SECONDS)
            .stream()
            .map(grupo -> new GroupInfo(
                grupo.groupId(),
                grupo.state().map(Enum::name).orElse("UNKNOWN")
            ))
            .toList();
    }

    @PostMapping("/topics")
    @PreAuthorize("hasAuthority('SCOPE_Pedidos.Write')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> crearTopic(
            @RequestBody CrearTopicRequest datos) throws Exception {

        validarNombre(datos.nombre());

        if (datos.particiones() < 1 || datos.particiones() > 12 ||
            datos.replicas() < 1 || datos.replicas() > 3) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Particiones o replicas invalidas"
            );
        }

        validarConfig(datos.minIsr(), datos.replicas(),
                datos.retencionMs(), datos.politica());

        NewTopic topic = new NewTopic(
            datos.nombre(),
            datos.particiones(),
            (short) datos.replicas()
        ).configs(Map.of(
            "min.insync.replicas", String.valueOf(datos.minIsr()),
            "retention.ms", String.valueOf(datos.retencionMs()),
            "cleanup.policy", datos.politica()
        ));

        admin.createTopics(List.of(topic))
            .all().get(15, TimeUnit.SECONDS);

        return Map.of("mensaje", "Topic creado correctamente");
    }

    @PutMapping("/topics/{nombre}")
    @PreAuthorize("hasAuthority('SCOPE_Pedidos.Write')")
    public Map<String, String> modificarTopic(
            @PathVariable("nombre") String nombre,
            @RequestBody ConfigTopicRequest datos) throws Exception {

        validarNombre(nombre);

        var descripcion = admin.describeTopics(List.of(nombre))
            .allTopicNames().get(10, TimeUnit.SECONDS)
            .get(nombre);

        int replicas = descripcion.partitions().stream()
            .mapToInt(p -> p.replicas().size())
            .min().orElse(0);

        validarConfig(datos.minIsr(), replicas,
                datos.retencionMs(), datos.politica());

        ConfigResource recurso = new ConfigResource(
            ConfigResource.Type.TOPIC, nombre
        );

        List<AlterConfigOp> cambios = List.of(
            new AlterConfigOp(
                new ConfigEntry("retention.ms",
                    String.valueOf(datos.retencionMs())),
                AlterConfigOp.OpType.SET
            ),
            new AlterConfigOp(
                new ConfigEntry("cleanup.policy", datos.politica()),
                AlterConfigOp.OpType.SET
            ),
            new AlterConfigOp(
                new ConfigEntry("min.insync.replicas",
                    String.valueOf(datos.minIsr())),
                AlterConfigOp.OpType.SET
            )
        );

        admin.incrementalAlterConfigs(Map.of(recurso, cambios))
            .all().get(15, TimeUnit.SECONDS);

        return Map.of("mensaje", "Topic modificado correctamente");
    }

    @DeleteMapping("/topics/{nombre}")
    @PreAuthorize("hasAuthority('SCOPE_Pedidos.Write')")
    public Map<String, String> eliminarTopic(
            @PathVariable("nombre") String nombre) throws Exception {

        validarNombre(nombre);

        admin.deleteTopics(List.of(nombre))
            .all().get(15, TimeUnit.SECONDS);

        return Map.of("mensaje", "Eliminacion solicitada correctamente");
    }

    private void validarNombre(String nombre) {
        if (nombre == null ||
            !nombre.matches("demo\\.[a-zA-Z0-9-]{1,50}")) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se permiten topics de prueba con prefijo demo."
            );
        }
    }

    private void validarConfig(int minIsr, int replicas,
                               long retencionMs, String politica) {

        if (minIsr < 1 || minIsr > replicas ||
            retencionMs < 60000 ||
            retencionMs > 2592000000L ||
            politica == null ||
            !Set.of("delete", "compact", "compact,delete",
                    "delete,compact").contains(politica)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Configuracion del topic invalida"
            );
        }
    }

    public record CrearTopicRequest(
        String nombre,
        int particiones,
        int replicas,
        int minIsr,
        long retencionMs,
        String politica
    ) {}

    public record ConfigTopicRequest(
        int minIsr,
        long retencionMs,
        String politica
    ) {}

    public record TopicInfo(
        String nombre,
        int particiones,
        int replicas,
        String minIsr,
        String retencionMs,
        String politica
    ) {}

    public record GroupInfo(
        String grupo,
        String estado
    ) {}

    @PreDestroy
    public void cerrar() {
        admin.close(Duration.ofSeconds(5));
    }
}
