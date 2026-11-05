package com.morales.helpdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.HashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Run only through compose.api-test.yml: never against the application database. */
@EnabledIfEnvironmentVariable(named = "HELPDESK_API_TESTS", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://${DB_HOST:postgres}:5432/helpdesk_api_test",
    "spring.datasource.username=api_test", "spring.datasource.password=api_test_only",
    "spring.sql.init.schema-locations=classpath:db/ddl_helpdesk.sql,classpath:db/flow_migration.sql",
    "app.bootstrap.legacy-enabled=false", "app.notifications.email-enabled=false", "app.ai.enabled=false",
    "logging.level.com.morales.helpdesk=WARN", "logging.file.name=target/api-test.log"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;
    @Autowired ApplicationContext context;
    Map<String, Long> ids;
    Map<String, String> tokens;

    @BeforeEach
    void fixtures() {
        assertThat(db.queryForObject("select current_database()", String.class)).isEqualTo("helpdesk_api_test");
        db.execute("TRUNCATE notificaciones, comentarios, historial_cambios, tickets, usuarios, roles, areas, categorias, prioridades, estados_ticket, ticket_correlativos RESTART IDENTITY CASCADE");
        db.update("INSERT INTO roles(nombre) VALUES ('rol_admin'),('rol_jefatura'),('rol_tecnico'),('rol_solicitante')");
        db.update("INSERT INTO areas(nombre) VALUES ('Area de prueba')");
        db.update("INSERT INTO categorias(nombre) VALUES ('Red')");
        db.update("INSERT INTO prioridades(nombre,nivel) VALUES ('baja',1),('media',2),('alta',3)");
        db.update("INSERT INTO estados_ticket(nombre,orden) VALUES ('abierto',1),('en_progreso',2),('en_espera',3),('resuelto',4),('cerrado',5)");
        db.update("UPDATE ajustes SET valor='false' WHERE clave='asignacion_automatica'");
        ids = new HashMap<>(); tokens = new HashMap<>();
        for (String role : new String[]{"admin","jefatura","tecnico","solicitante","otro"}) {
            ids.put(role, db.queryForObject("INSERT INTO usuarios(nombres,apellidos,email,password_hash,rol_id,area_id) VALUES (?,?,?, ?, (SELECT id FROM roles WHERE nombre=?),1) RETURNING id",
                Long.class, role, "Prueba", role+"@example.test", encoder.encode("Prueba123!"), "rol_"+(role.equals("otro")?"solicitante":role)));
        }
    }

    JsonNode call(String method, String path, String token, Object body, int expected) throws Exception {
        var r = request(HttpMethod.valueOf(method), path).contentType("application/json");
        if (token != null) r.header("Authorization", "Bearer " + token);
        if (body != null) r.content(body instanceof String ? (String)body : json.writeValueAsString(body));
        String response = mvc.perform(r).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }
    String token(String role) throws Exception {
        if (!tokens.containsKey(role)) tokens.put(role, call("POST","/api/auth/login",null,
            Map.of("email",role+"@example.test","password","Prueba123!"),200).get("token").asText());
        return tokens.get(role);
    }
    Map<String,Object> user(String email, String role) {
        return Map.of("nombres"," Usuario ","apellidos"," Prueba ","email",email,"password","Prueba123!","rol",role,"areaId",1);
    }
    long ticket() throws Exception {
        return call("POST","/api/tickets",token("solicitante"),Map.of("asunto","Sin internet","descripcion","No conecta","categoriaId",1),201).get("id").asLong();
    }

    @Test void loginAndRegistrationSecurity() throws Exception {
        assertThat(call("POST","/api/auth/login",null,Map.of("email"," ADMIN@EXAMPLE.TEST ","password","Prueba123!"),200).get("rol").asText()).isEqualTo("rol_admin");
        call("POST","/api/auth/login",null,Map.of("email","admin@example.test","password","incorrecta"),401);
        call("POST","/api/auth/registro",null,user("legacy@example.test","rol_admin"),401);
        call("POST","/api/auth/registro",token("solicitante"),user("legacy@example.test","rol_admin"),403);
        call("POST","/api/auth/registro",token("admin"),user("legacy@example.test","rol_solicitante"),201);
        assertThat(context.getBeansOfType(com.morales.helpdesk.config.DatosInicialesRunner.class)).isEmpty();
    }

    @Test void crudUsersAndNormalization() throws Exception {
        String admin = token("admin");
        JsonNode u = call("POST","/api/usuarios",admin,user(" NUEVO@EXAMPLE.TEST ","rol_solicitante"),201);
        long id = u.get("id").asLong();
        assertThat(u.get("email").asText()).isEqualTo("nuevo@example.test");
        assertThat(u.has("passwordHash")).isFalse();
        assertThat(u.has("password")).isFalse();
        assertThat(call("GET","/api/usuarios/"+id,admin,null,200).get("area").asText()).isEqualTo("Area de prueba");
        call("POST","/api/usuarios",admin,user("nuevo@example.test","rol_solicitante"),422);
        Map<String,Object> update = new HashMap<>(user("nuevo@example.test","rol_tecnico"));
        update.put("password", "");
        call("PUT","/api/usuarios/"+id,admin,update,200);
        call("POST","/api/auth/login",null,Map.of("email","nuevo@example.test","password","Prueba123!"),200);
        call("PATCH","/api/usuarios/"+id+"/estado",admin,Map.of("estado","inactivo"),200);
        call("PATCH","/api/usuarios/"+id+"/estado",admin,Map.of("estado","activo"),200);
        call("DELETE","/api/usuarios/"+id,admin,null,204);
        call("GET","/api/usuarios/"+id,admin,null,404);
    }

    @Test void userPermissionsAndSelfProtection() throws Exception {
        String admin = token("admin"); long id = ids.get("admin");
        call("DELETE","/api/usuarios/"+id,admin,null,422);
        call("PATCH","/api/usuarios/"+id+"/estado",admin,Map.of("estado","inactivo"),422);
        call("PUT","/api/usuarios/"+id,admin,user("admin@example.test","rol_tecnico"),422);
        for (String role : new String[]{"admin","jefatura"}) {
            call("GET","/api/usuarios",token(role),null,200);
            call("GET","/api/usuarios/"+id,token(role),null,200);
            call("GET","/api/usuarios/tecnicos",token(role),null,200);
        }
        for (String role : new String[]{"jefatura","tecnico","solicitante"}) {
            call("POST","/api/usuarios",token(role),user("denied@example.test","rol_admin"),403);
            call("PUT","/api/usuarios/"+id,token(role),user("denied@example.test","rol_admin"),403);
            call("DELETE","/api/usuarios/"+id,token(role),null,403);
            call("PATCH","/api/usuarios/"+id+"/estado",token(role),Map.of("estado","inactivo"),403);
        }
        call("GET","/api/usuarios",token("solicitante"),null,403);
        call("GET","/api/usuarios",token("tecnico"),null,403);
    }

    @Test void inactiveAndDeletedUsersCannotReuseTokens() throws Exception {
        String stale = token("otro"), admin = token("admin");
        call("PATCH","/api/usuarios/"+ids.get("otro")+"/estado",admin,Map.of("estado","inactivo"),200);
        call("GET","/api/tickets",stale,null,401);
        call("POST","/api/auth/login",null,Map.of("email","otro@example.test","password","Prueba123!"),401);
        call("DELETE","/api/usuarios/"+ids.get("otro"),admin,null,204);
        call("GET","/api/tickets",stale,null,401);
        call("GET","/api/tickets","invalid-token",null,401);
        call("GET","/api/tickets",null,null,401);
    }

    @Test void ticketLifecycleAndCommentAuthors() throws Exception {
        long id = ticket(); String path = "/api/tickets/"+id;
        call("PATCH",path+"/asignar",token("jefatura"),Map.of("tecnicoId",ids.get("tecnico")),200);
        call("PATCH",path+"/prioridad",token("admin"),Map.of("prioridadId",3),200);
        call("GET",path,token("tecnico"),null,200);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",4),422);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",2),200);
        for (String role : new String[]{"admin","solicitante","tecnico"}) {
            JsonNode c = call("POST",path+"/comentarios",token(role),Map.of("contenido","Mensaje de "+role,"usuarioId",ids.get("admin")),201);
            assertThat(c.get("autor").asText()).isEqualTo(role+" Prueba");
        }
        assertThat(call("GET",path+"/comentarios",token("solicitante"),null,200).size()).isEqualTo(3);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",4),200);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",5),422);
        call("PATCH",path+"/estado",token("jefatura"),Map.of("estadoId",5),200);
        call("DELETE","/api/usuarios/"+ids.get("solicitante"),token("admin"),null,204);
        assertThat(db.queryForObject("SELECT estado FROM usuarios WHERE id=?",String.class,ids.get("solicitante"))).isEqualTo("inactivo");
        assertThat(db.queryForObject("SELECT count(*) FROM comentarios WHERE ticket_id=?",Integer.class,id)).isEqualTo(3);
    }

    @Test void ticketAccessIsScoped() throws Exception {
        long id=ticket(); String path="/api/tickets/"+id;
        for (String role : new String[]{"otro","tecnico"}) {
            assertThat(call("GET","/api/tickets",token(role),null,200).size()).isZero();
            call("GET",path,token(role),null,403);
            call("GET",path+"/comentarios",token(role),null,403);
            call("POST",path+"/comentarios",token(role),Map.of("contenido","Ajeno"),403);
        }
        for (String role : new String[]{"admin","jefatura","tecnico"})
            call("POST","/api/tickets",token(role),Map.of("asunto","x","descripcion","x","categoriaId",1),403);
        call("PATCH",path+"/asignar",token("solicitante"),Map.of("tecnicoId",ids.get("tecnico")),403);
        call("PATCH",path+"/prioridad",token("tecnico"),Map.of("prioridadId",3),403);
        call("PATCH",path+"/estado",token("solicitante"),Map.of("estadoId",2),403);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",2),403);
        call("PATCH",path+"/asignar",token("admin"),Map.of("tecnicoId",ids.get("otro")),422);
    }

    @Test void historyIsOrderedAndScoped() throws Exception {
        long id = ticket(); String path = "/api/tickets/" + id;
        assertThat(call("GET",path+"/historial",token("solicitante"),null,200).size()).isZero();
        call("GET",path+"/historial",null,null,401);
        call("GET",path+"/historial",token("otro"),null,403);
        call("GET",path+"/historial",token("tecnico"),null,403);
        call("GET","/api/tickets/99999/historial",token("admin"),null,404);
        call("PATCH",path+"/asignar",token("admin"),Map.of("tecnicoId",ids.get("tecnico")),200);
        call("PATCH",path+"/prioridad",token("jefatura"),Map.of("prioridadId",3),200);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",2),200);
        call("PATCH",path+"/estado",token("tecnico"),Map.of("estadoId",2),200);
        db.update("UPDATE historial_cambios SET fecha_cambio='2026-01-01 12:00:00' WHERE ticket_id=?",id);
        for (String role : new String[]{"admin","jefatura","solicitante","tecnico"}) {
            JsonNode history = call("GET",path+"/historial",token(role),null,200);
            assertThat(history.size()).isEqualTo(3);
            assertThat(history.get(0).get("campoModificado").asText()).isEqualTo("estado");
            assertThat(history.get(0).get("usuario").asText()).isEqualTo("tecnico Prueba");
            assertThat(history.get(0).get("valorAnterior").asText()).isEqualTo("abierto");
            assertThat(history.get(0).get("valorNuevo").asText()).isEqualTo("en_progreso");
            assertThat(history.get(0).get("fechaCambio").asText()).startsWith("2026-01-01T12:00:00");
            assertThat(history.get(1).get("campoModificado").asText()).isEqualTo("prioridad");
            assertThat(history.get(2).get("campoModificado").asText()).isEqualTo("asignado_a");
            assertThat(history.get(2).get("valorAnterior").asText()).isEqualTo("sin asignar");
            assertThat(history.get(0).size()).isEqualTo(6);
        }
        long otherId = ticket();
        assertThat(call("GET","/api/tickets/"+otherId+"/historial",token("admin"),null,200).size()).isZero();
        call("GET",path+"/historial",token("otro"),null,403);
    }

    @Test void notificationsArePrivate() throws Exception {
        ticket();
        JsonNode inbox=call("GET","/api/notificaciones",token("solicitante"),null,200);
        long id=inbox.get("items").get(0).get("id").asLong();
        assertThat(inbox.get("noLeidas").asInt()).isPositive();
        call("PATCH","/api/notificaciones/"+id+"/leida",token("otro"),null,404);
        call("POST","/api/notificaciones/leer-todas",token("otro"),null,200);
        assertThat(call("GET","/api/notificaciones",token("solicitante"),null,200).get("noLeidas").asInt()).isPositive();
        call("PATCH","/api/notificaciones/"+id+"/leida",token("solicitante"),null,200);
        assertThat(call("POST","/api/notificaciones/leer-todas",token("solicitante"),null,200).get("noLeidas").asInt()).isZero();
    }

    @Test void catalogsSettingsAndAutomaticAssignment() throws Exception {
        for (String role : new String[]{"admin","jefatura","tecnico","solicitante"})
            for (String catalog : new String[]{"areas","categorias","prioridades","estados"})
                assertThat(call("GET","/api/catalogos/"+catalog,token(role),null,200).size()).isPositive();
        call("PUT","/api/ajustes",token("admin"),Map.of("asignacionAutomatica",true),200);
        assertThat(call("GET","/api/ajustes",token("admin"),null,200).get("asignacionAutomatica").asBoolean()).isTrue();
        long id=ticket();
        assertThat(call("GET","/api/tickets/"+id,token("admin"),null,200).get("asignadoAId").asLong()).isEqualTo(ids.get("tecnico"));
        for (String role : new String[]{"jefatura","tecnico","solicitante"}) {
            call("GET","/api/ajustes",token(role),null,403);
            call("PUT","/api/ajustes",token(role),Map.of("asignacionAutomatica",false),403);
        }
    }

    @Test void validationAndErrorContract() throws Exception {
        String admin=token("admin");
        JsonNode e=call("POST","/api/usuarios",admin,Map.of(),400);
        assertThat(e.has("detalles")).isTrue(); assertThat(e.get("status").asInt()).isEqualTo(400);
        call("POST","/api/usuarios",admin,user("invalid","rol_admin"),400);
        call("POST","/api/usuarios",admin,user("x@example.test","superadmin"),400);
        call("POST","/api/usuarios",admin,"{",400);
        call("GET","/api/usuarios/no-numero",admin,null,400);
        call("PATCH","/api/usuarios/"+ids.get("otro")+"/estado",admin,Map.of("estado","bloqueado"),400);
        call("PUT","/api/ajustes",admin,Map.of(),400);
        long id=ticket();
        call("POST","/api/tickets/"+id+"/comentarios",token("solicitante"),Map.of("contenido"," "),400);
        call("POST","/api/tickets/"+id+"/comentarios",token("solicitante"),Map.of("contenido","x".repeat(2001)),400);
        call("PATCH","/api/tickets/"+id+"/estado",admin,Map.of("estadoId",-1),400);
        call("PATCH","/api/tickets/"+id+"/estado",admin,Map.of("estadoId",99999),404);
        call("GET","/api/tickets/99999",admin,null,404);
    }

    @Test void openApiDocumentsAllModules() throws Exception {
        JsonNode spec=call("GET","/v3/api-docs",null,null,200);
        assertThat(spec.get("paths").get("/api/auth/login").get("post").get("security").isEmpty()).isTrue();
        assertThat(spec.get("paths").get("/api/usuarios").get("post").get("responses").has("201")).isTrue();
        assertThat(spec.get("paths").get("/api/usuarios/{id}").get("delete").get("responses").has("204")).isTrue();
        assertThat(spec.get("components").get("schemas").has("ApiErrorResponse")).isTrue();
        assertThat(spec.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        for(String path:new String[]{"/api/auth/login","/api/usuarios/{id}","/api/tickets/{id}/comentarios","/api/notificaciones","/api/ajustes"})
            assertThat(spec.get("paths").has(path)).isTrue();
        assertThat(spec.get("paths").get("/api/auth/registro").get("post").get("deprecated").asBoolean()).isTrue();
    }
}
