package cl.duoc.pedidos360.dto;

import java.time.Instant;

public record PedidoEvento(
        String eventId,
        String eventType,
        Instant timestamp,
        Long pedidoId,
        String producto,
        Integer cantidad,
        Long total,
        String estado,
        String actor
) {
}
