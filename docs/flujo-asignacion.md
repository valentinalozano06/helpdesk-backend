# Reparto y cola técnica

La migración `db/flow_migration.sql` es aditiva y se ejecuta con Spring SQL init antes de Hibernate validate. No requiere recrear la base. El ajuste asignacion_automatica se inicializa solo si no existe; reiniciar conserva su valor.

Crear, reasignar, cambiar estado y cambiar el interruptor toman el mismo bloqueo transaccional de PostgreSQL. La elección considera técnicos activos, número de tickets no resueltos/cerrados, última asignación y finalmente ID. Sin técnicos elegibles el ticket queda sin asignar. Resolver no redistribuye tickets existentes. El contador diario inicializa desde códigos existentes y evita duplicados concurrentes.

GET/PUT /api/ajustes: solo ADMIN, JSON {"asignacionAutomatica":true}.
GET /api/notificaciones: {items:[{id,tipoEvento,mensaje,leida,fechaCreacion,ticketId}],noLeidas}. Hasta 50 recientes; conteo total pendiente. Las notificaciones antiguas conservan ticketId null.
PATCH /api/notificaciones/{id}/leida y POST /api/notificaciones/leer-todas: únicamente la bandeja del usuario autenticado.
TicketResponse incluye solicitanteId y asignadoAId. El técnico lista asignados y solicitudes propias. Puede leer/comentar ambos; solo gestionar estados de los asignados: abierto/en_espera → en_progreso → resuelto. ADMIN/JEFATURA conservan control global. Solo SOLICITANTE (trabajadores/usuarios) puede crear tickets. ADMIN y JEFATURA mantienen sus funciones de supervisión; TÉCNICO atiende asignaciones. La restricción se aplica al endpoint POST, la ruta y los accesos visuales.

## Verificación realizada en una copia aislada

- Migración sobre copia de 23 tickets sin pérdida.
- Nueve creaciones HTTP simultáneas: códigos distintos y reparto 3/3/3.
- Resolver uno reduce la carga; el siguiente se asigna al técnico liberado.
- Rechazo de lectura/comentarios/gestión de tickets ajenos.
- Propiedad de notificaciones, lectura individual y masiva.
- Regla vigente: creación solo por SOLICITANTE; POST de ADMIN/JEFATURA/TÉCNICO debe responder 403. Gestión Jefatura, interruptor apagado y ausencia de técnicos activos.
- Pruebas UI con API real aislada, sin modificar la base de uso diario.

La campana y la cola consultan al entrar, al recuperar foco y cada 30 segundos con la pestaña visible. Las preferencias visuales continúan locales; el interruptor de reparto es global y se guarda inmediatamente.
