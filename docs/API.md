# Helpdesk Morales: API e Insomnia

Base local: `http://localhost:8080/api`. PostgreSQL sigue siendo la base de datos.
Documentación interactiva: `http://localhost:8080/docs`. Especificación: `http://localhost:8080/v3/api-docs`.

## Importar y empezar

1. En Insomnia selecciona **Import → File** y abre `helpdesk-insomnia.json` de esta carpeta. Crea una colección nueva; no reemplaza otros proyectos.
2. Selecciona el entorno **Local**. Completa `email_admin`, `password_admin` y los pares equivalentes para `jefatura`, `tecnico` y `solicitante`, usando cuentas activas existentes.
3. Envía los cuatro login de **01 · Autenticación**. Cada uno guarda su propio `token_<rol>`; no se mezclan identidades. Un login fallido borra únicamente el token de ese rol.
4. Consulta los catálogos. Sus scripts guardan IDs válidos. Para crear un usuario, completa una contraseña de prueba y utiliza un correo ficticio que todavía no exista.
5. Prueba usuarios: crear → consultar → editar → desactivar → activar. `usuario_id` se guarda al crear. No uses el ID de una cuenta real para este recorrido.
6. Prueba tickets: crear como solicitante → asignar como jefatura → iniciar como técnico → comentar con cuentas distintas → resolver como técnico → cerrar como jefatura. Cambia `estado_id` usando los IDs del catálogo. `tecnico_id` debe coincidir con el usuario autenticado en Login técnico.
7. Consulta el seguimiento y las notificaciones. Cada solicitud indica el rol y código HTTP esperado.

No ejecutes toda la colección en lote contra datos reales: contiene altas y cambios de estado/configuración. La carpeta 99 está separada y su script exige `confirmar_eliminacion=true`. Las pruebas automatizadas repetibles se ejecutan con la base temporal descrita abajo.

El archivo usa el [formato JSON v4 importable por Insomnia](https://developer.konghq.com/insomnia/import-export/). La captura de tokens usa [scripts posteriores a la respuesta](https://developer.konghq.com/how-to/set-a-value-from-a-response-as-an-environment-variable/). No contiene contraseñas reales ni tokens.

## Contratos y permisos

| Método | Ruta (prefijo /api) | Acceso y respuesta |
|---|---|---|
| POST | /auth/login | Público; 200 token, tipo, usuarioId, nombreCompleto, rol, expiraEnMs |
| GET | /usuarios | ADMIN/JEFATURA; 200 lista |
| GET | /usuarios/{id} | ADMIN/JEFATURA; 200 usuario |
| GET | /usuarios/tecnicos | ADMIN/JEFATURA; 200 técnicos activos {id,nombre} |
| POST | /usuarios | ADMIN; 201 usuario activo |
| PUT | /usuarios/{id} | ADMIN; 200 usuario actualizado |
| PATCH | /usuarios/{id}/estado | ADMIN; {estado: activo o inactivo}; 200 |
| DELETE | /usuarios/{id} | ADMIN; 204 eliminación o desactivación si hay historial |
| POST | /tickets | SOLICITANTE; 201 ticket |
| GET | /tickets | 200 lista según identidad/rol |
| GET | /tickets/{id} | 200 detalle autorizado |
| PATCH | /tickets/{id}/asignar | ADMIN/JEFATURA; {tecnicoId}; 200 |
| PATCH | /tickets/{id}/prioridad | ADMIN/JEFATURA; {prioridadId}; 200 |
| PATCH | /tickets/{id}/estado | ADMIN/JEFATURA o técnico asignado; {estadoId}; 200 |
| GET | /tickets/{id}/comentarios | Usuario con acceso al ticket; 200 lista |
| POST | /tickets/{id}/comentarios | Usuario con acceso al ticket; {contenido}; 201 comentario |
| GET | /catalogos/areas, /categorias, /prioridades, /estados | Autenticado; 200 listas {id,nombre} |
| GET | /notificaciones | Autenticado; 200 {items,noLeidas} propios |
| PATCH | /notificaciones/{id}/leida | Propietario; 200 aviso |
| POST | /notificaciones/leer-todas | Autenticado; 200 bandeja propia |
| GET | /ajustes | ADMIN; 200 {asignacionAutomatica} |
| PUT | /ajustes | ADMIN; {asignacionAutomatica: boolean}; 200 |

`POST /auth/registro` se conserva solo por compatibilidad, restringido a ADMIN y marcado obsoleto. Usa `POST /usuarios`; el registro heredado devuelve un token de la nueva cuenta que no debe sustituir el token del administrador.

Usuarios: cuerpo con `nombres`, `apellidos`, `email`, `password`, `rol`, `areaId`. Roles: `rol_admin`, `rol_jefatura`, `rol_tecnico`, `rol_solicitante`. Correo normalizado y único; nombres/apellidos máximo 100 caracteres, correo 150. Contraseña obligatoria al crear, mínimo 6 caracteres; `null` o vacía al editar conserva la actual. `areaId` es opcional y `null` al editar elimina el área. Activación mediante PATCH separado, como en Angular.

Los DTO nunca devuelven contraseñas ni hashes. No puedes eliminar/desactivar tu cuenta ni quitarte tu propio rol administrador. Una cuenta inactiva no puede iniciar sesión ni reutilizar un token anterior.

Tickets: crear con `asunto` (máximo 200), `descripcion`, `categoriaId`. La prioridad la determina el backend para solicitantes. El técnico solo consulta/atiende tickets asignados (y puede consultar los propios), e inicia desde abierto/en_espera y resuelve desde en_progreso; no cierra ni reabre. ADMIN/JEFATURA tienen vista global y gestionan asignaciones/prioridades. Los comentarios admiten de 1 a 2000 caracteres no vacíos; el autor siempre sale del token. Las notificaciones ajenas devuelven 404.

Errores: JSON con `timestamp`, `status`, `error`, `message`, `path`, `detalles`. 400: entrada inválida; 401: credenciales/token ausente, inválido o cuenta inactiva; 403: permisos; 404: recurso inexistente/no accesible en notificaciones; 422: regla de negocio, duplicados o referencias incompatibles. No se envían detalles internos de SQL.

## Pruebas automatizadas aisladas

Desde la raíz del backend:

```powershell
docker compose -p helpdesk-api-tests -f compose.api-test.yml up --abort-on-container-exit --exit-code-from tests
docker compose -p helpdesk-api-tests -f compose.api-test.yml down
```

Usa PostgreSQL 16 temporal, sin puerto publicado ni volumen de datos persistente. Las cuentas son ficticias; correos e IA externos están deshabilitados. La clase de integración solo se habilita con `HELPDESK_API_TESTS=true`, apunta a `helpdesk_api_test` y verifica el nombre antes de limpiar sus fixtures. No reutilizar esa variable fuera de este entorno de pruebas. Maven conserva sus dependencias en una caché de Docker y guarda resultados en `target/surefire-reports`.

El inicializador histórico `DatosInicialesRunner` queda deshabilitado por defecto. No activar `app.bootstrap.legacy-enabled`: es una operación heredada que modifica cuentas y puede limpiar datos, no una migración requerida para estas APIs. Esta entrega no ejecuta cambios destructivos sobre la base existente.

No se agregan todavía recuperación de contraseña, adjuntos ni nuevos endpoints de reportes. Se conservan las rutas y DTO consumidos por Angular.
