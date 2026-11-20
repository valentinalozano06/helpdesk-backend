-- Additive and repeatable. Runs before Hibernate validation on existing databases.
CREATE TABLE IF NOT EXISTS ajustes (clave VARCHAR(80) PRIMARY KEY, valor VARCHAR(255) NOT NULL);
INSERT INTO ajustes(clave,valor) VALUES ('asignacion_automatica','true') ON CONFLICT(clave) DO NOTHING;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultima_asignacion TIMESTAMP;
ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS ticket_id BIGINT REFERENCES tickets(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_notificaciones_pendientes ON notificaciones(usuario_id,leida,fecha_creacion DESC);
CREATE TABLE IF NOT EXISTS ticket_correlativos (fecha DATE PRIMARY KEY, ultimo BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS adjuntos (
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT       NOT NULL REFERENCES tickets(id),
    usuario_id     BIGINT       NOT NULL REFERENCES usuarios(id),
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_mime      VARCHAR(100) NOT NULL,
    tamano         BIGINT       NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_adjuntos_ticket ON adjuntos(ticket_id);

-- Clasificacion opcional para solicitantes que no conocen la categoria.
INSERT INTO categorias(nombre,descripcion)
VALUES ('Por clasificar','La persona solicitante no sabe a que categoria corresponde')
ON CONFLICT(nombre) DO NOTHING;

-- Estados adicionales para escalamiento y cierre no resuelto.
INSERT INTO estados_ticket(nombre,orden) VALUES
  ('requiere_revision',6), ('no_resuelto',7)
ON CONFLICT(nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS escalamientos (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    tecnico_id BIGINT NOT NULL REFERENCES usuarios(id),
    motivo VARCHAR(120) NOT NULL,
    diagnostico TEXT NOT NULL,
    acciones_intentadas TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    decision TEXT,
    decision_por_id BIGINT REFERENCES usuarios(id),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_decision TIMESTAMP,
    CONSTRAINT ck_escalamientos_estado CHECK (estado IN ('pendiente','devuelto','no_resuelto'))
);
CREATE INDEX IF NOT EXISTS idx_escalamientos_estado_fecha ON escalamientos(estado,fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_escalamientos_ticket ON escalamientos(ticket_id,fecha_creacion DESC);

CREATE TABLE IF NOT EXISTS escalamiento_evidencias (
    id BIGSERIAL PRIMARY KEY,
    escalamiento_id BIGINT NOT NULL REFERENCES escalamientos(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(100) NOT NULL,
    tamano BIGINT NOT NULL,
    ruta_archivo VARCHAR(500) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_escalamiento_evidencias ON escalamiento_evidencias(escalamiento_id);
