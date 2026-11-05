package com.morales.helpdesk.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Patron Builder aplicado a la construccion del codigo unico del ticket
 * (formato TK-YYYYMMDD-NNN, definido en el diseno de base de datos del
 * Entregable 2). Encapsula el formateo y el correlativo en un unico
 * punto, evitando construir el String de forma dispersa por el codigo.
 */
public class TicketCodeBuilder {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LocalDate fecha = LocalDate.now();
    private long correlativoDelDia = 1;

    public TicketCodeBuilder conFecha(LocalDate fecha) {
        this.fecha = fecha;
        return this;
    }

    public TicketCodeBuilder conCorrelativo(long correlativoDelDia) {
        this.correlativoDelDia = correlativoDelDia;
        return this;
    }

    public String prefijoDelDia() {
        return "TK-" + fecha.format(FORMATO_FECHA) + "-";
    }

    public String build() {
        return prefijoDelDia() + String.format("%03d", correlativoDelDia);
    }

    public static TicketCodeBuilder nuevo() {
        return new TicketCodeBuilder();
    }
}
