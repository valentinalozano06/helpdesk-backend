package com.morales.helpdesk.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registro de eventos transversal (CE0231) implementado con AOP
 * (patron Proxy): cada metodo publico de la capa de servicio queda
 * envuelto por este aspecto, que registra entrada, salida, duracion
 * y excepciones sin ensuciar la logica de negocio con logs repetidos.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("com.morales.helpdesk.EVENTOS");

    @Before("execution(* com.morales.helpdesk.service..*ServiceImpl.*(..))")
    public void antesDeEjecutar(JoinPoint jp) {
        log.debug(">> {} args={}", firma(jp), jp.getArgs());
    }

    @Around("execution(* com.morales.helpdesk.controller..*Controller.*(..))")
    public Object registrarLlamadaApi(ProceedingJoinPoint pjp) throws Throwable {
        long inicio = System.currentTimeMillis();
        String firma = pjp.getSignature().toShortString();
        try {
            Object resultado = pjp.proceed();
            long duracion = System.currentTimeMillis() - inicio;
            log.info("API {} ejecutado en {} ms", firma, duracion);
            return resultado;
        } catch (Exception ex) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("API {} fallo tras {} ms: {}", firma, duracion, ex.getMessage());
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "execution(* com.morales.helpdesk.service..*ServiceImpl.*(..))", throwing = "ex")
    public void alFallar(JoinPoint jp, Throwable ex) {
        log.error("Excepcion en {}: {}", firma(jp), ex.getMessage());
    }

    private String firma(JoinPoint jp) {
        return jp.getSignature().toShortString();
    }
}
