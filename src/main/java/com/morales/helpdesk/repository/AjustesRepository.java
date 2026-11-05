package com.morales.helpdesk.repository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository @RequiredArgsConstructor
public class AjustesRepository {
    private final JdbcTemplate jdbc;
    public boolean asignacionAutomatica() {
        return Boolean.parseBoolean(jdbc.queryForObject("SELECT valor FROM ajustes WHERE clave='asignacion_automatica'", String.class));
    }
    public void guardar(boolean habilitada) {
        jdbc.update("UPDATE ajustes SET valor=? WHERE clave='asignacion_automatica'", Boolean.toString(habilitada));
    }
    // One DB transaction lock shared by every operation changing the allocation/load.
    public void bloquearReparto() { jdbc.execute("SELECT pg_advisory_xact_lock(2026092801)"); }
}
