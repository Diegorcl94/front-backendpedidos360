package cl.duoc.pedidos360;

import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

// Aislado del BFF en ejecución y sin configuración RabbitMQ/JWT. Nunca activa mysql.
@ActiveProfiles("persistence-test")
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pedidos360-persistence-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenciaH2Test {
    @Autowired PedidoRepository repository;
    @Autowired EntityManager entityManager;
    @Autowired DataSource dataSource;

    @Test
    void h2GuardaYConsultaPedidoConIdGenerado() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
        Pedido guardado = repository.saveAndFlush(new Pedido(null, "Cliente prueba", "Teclado mecánico", 2, 69980L, "PENDIENTE"));
        Long id = guardado.getId();
        assertThat(id).isPositive();
        entityManager.clear();
        Pedido consultado = repository.findById(id).orElseThrow();
        assertThat(consultado.getProducto()).isEqualTo("Teclado mecánico");
        assertThat(consultado.getTotal()).isEqualTo(69980L);
    }

    @Test
    void cargaInicialNoSeDuplicaAlEjecutarseDosVeces() throws Exception {
        repository.deleteAll(); // Solo la H2 de prueba, dentro de una transacción con rollback.
        var cargaInicial = new Pedidos360Application().cargarDatos(repository);
        cargaInicial.run();
        List<Long> idsIniciales = repository.findAll().stream().map(Pedido::getId).sorted().toList();
        assertThat(idsIniciales).hasSize(3);
        cargaInicial.run();
        assertThat(repository.findAll().stream().map(Pedido::getId).sorted().toList()).isEqualTo(idsIniciales);
    }

    @Test
    void cargaInicialRespetaUnaTablaConDatos() throws Exception {
        repository.deleteAll();
        Pedido existente = repository.saveAndFlush(new Pedido(null, "Cliente existente", "Monitor", 1, 100000L, "PENDIENTE"));
        new Pedidos360Application().cargarDatos(repository).run();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(existente.getId())).isPresent();
    }
}
