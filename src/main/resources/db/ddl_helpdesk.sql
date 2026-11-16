-- ============================================================
-- DDL - Sistema Help Desk, Municipalidad Distrital de Morales
-- Corresponde 1 a 1 con las entidades JPA del backend
-- (com.morales.helpdesk.model.*). Ejecutar ANTES de schema_seed.sql.
-- ============================================================

-- Catalogos (sin dependencias)

CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(40)  NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS areas (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS categorias (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS prioridades (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    nivel  INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS estados_ticket (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    orden  INTEGER     NOT NULL
);

-- Usuarios (depende de roles y areas)

CREATE TABLE IF NOT EXISTS usuarios (
    id             BIGSERIAL PRIMARY KEY,
    nombres        VARCHAR(100)  NOT NULL,
    apellidos      VARCHAR(100)  NOT NULL,
    email          VARCHAR(150)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255)  NOT NULL,
    rol_id         BIGINT        NOT NULL REFERENCES roles(id),
    area_id        BIGINT        REFERENCES areas(id),
    estado         VARCHAR(20)   NOT NULL DEFAULT 'activo',
    fecha_creacion TIMESTAMP     NOT NULL DEFAULT now()
);

-- Tickets (depende de categorias, prioridades, estados_ticket, usuarios)

CREATE TABLE IF NOT EXISTS tickets (
    id                         BIGSERIAL PRIMARY KEY,
    codigo_unico               VARCHAR(30)  NOT NULL UNIQUE,
    asunto                     VARCHAR(200) NOT NULL,
    descripcion                TEXT,
    categoria_id               BIGINT       NOT NULL REFERENCES categorias(id),
    prioridad_id               BIGINT       NOT NULL REFERENCES prioridades(id),
    estado_id                  BIGINT       NOT NULL REFERENCES estados_ticket(id),
    solicitante_id             BIGINT       NOT NULL REFERENCES usuarios(id),
    asignado_a_id              BIGINT       REFERENCES usuarios(id),
    fecha_creacion             TIMESTAMP    NOT NULL DEFAULT now(),
    fecha_ultima_actualizacion TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_solicitante ON tickets(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_tickets_asignado    ON tickets(asignado_a_id);

-- Comentarios, historial, notificaciones y evidencia adjunta (dependen de tickets/usuarios)

CREATE TABLE IF NOT EXISTS comentarios (
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT    NOT NULL REFERENCES tickets(id),
    usuario_id     BIGINT    NOT NULL REFERENCES usuarios(id),
    contenido      TEXT      NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comentarios_ticket ON comentarios(ticket_id);

CREATE TABLE IF NOT EXISTS historial_cambios (
    id                BIGSERIAL PRIMARY KEY,
    ticket_id         BIGINT      NOT NULL REFERENCES tickets(id),
    usuario_id        BIGINT      NOT NULL REFERENCES usuarios(id),
    campo_modificado  VARCHAR(60) NOT NULL,
    valor_anterior    VARCHAR(255),
    valor_nuevo       VARCHAR(255),
    fecha_cambio      TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_historial_ticket ON historial_cambios(ticket_id);

CREATE TABLE IF NOT EXISTS notificaciones (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL REFERENCES usuarios(id),
    tipo_evento    VARCHAR(60) NOT NULL,
    mensaje        TEXT        NOT NULL,
    leida          BOOLEAN     NOT NULL DEFAULT false,
    fecha_creacion TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario ON notificaciones(usuario_id);

-- Evidencia adjunta por ticket (imagenes PNG/JPG o PDF hasta 10 MB cada uno)

CREATE TABLE IF NOT EXISTS adjuntos (
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT       NOT NULL REFERENCES tickets(id),
    usuario_id    BIGINT       NOT NULL REFERENCES usuarios(id),
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_mime      VARCHAR(100) NOT NULL,
    tamano         BIGINT       NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_adjuntos_ticket ON adjuntos(ticket_id);
