CREATE TABLE IF NOT EXISTS reporte_eventos (
    event_id VARCHAR(64) PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto VARCHAR(255) NOT NULL,
    cantidad INTEGER NOT NULL,
    total BIGINT NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha VARCHAR(50) NOT NULL
);
