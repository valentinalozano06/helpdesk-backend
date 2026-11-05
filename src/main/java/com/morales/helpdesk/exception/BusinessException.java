package com.morales.helpdesk.exception;

/** Error de regla de negocio (transicion de estado invalida, datos inconsistentes, etc). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
