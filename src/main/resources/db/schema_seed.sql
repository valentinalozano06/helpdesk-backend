-- ============================================================
-- Semilla de catalogos base para el Sistema Help Desk
-- Municipalidad Distrital de Morales.
-- Complementa el DDL del Entregable 2 (Diseno de Base de Datos).
-- Ejecutar una sola vez despues de crear las tablas.
-- ============================================================

INSERT INTO roles (nombre, descripcion) VALUES
  ('rol_solicitante', 'Trabajador municipal que registra y consulta tickets'),
  ('rol_tecnico',      'Soporte TI que atiende, comenta y resuelve tickets'),
  ('rol_jefatura',     'Jefatura TI: reportes, indicadores y asignacion'),
  ('rol_admin',        'Administrador TI: gestion total del sistema')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO areas (nombre) VALUES
  ('Tecnologias de la Informacion'),
  ('Administracion'),
  ('Rentas'),
  ('Obras Publicas'),
  ('Secretaria General')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO categorias (nombre, descripcion) VALUES
  ('Hardware', 'Equipos, impresoras y perifericos'),
  ('Software', 'Sistemas, aplicativos y licencias'),
  ('Red',      'Internet, VLAN, VPN y conectividad'),
  ('Accesos',  'Usuarios, contrasenas y permisos')
  ,('Por clasificar', 'La persona solicitante no sabe a que categoria corresponde')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO prioridades (nombre, nivel) VALUES
  ('baja', 1), ('media', 2), ('alta', 3), ('critica', 4)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO estados_ticket (nombre, orden) VALUES
  ('abierto', 1), ('en_progreso', 2), ('en_espera', 3), ('resuelto', 4), ('cerrado', 5),
  ('requiere_revision', 6), ('no_resuelto', 7)
ON CONFLICT (nombre) DO NOTHING;
