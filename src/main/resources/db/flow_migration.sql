-- Additive and repeatable. Runs before Hibernate validation on existing databases.
CREATE TABLE IF NOT EXISTS ajustes (clave VARCHAR(80) PRIMARY KEY, valor VARCHAR(255) NOT NULL);
INSERT INTO ajustes(clave,valor) VALUES ('asignacion_automatica','true') ON CONFLICT(clave) DO NOTHING;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultima_asignacion TIMESTAMP;
ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS ticket_id BIGINT REFERENCES tickets(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_notificaciones_pendientes ON notificaciones(usuario_id,leida,fecha_creacion DESC);
CREATE TABLE IF NOT EXISTS ticket_correlativos (fecha DATE PRIMARY KEY, ultimo BIGINT NOT NULL);
