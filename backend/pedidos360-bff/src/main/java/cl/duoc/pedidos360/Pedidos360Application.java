package cl.duoc.pedidos360;

import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Pedidos360Application {

    public static void main(String[] args) {
        SpringApplication.run(Pedidos360Application.class, args);
    }

    @Bean
    CommandLineRunner cargarDatos(PedidoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Pedido(null, "Cliente Norte", "Teclado mecánico", 2, 69980L, "PENDIENTE"));
                repository.save(new Pedido(null, "Cliente Centro", "Monitor 24 pulgadas", 1, 129990L, "EN_PREPARACION"));
                repository.save(new Pedido(null, "Cliente Sur", "Mouse inalámbrico", 3, 44970L, "DESPACHADO"));
            }
        };
    }
}
