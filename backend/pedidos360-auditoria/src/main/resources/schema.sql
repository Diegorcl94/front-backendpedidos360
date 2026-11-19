CREATE TABLE IF NOT EXISTS auditoria_eventos (
    event_id VARCHAR(64) PRIMARY KEY,
    tipo_evento VARCHAR(100) NOT NULL,
    pedido_id BIGINT NOT NULL,
    producto VARCHAR(255) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    fecha VARCHAR(50) NOT NULL,
    particion INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL
);
